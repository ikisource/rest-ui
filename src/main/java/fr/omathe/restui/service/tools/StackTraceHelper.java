package fr.omathe.restui.service.tools;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import fr.omathe.restui.service.Logger;
import fr.omathe.restui.service.Notifier;

public interface StackTraceHelper {

	static String toString(final Throwable throwable) {

		String s = "";
		if (throwable != null) {
			try (StringWriter errors = new StringWriter()) {
				throwable.printStackTrace(new PrintWriter(errors));
				s = errors.toString();
			} catch (IOException e) {
				Logger.error(e);
				Notifier.notifyError(e.getMessage());
			}
		}
		return s;
	}
}
