package fr.omathe.restui.controller;

import java.io.IOException;
import java.io.InputStream;

import fr.omathe.restui.conf.App;
import fr.omathe.restui.service.Logger;
import fr.omathe.restui.service.Notifier;
import javafx.fxml.FXMLLoader;

public class ControllerManager {

	private static ProjectController projectController;
	private static EndpointController endpointController;
	private static MainController mainController;
	private static RequestBodyController requestBodyController;
	private static LogsController logsController;

	public static MainController getMainController() {

		if (mainController == null) {
			try(InputStream is = ControllerManager.class.getResource(App.MAIN_FXML).openStream()) {
				FXMLLoader fxmlLoader = new FXMLLoader();
				fxmlLoader.load(is);
				mainController = (MainController) fxmlLoader.getController();
			} catch (IOException e) {
				Logger.error(e);
				Notifier.notifyError(e.getMessage());
			}
		}
		return mainController;
	}

	public static ProjectController getProjectController() {

		if (projectController == null) {
			try {
				FXMLLoader fxmlLoader = new FXMLLoader();
				fxmlLoader.load(ControllerManager.class.getResource(App.PROJECT_FXML).openStream());
				projectController = (ProjectController) fxmlLoader.getController();
			} catch (IOException e) {
				Logger.error(e);
				Notifier.notifyError(e.getMessage());
			}
		}
		return projectController;
	}

	public static EndpointController getEndpointController() {

		if (endpointController == null) {
			try {
				FXMLLoader fxmlLoader = new FXMLLoader();
				fxmlLoader.load(ControllerManager.class.getResource(App.ENDPOINT_FXML).openStream());
				endpointController = (EndpointController) fxmlLoader.getController();
			} catch (IOException e) {
				Logger.error(e);
				Notifier.notifyError(e.getMessage());
			}
		}
		return endpointController;
	}

	public static RequestBodyController getRequestBodyController() {

		if (requestBodyController == null) {
			try {
				FXMLLoader fxmlLoader = new FXMLLoader();
				fxmlLoader.load(ControllerManager.class.getResource(App.REQUEST_BODY_FXML).openStream());
				requestBodyController = (RequestBodyController) fxmlLoader.getController();
			} catch (IOException e) {
				Logger.error(e);
				Notifier.notifyError(e.getMessage());
			}
		}
		return requestBodyController;
	}

	public static LogsController getLogsController() {

		if (logsController == null) {
			try {
				FXMLLoader fxmlLoader = new FXMLLoader();
				fxmlLoader.load(ControllerManager.class.getResource(App.LOGS_FXML).openStream());
				logsController = (LogsController) fxmlLoader.getController();
			} catch (IOException e) {
				Logger.error(e);
				Notifier.notifyError(e.getMessage());
			}
		}
		return logsController;
	}

}
