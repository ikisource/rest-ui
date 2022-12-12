package fr.omathe.restui.service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.ws.rs.core.MultivaluedMap;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.client.urlconnection.URLConnectionClientHandler;
import com.sun.jersey.core.util.MultivaluedMapImpl;

import fr.omathe.restui.conf.App;
import fr.omathe.restui.exception.ClientException;
import fr.omathe.restui.model.Exchange;
import fr.omathe.restui.model.Exchange.BodyType;
import fr.omathe.restui.model.Parameter;
import fr.omathe.restui.model.Parameter.Direction;
import fr.omathe.restui.model.Parameter.Location;
import fr.omathe.restui.model.Parameter.Type;

public class RestClient {

	private static final String LINE_FEED = "\r\n";
	private static final String BOUNDARY = "oma";
	private static final String END_BOUNDARY = "--" + BOUNDARY;
	private static final String CLOSE_BOUNDARY = "--" + BOUNDARY + "--";

	private static final Client client = Client.create();

	public static ClientResponse execute(final String method, final Exchange exchange) throws ClientException {
		ClientResponse response = null;

		switch (method) {
		case "POST":
			response = post(exchange);
			break;
		case "GET":
			response = get(exchange);
			break;
		case "PUT":
			response = put(exchange);
			break;
		case "PATCH":
			response = patch(exchange);
			break;
		case "DELETE":
			response = delete(exchange);
			break;

		default:
			break;
		}
		return response;
	}

	public static void setConnectionTimeout(final Integer duration) {
		client.setConnectTimeout(duration == null ? App.DEFAULT_CONNECTION_TIMEOUT : duration);
	}

	public static void setReadTimeout(final Integer duration) {
		client.setReadTimeout(duration == null ? App.DEFAULT_READ_TIMEOUT : duration);
	}

	private static ClientResponse get(final Exchange exchange) throws ClientException {

		ClientResponse response = null;

		try {
			List<Parameter> parameters = exchange.getParameters().stream().filter(p -> p.getEnabled()).collect(Collectors.toList());
			String uri = exchange.getUri();

			final WebResource webResource = client.resource(uriWithoutQueryParams(uri)).queryParams(buildParams(parameters));
			final WebResource.Builder builder = webResource.getRequestBuilder();

			addHeaders(builder, parameters);

			response = builder.get(ClientResponse.class);
		} catch (final ClientHandlerException e) {
			Logger.error(e);
			Notifier.notifyError(e.getMessage());
			throw new ClientException(e.getMessage());
		} finally {
			client.destroy();
		}
		return response;
	}

	private static ClientResponse post(final Exchange exchange) throws ClientException {

		ClientResponse response = null;

		String body = null;
		ByteArrayOutputStream bos = new ByteArrayOutputStream();

		try {
			String uri = exchange.getUri();
			List<Parameter> parameters = exchange.getParameters().stream().filter(p -> p.getEnabled()).collect(Collectors.toList());

			final WebResource webResource = client.resource(uriWithoutQueryParams(uri)).queryParams(buildParams(parameters));
			final WebResource.Builder builder = webResource.getRequestBuilder();

			if (exchange.getRequestBodyType().equals(BodyType.X_WWW_FORM_URL_ENCODED)) {
				body = parameters.stream().filter(p -> p.getEnabled() && p.isBodyParameter() && p.isTypeText()).map(p -> encode(p.getName()) + "=" + encode(p.getValue())).collect(Collectors.joining("&"));

				Optional<Parameter> optional = exchange.findParameter(Direction.REQUEST, Location.HEADER, "Content-Type");
				if (optional.isPresent()) {
					parameters.remove(optional.get());
				}
				parameters.add(new Parameter(Boolean.TRUE, Direction.REQUEST, Location.HEADER, Type.TEXT, "Content-Type", "application/x-www-form-urlencoded"));

				bos.write(new String(body).getBytes());

			} else if (exchange.getRequestBodyType().equals(BodyType.RAW)) {
				body = exchange.getRequestRawBody();
				if (body != null) {
					bos.write(new String(body).getBytes());
				}

			} else if (exchange.getRequestBodyType().equals(BodyType.FORM_DATA)) {
				Optional<Parameter> optional = exchange.findParameter(Direction.REQUEST, Location.HEADER, "Content-Type");
				if (optional.isPresent()) {
					parameters.remove(optional.get());
				}
				parameters.add(new Parameter(Boolean.TRUE, Direction.REQUEST, Location.HEADER, Type.TEXT, "Content-Type", "multipart/form-data; boundary=" + BOUNDARY));

				for (Parameter parameter : parameters) {
					if (parameter.getEnabled() && parameter.isBodyParameter() && parameter.getType().equals(Type.TEXT.name())) {
						addMultiparTextParameter(bos, parameter);
					} else if (parameter.getEnabled() && parameter.isBodyParameter() && parameter.getType().equals(Type.FILE.name())) {
						addMultiparFileParameter(bos, parameter);
					}
				}
				bos.write(new String(CLOSE_BOUNDARY + LINE_FEED).getBytes());
			}
			addHeaders(builder, parameters);
			bos.flush();
			response = builder.post(ClientResponse.class, bos.toByteArray());
		} catch (final Exception e) {
			Logger.error(e);
			Notifier.notifyError(e.getMessage());
			throw new ClientException(e.getMessage());
		} finally {
			client.destroy();
			if (bos != null) {
				try {
					bos.close();
				} catch (IOException e) {
					Logger.error(e);
					Notifier.notifyError(e.getMessage());
				}
			}
		}
		return response;
	}

