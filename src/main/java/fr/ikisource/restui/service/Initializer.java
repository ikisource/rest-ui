package fr.ikisource.restui.service;

import java.io.File;

import fr.ikisource.restui.conf.App;
import fr.ikisource.restui.commons.ResourceHelper;

/**
 * Initialize the application
 * @author Olivier MATHE
 */
public interface Initializer {

	static void build() {
		Initializer.createDefaultApplicationFile();
		Initializer.copyStyles();
	}

	private static void createDefaultApplicationFile() {

		File applicationFile = new File(App.APPLICATION_FILE);
		if (!applicationFile.exists()) {
			ApplicationService.createDefaultApplicationFile();
		}
	}

	private static void copyStyles() {

		try {
			ResourceHelper.copyResource(App.STYLE_LOCATION, App.getApplicationHome());
		} catch (Exception e) {
			Logger.error(e);
			Notifier.notifyError(e.getMessage());
		}
	}

}
