package fr.omathe.restui.service;

import java.time.Instant;
import java.time.ZoneId;

import fr.omathe.restui.controller.ControllerManager;
import fr.omathe.restui.service.tools.DateFormater;
import fr.omathe.restui.service.tools.StackTraceHelper;

public interface Logger {

	enum Level {
		INFO, DEBUG, ERROR;
	}

	static void info(final Throwable throwable) {

		ControllerManager.getLogsController().getLogsArea().appendText(buildLog(StackTraceHelper.toString(throwable), Level.INFO));
	}

	static void info(final String text) {

		ControllerManager.getLogsController().getLogsArea().appendText(buildLog(text, Level.INFO));
	}

	static void debug(final Throwable throwable) {

		ControllerManager.getLogsController().getLogsArea().appendText(buildLog(StackTraceHelper.toString(throwable), Level.DEBUG));
	}

	static void debug(final String text) {

		ControllerManager.getLogsController().getLogsArea().appendText(buildLog(text, Level.DEBUG));
	}

	static void error(final Throwable throwable) {

		ControllerManager.getLogsController().getLogsArea().appendText(buildLog(StackTraceHelper.toString(throwable), Level.ERROR));
	}

	static void error(final String text) {

		ControllerManager.getLogsController().getLogsArea().appendText(buildLog(text, Level.ERROR));
	}

	private static String buildLog(final String text, final Level level) {

		return DateFormater.iso(Instant.now().toEpochMilli(), ZoneId.systemDefault().getId()) + " [" + level.name() + "] " + text + "\n";
	}
}