	private static ClientResponse put(final Exchange exchange) throws ClientException {

		ClientResponse response = null;

		String body = null;
		ByteArrayOutputStream bos = new ByteArrayOutputStream();

		try {
			String uri = exchange.getUri();
			List<Parameter> parameters = exchange.getParameters().stream().filter(p -> p.getEnabled()).collect(Collectors.toList());

			final WebResource webResource = client.resource(uriWithoutQueryParams(uri)).queryParams(buildParams(parameters));
			final WebResource.Builder builder = webResource.getRequestBuilder();

			if (exchange.getRequestBodyType().equals(BodyType.X_WWW_FORM_URL_ENCODED)) {
				body = parameters.stream().filter(p -> p.getEnabled() && p.isBodyParameter() && p.isTypeText()).map(p -> encode(p.getName()) + "=" + encode(p.getValue())).collect(Collectors.joining("&"));

				Optional<Parameter> optional = exchange.findParameter(Direction.REQUEST, Location.HEADER, "Content-Type");
				if (optional.isPresent()) {
					parameters.remove(optional.get());
				}
				parameters.add(new Parameter(Boolean.TRUE, Direction.REQUEST, Location.HEADER, Type.TEXT, "Content-Type", "application/x-www-form-urlencoded"));

				if (body != null) {
					bos.write(new String(body).getBytes());
				}

			} else if (exchange.getRequestBodyType().equals(BodyType.RAW)) {
				body = exchange.getRequestRawBody();
				if (body != null) {
					bos.write(new String(body).getBytes());
				}

			} else if (exchange.getRequestBodyType().equals(BodyType.FORM_DATA)) {
				Optional<Parameter> optional = exchange.findParameter(Direction.REQUEST, Location.HEADER, "Content-Type");
				if (optional.isPresent()) {
					parameters.remove(optional.get());
				}
				parameters.add(new Parameter(Boolean.TRUE, Direction.REQUEST, Location.HEADER, Type.TEXT, "Content-Type", "multipart/form-data; boundary=" + BOUNDARY));

				for (Parameter parameter : parameters) {
					if (parameter.getEnabled() && parameter.isBodyParameter() && parameter.getType().equals(Type.TEXT.name())) {
						addMultiparTextParameter(bos, parameter);
					} else if (parameter.getEnabled() && parameter.isBodyParameter() && parameter.getType().equals(Type.FILE.name())) {
						addMultiparFileParameter(bos, parameter);
					}
				}
				bos.write(new String(CLOSE_BOUNDARY + LINE_FEED).getBytes());
			}
			addHeaders(builder, parameters);
			bos.flush();
			response = builder.put(ClientResponse.class, bos.toByteArray());
		} catch (final Exception e) {
			Logger.error(e);
			Notifier.notifyError(e.getMessage());
			throw new ClientException(e.getMessage());
		} finally {
			client.destroy();
			if (bos != null) {
				try {
					bos.close();
				} catch (IOException e) {
					Logger.error(e);
					Notifier.notifyError(e.getMessage());
				}
			}
		}
		return response;
	}

