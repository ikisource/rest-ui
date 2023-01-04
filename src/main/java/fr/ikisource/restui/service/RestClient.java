package fr.ikisource.restui.service;

import java.io.*;
import java.net.ConnectException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpConnectTimeoutException;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;
import java.net.http.HttpClient;

import fr.ikisource.restui.conf.App;
import fr.ikisource.restui.controller.MainController;
import fr.ikisource.restui.exception.ClientException;
import fr.ikisource.restui.model.Application;
import fr.ikisource.restui.model.Exchange;
import fr.ikisource.restui.model.Parameter;
import fr.ikisource.restui.model.Parameter.Direction;
import fr.ikisource.restui.model.Parameter.Location;
import fr.ikisource.restui.model.Parameter.Type;

import static java.nio.charset.StandardCharsets.UTF_8;

public class RestClient {

    private static final String LINE_FEED = "\r\n";
    private static final String BOUNDARY = "oma";
    private static final String END_BOUNDARY = "--" + BOUNDARY;
    private static final String CLOSE_BOUNDARY = "--" + BOUNDARY + "--";

//    private static Application application;
    private static HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofMillis(MainController.application.getConnectionTimeout()))
            .build();

    public static HttpResponse<byte[]> execute(final String method, final Exchange exchange) throws ClientException {

        HttpResponse<byte[]> response = null;
        try {
            switch (method) {
                case "GET" -> response = get(exchange);
                case "POST" -> response = post(exchange);
                case "PUT" -> response = put(exchange);
                case "PATCH" -> response = patch(exchange);
                case "DELETE" -> response = delete(exchange);
            }
        } catch (Exception e) {
            Logger.error(e);
            if (e instanceof ConnectException || e instanceof HttpConnectTimeoutException) {
                Notifier.notifyError("Can't connect to server");
            } else {
                Notifier.notifyError(e.getMessage());
            }
            throw new ClientException(e.getMessage());
        }
        return response;
    }

    public static void setConnectionTimeout(Integer duration) {
        client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(duration))
                .build();
    }

