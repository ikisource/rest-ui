package fr.ikisource.restui.service;

import fr.ikisource.restui.controller.ControllerManager;
import javafx.scene.paint.Color;

public interface Notifier {

	static void notifyInfo(final String message) {

		ControllerManager.getMainController().getBottomController().getNotification().setText(message);
		ControllerManager.getMainController().getBottomController().getNotification().setTextFill(Color.BLACK);
	}
	
	static void notifyError(final String message) {
		
		ControllerManager.getMainController().getBottomController().getNotification().setText(message);
		ControllerManager.getMainController().getBottomController().getNotification().setTextFill(Color.RED);
	}
	
	static void clear() {
		
		ControllerManager.getMainController().getBottomController().getNotification().setText("");
	}
}
