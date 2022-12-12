package fr.omathe.restui.service.tools;

import java.text.SimpleDateFormat;
import java.util.TimeZone;

public interface DateFormater {

	static String iso(final Long date, final String timeZone) {
		
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("YYYY-MM-dd HH:mm:ss");
		simpleDateFormat.setTimeZone(TimeZone.getTimeZone(timeZone));
		return simpleDateFormat.format(date);
	}
}