//    public static void setRequestTimeout(final Integer duration) {
//        requestTimeout = duration == null ? App.DEFAULT_READ_TIMEOUT : duration;
//    }

    private static HttpRequest.Builder requestBuilder(Exchange exchange) {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .version(HttpClient.Version.HTTP_2)
// TODO               .uri(exchange.getEncodedUri())
                .uri(URI.create("http://10.255.255.1"))
                .timeout(Duration.ofMillis(MainController.application.getReadTimeout()));
        if (exchange.requestHeaders().length >= 2) {
            builder.headers(exchange.requestHeaders());
        }
        return builder;
    }

    private static HttpResponse<byte[]> get(Exchange exchange) throws IOException, InterruptedException {

        HttpRequest.Builder builder = requestBuilder(exchange)
                .GET();
        HttpRequest request = builder.build();
        System.out.println("timeout = " + client.connectTimeout());
        System.out.println("request timeout = " + request.timeout());
        return client.send(request, HttpResponse.BodyHandlers.ofByteArray());
    }

    private static HttpResponse<byte[]> post(Exchange exchange) throws Exception {

        HttpRequest.BodyPublisher bodyPublisher = body(exchange);
        HttpRequest.Builder builder = requestBuilder(exchange)
                .POST(bodyPublisher);
        HttpRequest request = builder.build();
        return client.send(request, HttpResponse.BodyHandlers.ofByteArray());
    }

    private static HttpResponse put(final Exchange exchange) throws Exception {

        HttpRequest.BodyPublisher bodyPublisher = body(exchange);
        HttpRequest.Builder builder = requestBuilder(exchange)
                .PUT(bodyPublisher);
        HttpRequest request = builder.build();
        return client.send(request, HttpResponse.BodyHandlers.ofByteArray());
    }

    private static HttpResponse patch(Exchange exchange) throws IOException, InterruptedException {

        HttpRequest.BodyPublisher bodyPublisher = body(exchange);
        HttpRequest.Builder builder = requestBuilder(exchange)
                .method("PATCH", bodyPublisher);
        HttpRequest request = builder.build();
        return client.send(request, HttpResponse.BodyHandlers.ofByteArray());
    }

    private static HttpResponse delete(Exchange exchange) throws IOException, InterruptedException {

        HttpRequest.BodyPublisher bodyPublisher = body(exchange);
        HttpRequest.Builder builder = requestBuilder(exchange)
                .DELETE();
        HttpRequest request = builder.build();
        return client.send(request, HttpResponse.BodyHandlers.ofByteArray());
    }

    private static HttpRequest.BodyPublisher body(Exchange exchange) throws IOException {

        List<Parameter> parameters = exchange.getParameters().stream().filter(p -> p.getEnabled()).collect(Collectors.toList());

        HttpRequest.BodyPublisher bodyPublisher = null;
        switch (exchange.getRequestBodyType()) {
            case X_WWW_FORM_URL_ENCODED -> {
                String body = parameters.stream()
                        .filter(p -> p.getEnabled() && p.isBodyParameter() && p.isTypeText())
                        .map(p -> encode(p.getName()) + "=" + encode(p.getValue()))
                        .collect(Collectors.joining("&"));
                Optional<Parameter> optional = exchange.findParameter(Direction.REQUEST, Location.HEADER, "Content-Type");
                if (optional.isPresent()) {
                    parameters.remove(optional.get());
                }
                parameters.add(new Parameter(Boolean.TRUE, Direction.REQUEST, Location.HEADER, Type.TEXT, "Content-Type", "application/x-www-form-urlencoded"));
                bodyPublisher = BodyPublishers.ofInputStream(() -> new ByteArrayInputStream(body.getBytes()));
            }
            case RAW -> {
                bodyPublisher = BodyPublishers.ofInputStream(() -> new ByteArrayInputStream(exchange.getRequestRawBody().getBytes()));
            }
            case FORM_DATA -> {
                Optional<Parameter> optional = exchange.findParameter(Direction.REQUEST, Location.HEADER, "Content-Type");
                if (optional.isPresent()) {
                    parameters.remove(optional.get());
                }
                parameters.add(new Parameter(Boolean.TRUE, Direction.REQUEST, Location.HEADER, Type.TEXT, "Content-Type", "multipart/form-data; boundary=" + BOUNDARY));

                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                bodyPublisher = HttpRequest.BodyPublishers.ofString("");
                for (Parameter parameter : parameters) {
                    if (parameter.getEnabled() && parameter.isBodyParameter() && parameter.getType().equals(Type.TEXT.name())) {
                        addMultipartTextParameter(bos, parameter);
                    } else if (parameter.getEnabled() && parameter.isBodyParameter() && parameter.getType().equals(Type.FILE.name())) {
                        addMultipartFileParameter(bos, parameter);
                    }
                }
                bos.write(new String(CLOSE_BOUNDARY + LINE_FEED).getBytes());
                bos.flush();
                bos.close();
                bodyPublisher = BodyPublishers.ofByteArray(bos.toByteArray());
            }
            default -> HttpRequest.BodyPublishers.noBody();
        }
        return bodyPublisher;
    }

    private static String encode(final String value) {
        return URLEncoder.encode(value, UTF_8);
    }

    private static byte[] getFileContentBytes(final String uri) throws IOException {
        final Path path = Paths.get(URI.create(uri));
        return Files.readAllBytes(path);
    }

    private static void addMultipartTextParameter(ByteArrayOutputStream bos, Parameter parameter) throws IOException {

        bos.write(new String(END_BOUNDARY + LINE_FEED).getBytes());
        bos.write(new String("Content-Disposition: form-data; name=\"" + parameter.getName() + "\"" + LINE_FEED).getBytes());
        bos.write(new String(LINE_FEED).getBytes());
        bos.write(new String(parameter.getValue() + LINE_FEED).getBytes());
    }

    private static void addMultipartFileParameter(ByteArrayOutputStream bos, Parameter parameter) throws IOException {

        Path path = Paths.get(URI.create(parameter.getValue()));
        File file = path.toFile();
        if (file.exists()) {
            String fileName = file.getName();
            bos.write(new String(END_BOUNDARY + LINE_FEED).getBytes());
            bos.write(new String("Content-Disposition: form-data; name=\"" + parameter.getName() + "\"; filename=\"" + fileName + "\"" + LINE_FEED).getBytes());
            bos.write(new String("Content-Type: application/octet-stream" + LINE_FEED).getBytes());
            bos.write(new String(LINE_FEED).getBytes());
            byte[] bytes = getFileContentBytes(parameter.getValue());
            bos.write(bytes);
            bos.write(new String(LINE_FEED).getBytes());
        } else {
            Logger.error("Error : the file '" + parameter.getValue() + "' does not exist.");
            Notifier.notifyError("Error : the file '" + parameter.getValue() + "' does not exist.");
        }
    }

}
