package fr.ikisource.restui.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import fr.ikisource.restui.exception.NotFoundException;
import fr.ikisource.restui.exception.TechnicalException;
import fr.ikisource.restui.model.Endpoint;
import fr.ikisource.restui.model.Item;
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
import fr.ikisource.restui.model.Path;

public interface ProjectService {

	/**
	 * Opens a project from an URI
	 */
	static Project openProject(final URI uri) throws NotFoundException, TechnicalException {

		final File file = new File(uri);
		if (!file.exists()) {
			throw new NotFoundException("The project file", file.getAbsolutePath());
		}

		Project project = new Project("");
		try (FileInputStream inputStream = new FileInputStream(file)) {
			project = parseXml(inputStream);
		} catch (IOException e) {
			Logger.error(e);
			Notifier.notifyError(e.getMessage());
			throw new TechnicalException(e.getMessage());
		}

		// build the hierarchy
		buildHierarchy(project);

		return project;
	}

	/**
	 * Opens a project from an imputStream
	 */
	static Project openProject(final InputStream inputStream) throws TechnicalException {

		Project project = new Project("");
		project = parseXml(inputStream);

		// build the hierarchy
		buildHierarchy(project);

		return project;
	}

	static Project parseXml(final InputStream inputStream) throws TechnicalException {

		Project project = new Project("");

		if (inputStream != null) {
			try {
				final SAXBuilder sxb = new SAXBuilder();
				final Document document = sxb.build(inputStream);

				// project
				final Element elementProject = document.getRootElement();
				project.setName(elementProject.getAttributeValue("name"));

				// endpoints
				final Element elementEndpoints = elementProject.getChild("endpoints");
				if (elementEndpoints != null) {
					for (final Element elementEndpoint : elementEndpoints.getChildren()) {
						final Endpoint endpoint = new Endpoint(elementEndpoint.getAttributeValue("name"), elementEndpoint.getAttributeValue("path"), elementEndpoint.getAttributeValue("method"), elementEndpoint.getAttributeValue("description"));

						// Parameters
						final Element elementEndpointParameters = elementEndpoint.getChild("parameters");
						if (elementEndpointParameters != null) {

							for (final Element elementParameter : elementEndpointParameters.getChildren()) {
								if (elementParameter != null) {
									final Boolean enabled = Boolean.valueOf(elementParameter.getAttributeValue("enabled"));
									final Direction direction = Direction.valueOf(elementParameter.getAttributeValue("direction"));
									final Location location = Location.valueOf(elementParameter.getAttributeValue("location"));
									final Type type = Type.valueOf(elementParameter.getAttributeValue("type"));
									String attributeName = elementParameter.getAttributeValue("name");
									final String name = attributeName == null ? null : attributeName;
									final String value = elementParameter.getAttributeValue("value");
									final Parameter parameter = new Parameter(enabled, direction, location, type, name, value);
									endpoint.addParameter(parameter);
								}
							}
						}
						project.addEnpoint(endpoint);
					}
				}
			} catch (final Exception e) {
				Logger.error(e);
				Notifier.notifyError(e.getMessage());
				throw new TechnicalException(e.getMessage());
			} finally {
				try {
					inputStream.close();
				} catch (IOException e) {
					Logger.error(e);
					Notifier.notifyError(e.getMessage());
					throw new TechnicalException(e.getMessage());
				}
			}
		}
		return project;
	}

	/**
	 * Saves the project to an XML file
	 */
	static void saveProject(final Project project, final URI uri) throws TechnicalException {

		if (project != null) {

			// project
			final Element elementProject = new Element("project");
			final Attribute attributeProjectName = new Attribute("name", project.getName());
			elementProject.setAttribute(attributeProjectName);

			// endpoints
			final Element elementEndpoints = new Element("endpoints");
			elementProject.addContent(elementEndpoints);

			project.getAllChildren().filter(item -> item instanceof Endpoint).forEach(item -> {
				Endpoint endpoint = (Endpoint) item;
				final Element elementEndpoint = new Element("endpoint");
				final Attribute attributeEndpointName = new Attribute("name", endpoint.getName());
				final Attribute attributeEndpointPath = new Attribute("path", endpoint.getPath());
				final Attribute attributeEndpointMethod = new Attribute("method", endpoint.getMethod());
				final Attribute attributeEndpointDescription = new Attribute("description", endpoint.getDescription());
				elementEndpoint.setAttribute(attributeEndpointName);
				elementEndpoint.setAttribute(attributeEndpointPath);
				elementEndpoint.setAttribute(attributeEndpointMethod);
				elementEndpoint.setAttribute(attributeEndpointDescription);
				elementEndpoints.addContent(elementEndpoint);

				// parameters
				if (endpoint.hasParameters()) {
					final Element elementEndpointParameters = new Element("parameters");
					elementEndpoint.addContent(elementEndpointParameters);
					List<Parameter> parameters = endpoint.getParameters().stream()
							.filter(p -> p.isRequestParameter())
							.collect(Collectors.toList());
					for (final Parameter parameter : parameters) {
						final Element elementParameter = new Element("parameter");
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
						elementEndpointParameters.addContent(elementParameter);
					}
				}
			});

			final XMLOutputter xmlOutputter = new XMLOutputter(Format.getPrettyFormat());
			final Document document = new Document(elementProject);

			try (FileOutputStream fileOutputStream = new FileOutputStream(new File(uri))) {
				xmlOutputter.output(document, fileOutputStream);
			} catch (final IOException e) {
				Logger.error(e);
				Notifier.notifyError(e.getMessage());
				throw new TechnicalException(e.getMessage());
			}
		}
	}

	static void buildHierarchy(final Project project) {

		for (Endpoint endpoint : project.getEndpoints()) {

			List<String> paths = endpoint.getPaths();

			Item parent = project;
			for (String pathString : paths) {
				Optional<Item> optionalItem = parent.findChild(pathString);
				if (optionalItem.isPresent()) {
					parent = optionalItem.get();
				} else {
					// creates path
					Path path = new Path(parent, pathString);
					parent.getChildren().add(path);
					path.setParent(parent);
					parent = path;
				}
			}
			// add endpoint
			parent.getChildren().add(endpoint);
			endpoint.setParent(parent);
		}

		// clear the list of endpoints
		project.getEndpoints().clear();
	}

}
