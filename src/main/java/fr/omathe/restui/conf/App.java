package fr.omathe.restui.conf;

import java.io.File;
import java.util.Optional;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public interface App {

	// application
	String TITLE = "RestUI";
	String APLICATION_FILE = getApplicationHome() + "/" + "application.xml";

	// FXML
	String FXML_LOCATION = "/fxml";
	String MAIN_FXML = FXML_LOCATION + "/restui.fxml";
	String PROJECT_FXML = FXML_LOCATION + "/project.fxml";
	String ENDPOINT_FXML = FXML_LOCATION + "/endpoint.fxml";
	String REQUEST_BODY_FXML = FXML_LOCATION + "/requestBody.fxml";
	String TEST_FXML = FXML_LOCATION + "/test.fxml";
	String LOGS_FXML = FXML_LOCATION + "/logs.fxml";

	// style
	String STYLE_LOCATION = "/style";
	String APPLICATION_ICON = STYLE_LOCATION + "/applicationIcon.png";
	String DEFAULT_STYLE = "default";
	String STYLE_SHEET_NAME = "stylesheet.css";

	// http client
	Integer DEFAULT_CONNECTION_TIMEOUT = 5000; // connection timeout in ms
	Integer DEFAULT_READ_TIMEOUT = 10000; // read timeout in ms

	static String getApplicationHome() {

		final String userHome = System.getProperty("user.home").replace("\\", "/");
		return userHome + "/.restui";
	}

	static Optional<String> getStyleUri(final String style) {

		Optional<String> opt = Optional.empty();
		String styleDir = style == null || style.isEmpty() ? "/" + DEFAULT_STYLE : "/" + style;

		File file = new File(getApplicationHome() + STYLE_LOCATION + styleDir + "/" + STYLE_SHEET_NAME);
		if (file.exists()) {
			opt = Optional.of("file:///" + getApplicationHome() + STYLE_LOCATION + styleDir + "/" + STYLE_SHEET_NAME);
		}
		return opt;
	}

	static ObservableList<String> getStyles() {
		ObservableList<String> styles = FXCollections.observableArrayList();

		File styleDirectory = new File(getApplicationHome() + STYLE_LOCATION);
		if (styleDirectory.exists()) {
			File[] files = styleDirectory.listFiles();
			for (File file : files) {
				if (file.isDirectory()) {
					styles.add(file.getName());
				}
			}
		}
		return styles;
	}

}
