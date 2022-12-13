package fr.ikisource.restui.service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.util.Optional;

import fr.ikisource.restui.exception.NotFoundException;
import fr.ikisource.restui.exception.TechnicalException;
import fr.ikisource.restui.model.Endpoint;
import fr.ikisource.restui.model.Exchange;
import fr.ikisource.restui.model.Project;
import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import fr.ikisource.restui.model.Parameter;
import fr.ikisource.restui.model.Parameter.Direction;
import fr.ikisource.restui.model.Parameter.Location;
import fr.ikisource.restui.model.Parameter.Type;

public class ExchangesService {

	/**
	 * Loads exchanges from the exchanges file
	 * @param projectUri - The project URI
	 * @param project - The project to populate
	 * @throws NotFoundException
	 * @throws TechnicalException
	 */
	public static void loadExchanges(final URI projectUri, final Project project) throws NotFoundException, TechnicalException {

		URI exchangeUri = buildExchangesUri(projectUri);

		final File file = new File(exchangeUri);
		if (!file.exists()) {
			throw new NotFoundException("The exchange file", file.getAbsolutePath());
		}

		final SAXBuilder sxb = new SAXBuilder();
		try {

			final Document document = sxb.build(exchangeUri.toString());

			// exchanges
			final Element exchangesElement = document.getRootElement();
			if (exchangesElement != null) {
				for (final Element endpointElement : exchangesElement.getChildren()) {
					// endpoints
					String endpointName = endpointElement.getAttributeValue("name");

					// searching the endpoint in the project
					Optional<Endpoint> optionalEndpoint = project.getAllChildren()
							.filter(item -> item instanceof Endpoint && item.getName().equalsIgnoreCase(endpointName))
							.map(item -> (Endpoint) item)
							.findFirst();

					if (optionalEndpoint.isPresent()) {
						Endpoint endpoint = optionalEndpoint.get();

						// exchanges of the endpoint
						for (final Element exchangeElement : endpointElement.getChildren()) {
							String name = exchangeElement.getAttributeValue("name");
							String date = exchangeElement.getAttributeValue("date");
							String requestBodyType = exchangeElement.getAttributeValue("requestBodyType");
							String status = exchangeElement.getAttributeValue("status");
							String duration = exchangeElement.getAttributeValue("duration");
							String uri = exchangeElement.getAttributeValue("uri");
							Exchange exchange = new Exchange(endpointName, name, Long.valueOf(date), Integer.valueOf(duration), Integer.valueOf(status), Exchange.BodyType.valueOf(requestBodyType));
							exchange.setUri(uri);

							for (final Element parameterElement : exchangeElement.getChildren()) {
								String enabled = parameterElement.getAttributeValue("enabled");
								String direction = parameterElement.getAttributeValue("direction");
								String location = parameterElement.getAttributeValue("location");
								String type = parameterElement.getAttributeValue("type");
								String parameterName = parameterElement.getAttributeValue("name");
								String value = parameterElement.getAttributeValue("value");
								Parameter parameter = new Parameter(Boolean.valueOf(enabled), Direction.valueOf(direction), Location.valueOf(location), Type.valueOf(type), parameterName, value);

								// add parameter to the exchange
								exchange.addParameter(parameter);
							}
							// retrieve the endpoint parameters that are not in the exchange (only for working exchange)
							if (exchange.isWorking()) {
								endpoint.getParameters().stream()
								.filter(p -> !exchange.containsParameter(p))
								.forEach(p -> exchange.addParameter(p.duplicate()));
							}

							if (!exchange.isEmpty()) {
								endpoint.addExchange(exchange);
							}
						}
					}
				}
			}
		} catch (final Exception e) {
			Logger.error(e);
			Notifier.notifyError(e.getMessage());
			throw new TechnicalException(e.getMessage());
		}
	}

	/**
	 * Saves the exchanges to an XML file
	 * @param project - The project
	 * @param projectUri - The project URI
	 * @throws TechnicalException
	 */
	public static void saveExchanges(final Project project, final URI projectUri) throws TechnicalException {

		URI exchangeUri = buildExchangesUri(projectUri);

		if (project != null) {
			Element root = new Element("exchanges");
			project.getAllChildren().filter(item -> item instanceof Endpoint)
					.map(item -> (Endpoint) item)
					.forEach(endpoint -> {

						// endpoint
						Element elementEndpoint = new Element("endpoint");
						elementEndpoint.setAttribute(new Attribute("name", endpoint.getName()));

						root.addContent(elementEndpoint);

						// exchanges
						endpoint.getExchanges().stream().forEach(exchange -> {
							Element elementExchange = new Element("exchange");
							elementExchange.setAttribute(new Attribute("name", exchange.getName()));
							elementExchange.setAttribute(new Attribute("date", exchange.getDate().toString()));
							elementExchange.setAttribute(new Attribute("requestBodyType", exchange.getRequestBodyType().name()));
							elementExchange.setAttribute(new Attribute("status", exchange.getStatus() == null ? "" : exchange.getStatus().toString()));
							elementExchange.setAttribute(new Attribute("duration", exchange.getDuration() == null ? "" : exchange.getDuration().toString()));
							elementExchange.setAttribute(new Attribute("uri", exchange.getUri() == null ? "" : exchange.getUri().toString()));

							elementEndpoint.addContent(elementExchange);

							// parameters
							exchange.getParameters().forEach(parameter -> {
								Element elementParameter = new Element("parameter");
								elementParameter.setAttribute(new Attribute("enabled", parameter.getEnabled().toString()));
								elementParameter.setAttribute(new Attribute("direction", parameter.getDirection()));
								elementParameter.setAttribute(new Attribute("location", parameter.getLocation()));
								elementParameter.setAttribute(new Attribute("type", parameter.getType()));
								if (parameter.getName() != null) {
									elementParameter.setAttribute(new Attribute("name", parameter.getName()));
								}
								if (parameter.getValue() != null) {
									elementParameter.setAttribute(new Attribute("value", parameter.getValue()));
								}

								elementExchange.addContent(elementParameter);
							});

						});
					});
			final XMLOutputter xmlOutputter = new XMLOutputter(Format.getPrettyFormat());
			final Document document = new Document(root);

			try(FileOutputStream fileOutputStream =  new FileOutputStream(new File(exchangeUri))) {
				xmlOutputter.output(document, fileOutputStream);
			} catch (final IOException e) {
				Logger.error(e);
				Notifier.notifyError(e.getMessage());
				throw new TechnicalException(e.getMessage());
			}
		}
	}

	private static URI buildExchangesUri(final URI projectUri) {
		String exchangesUri = null;

		String uri = projectUri.toString();
		int index = uri.lastIndexOf("/");
		if (index != -1) {
			String path = uri.substring(0, index);
			String fileName = uri.substring(index + 1, uri.length());
			if (!fileName.isEmpty()) {
				String name = null;
				String extension = null;
				String[] split = fileName.split("\\.");
				if (split.length == 1) {
					name = split[0];
					extension = "";

				} else {
					name = split[0];
					extension = split[1];
				}
				String exchangeFileName = name + "-exchanges" + (extension.isEmpty() ? "" : "." + extension);
				exchangesUri = path + "/" + exchangeFileName;
			}
		}
		return URI.create(exchangesUri);
	}

}
