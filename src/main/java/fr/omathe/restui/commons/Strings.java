package fr.omathe.restui.commons;

import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

public interface Strings {

	static boolean isNullOrEmpty(final String string) {

		return string == null || string.isEmpty();
	}

	/**
	 * Build a base 64 string from an UUID
	 * @return A String
	 */
	static String generate() {

		final UUID uuid2 = UUID.randomUUID();
		final ByteBuffer byteBuffer = ByteBuffer.wrap(new byte[16]);
		byteBuffer.putLong(uuid2.getMostSignificantBits());
		byteBuffer.putLong(uuid2.getLeastSignificantBits());
		final String uuid64 = Base64.getUrlEncoder().withoutPadding().encodeToString(byteBuffer.array());

		return uuid64;
	}

	/**
	 * Return the next value according to the existing values in the list and the pattern
	 * It is case sensitive
	 * @param list - The values ("name 1", "name 2")
	 * @param pattern - The pattern used to search values ("name")
	 * @return A String representing the next occurrence ("name 3")
	 */
	static String getNextValue(List<String> list, String pattern) {

		String next = pattern;

		int index = 1;
		boolean found = true;
		while(found) {
			if (list.contains(pattern + " " + index)) {
				index++;
			} else {
				found = false;
			}
		}
		next = pattern + " " + index;
		return next;
	}

}
