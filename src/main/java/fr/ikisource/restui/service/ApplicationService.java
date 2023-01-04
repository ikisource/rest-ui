package fr.ikisource.restui.service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import fr.ikisource.restui.conf.App;
import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import fr.ikisource.restui.model.Application;
import fr.ikisource.restui.model.BaseUrl;

public class ApplicationService {

	public static Application openApplication() {

		Application application = new Application();

		SAXBuilder saxBuilder = new SAXBuilder();

		try {
			Document document = saxBuilder.build(App.APPLICATION_FILE);
			Element applicationElement = document.getRootElement();
			Element lastProjectUriElement = applicationElement.getChild("lastProjectUri");
			application.setLastProjectUri(lastProjectUriElement.getValue());
			Element styleElement = applicationElement.getChild("style");
			if (styleElement == null || styleElement.getValue().isEmpty()) {
				App.getStyleUri(App.DEFAULT_STYLE).ifPresent(style -> application.setStyle(style));
			} else {
				application.setStyle(styleElement.getValue());
			}
			// baseUrls
			final Element elementBaseUrls = applicationElement.getChild("baseUrls");
			if (elementBaseUrls != null) {
				for (final Element elementBaseUrl : elementBaseUrls.getChildren()) {
					// BaseUrl
					final BaseUrl baseUrl = new BaseUrl(elementBaseUrl.getAttributeValue("name"), elementBaseUrl.getAttributeValue("url"), Boolean.valueOf(elementBaseUrl.getAttributeValue("enabled")));
					application.addBaseUrl(baseUrl);
				}
			}
			// connectionTimeout
			Element connectionTimeout = applicationElement.getChild("connectionTimeout");
			if (connectionTimeout == null || connectionTimeout.getValue().isEmpty()) {
				application.setConnectionTimeout(App.DEFAULT_CONNECTION_TIMEOUT);
			} else {
				application.setConnectionTimeout(Integer.valueOf(connectionTimeout.getValue()));
			}
			// readTimeout
			Element readTimeoutElement = applicationElement.getChild("readTimeout");
			if (readTimeoutElement == null || readTimeoutElement.getValue().isEmpty()) {
				application.setReadTimeout(App.DEFAULT_READ_TIMEOUT);
			} else {
				application.setReadTimeout(Integer.valueOf(readTimeoutElement.getValue()));
			}
		} catch (final Exception e) {
			Logger.error(e);
			Notifier.notifyError(e.getMessage());
		}
		return application;
	}

	public static void saveApplication(final Application application) {

		if (application != null) {
			Element rootElement = new Element("application");

			Element lastProjectUriElement = new Element("lastProjectUri");
			lastProjectUriElement.addContent(application.getLastProjectUri());
			rootElement.addContent(lastProjectUriElement);

			Element styleElement = new Element("style");
			styleElement.addContent(application.getStyle());
			rootElement.addContent(styleElement);

			// base URLs
			if (!application.getBaseUrls().isEmpty()) {
				final Element elementBaseUrls = new Element("baseUrls");
				for (final BaseUrl baseUrl : application.getBaseUrls()) {
					final Element elementBaseUrl = new Element("baseUrl");
					elementBaseUrl.setAttribute(new Attribute("name", baseUrl.getName()));
					elementBaseUrl.setAttribute(new Attribute("url", baseUrl.getUrl()));
					elementBaseUrl.setAttribute(new Attribute("enabled", baseUrl.getEnabled().toString()));
					elementBaseUrls.addContent(elementBaseUrl);
				}
				rootElement.addContent(elementBaseUrls);
			}

			// connectionTimeout
			Element connectionTimeout = new Element("connectionTimeout");
			connectionTimeout.addContent(String.valueOf(application.getConnectionTimeout()));
			rootElement.addContent(connectionTimeout);
			
			// readTimeout
			Element readTimeoutElement = new Element("readTimeout");
			readTimeoutElement.addContent(String.valueOf(application.getReadTimeout()));
			rootElement.addContent(readTimeoutElement);
			
			XMLOutputter xmlOutputter = new XMLOutputter(Format.getPrettyFormat());
			Document document = new Document(rootElement);
			File applicationFile = new File(App.APPLICATION_FILE);
			try (FileOutputStream fileOutputStream = new FileOutputStream(applicationFile)) {
				xmlOutputter.output(document, fileOutputStream);
			} catch (final IOException e) {
				Logger.error(e);
				Notifier.notifyError(e.getMessage());
			}
		}
	}

	public static void createDefaultApplicationFile() {

		Element rootElement = new Element("application");
		Element lastProjectUriElement = new Element("lastProjectUri");
		rootElement.addContent(lastProjectUriElement);
		Element styleElement = new Element("style");
		App.getStyleUri(App.DEFAULT_STYLE).ifPresent(styleUri -> styleElement.addContent(App.DEFAULT_STYLE));
		rootElement.addContent(styleElement);
		// connectionTimeout
		Element connectionTimeout = new Element("connectionTimeout");
		connectionTimeout.addContent(App.DEFAULT_CONNECTION_TIMEOUT.toString());
		rootElement.addContent(connectionTimeout);
		// readTimeout
		Element readTimeoutElement = new Element("readTimeout");
		readTimeoutElement.addContent(App.DEFAULT_READ_TIMEOUT.toString());
		rootElement.addContent(readTimeoutElement);

		XMLOutputter xmlOutputter = new XMLOutputter(Format.getPrettyFormat());
		Document document = new Document(rootElement);
		File applicationFile = new File(App.APPLICATION_FILE);
		
		if (!applicationFile.getParentFile().exists()) {
			applicationFile.getParentFile().mkdir();
		}
		
		try(FileOutputStream fos = new FileOutputStream(applicationFile)) {
			xmlOutputter.output(document, fos);
		} catch (final IOException e) {
			Logger.error(e);
			Notifier.notifyError(e.getMessage());
		}
	}

}