	private static ClientResponse patch(final Exchange exchange) throws ClientException {

		ClientResponse response = null;
		final DefaultClientConfig config = new DefaultClientConfig();
		config.getProperties().put(URLConnectionClientHandler.PROPERTY_HTTP_URL_CONNECTION_SET_METHOD_WORKAROUND, true);

		final Client client = Client.create(config);
		String body = null;
		ByteArrayOutputStream bos = new ByteArrayOutputStream();

		try {
			String uri = exchange.getUri();
			List<Parameter> parameters = exchange.getParameters().stream().filter(p -> p.getEnabled()).collect(Collectors.toList());

			final WebResource webResource = client.resource(uriWithoutQueryParams(uri)).queryParams(buildParams(parameters));
			final WebResource.Builder builder = webResource.getRequestBuilder();

			if (exchange.getRequestBodyType().equals(BodyType.X_WWW_FORM_URL_ENCODED)) {
				body = parameters.stream().filter(p -> p.getEnabled() && p.isBodyParameter() && p.isTypeText()).map(p -> encode(p.getName()) + "=" + encode(p.getValue())).collect(Collectors.joining("&"));

				Optional<Parameter> optional = exchange.findParameter(Direction.REQUEST, Location.HEADER, "Content-Type");
				if (optional.isPresent()) {
					parameters.remove(optional.get());
				}
				parameters.add(new Parameter(Boolean.TRUE, Direction.REQUEST, Location.HEADER, Type.TEXT, "Content-Type", "application/x-www-form-urlencoded"));

				if (body != null) {
					bos.write(new String(body).getBytes());
				}

			} else if (exchange.getRequestBodyType().equals(BodyType.RAW)) {
				body = exchange.getRequestRawBody();
				if (body != null) {
					bos.write(new String(body).getBytes());
				}

			} else if (exchange.getRequestBodyType().equals(BodyType.FORM_DATA)) {
				Optional<Parameter> optional = exchange.findParameter(Direction.REQUEST, Location.HEADER, "Content-Type");
				if (optional.isPresent()) {
					parameters.remove(optional.get());
				}
				parameters.add(new Parameter(Boolean.TRUE, Direction.REQUEST, Location.HEADER, Type.TEXT, "Content-Type", "multipart/form-data; boundary=" + BOUNDARY));

				for (Parameter parameter : parameters) {
					if (parameter.getEnabled() && parameter.isBodyParameter() && parameter.getType().equals(Type.TEXT.name())) {
						addMultiparTextParameter(bos, parameter);
					} else if (parameter.getEnabled() && parameter.isBodyParameter() && parameter.getType().equals(Type.FILE.name())) {
						addMultiparFileParameter(bos, parameter);
					}
				}
				bos.write(new String(CLOSE_BOUNDARY + LINE_FEED).getBytes());
			}
			addHeaders(builder, parameters);
			bos.flush();
			response = builder.method("PATCH", ClientResponse.class, bos.toByteArray());
		} catch (final Exception e) {
			Logger.error(e);
			Notifier.notifyError(e.getMessage());
			throw new ClientException(e.getMessage());
		} finally {
			client.destroy();
			if (bos != null) {
				try {
					bos.close();
				} catch (IOException e) {
					Logger.error(e);
					Notifier.notifyError(e.getMessage());
				}
			}
		}
		return response;
	}

	private static ClientResponse delete(final Exchange exchange) throws ClientException {

		ClientResponse response = null;
		final Client client = Client.create();

		try {
			List<Parameter> parameters = exchange.getParameters().stream().filter(p -> p.getEnabled()).collect(Collectors.toList());
			String uri = exchange.getUri();

			final WebResource webResource = client.resource(uriWithoutQueryParams(uri)).queryParams(buildParams(parameters));
			final WebResource.Builder builder = webResource.getRequestBuilder();

			addHeaders(builder, parameters);

			response = builder.delete(ClientResponse.class);
		} catch (final Exception e) {
			Logger.error(e);
			Notifier.notifyError(e.getMessage());
			throw new ClientException(e.getMessage());
		} finally {
			client.destroy();
		}
		return response;
	}

	private static WebResource.Builder addHeaders(final WebResource.Builder builder, final List<Parameter> parameters) {

		// add query parameters
		parameters.stream()
				.filter(p -> p.getEnabled() && p.isHeaderParameter()).forEach(p -> builder.header(p.getName(), p.getValue()));

		return builder;
	}

	private static MultivaluedMap<String, String> buildParams(final List<Parameter> parameters) {

		final MultivaluedMap<String, String> params = new MultivaluedMapImpl();
		for (final Parameter parameter : parameters) {
			if (parameter.getEnabled() && (parameter.isQueryParameter() /* || parameter.isBodyParameter() */)) {
				try {
					final String encodedName = URLEncoder.encode(parameter.getName(), "UTF-8");
					final String encodedValue = URLEncoder.encode(parameter.getValue(), "UTF-8").replaceAll("[+]", "%20");
					params.add(encodedName, encodedValue);
				} catch (final UnsupportedEncodingException e) {
					Logger.error(e);
					Notifier.notifyError(e.getMessage());
				}
			}
		}
		return params;
	}

	private static String uriWithoutQueryParams(final String uri) {

		return uri.split("[?]")[0];
	}

	private static String encode(final String value) {

		String encodedValue = null;
		try {
			encodedValue = URLEncoder.encode(value, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			Logger.error(e);
			Notifier.notifyError(e.getMessage());
		}
		return encodedValue;
	}

	private static byte[] getFileContentBytes(final String uri) {
		byte[] bytes = null;

		try {
			final Path path = Paths.get(URI.create(uri));
			bytes = Files.readAllBytes(path);
		} catch (IOException e) {
			Logger.error(e);
			Notifier.notifyError(e.getMessage());
		}
		return bytes;
	}

	private static void addMultiparTextParameter(final ByteArrayOutputStream bos, final Parameter parameter) {

		try {
			bos.write(new String(END_BOUNDARY + LINE_FEED).getBytes());
			bos.write(new String("Content-Disposition: form-data; name=\"" + parameter.getName() + "\"" + LINE_FEED).getBytes());
			bos.write(new String(LINE_FEED).getBytes());
			bos.write(new String(parameter.getValue() + LINE_FEED).getBytes());
		} catch (IOException e) {
			Logger.error(e);
			Notifier.notifyError(e.getMessage());
		}
	}

	private static void addMultiparFileParameter(final ByteArrayOutputStream bos, final Parameter parameter) {

		try {
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
				System.err.println("Error : the file '" + parameter.getValue() + "' does not exist.");
			}
		} catch (IOException e) {
			Logger.error(e);
			Notifier.notifyError(e.getMessage());
		}
	}
}
