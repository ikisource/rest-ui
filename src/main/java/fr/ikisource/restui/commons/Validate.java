package fr.ikisource.restui.commons;

public interface Validate {

	static void notNull(final Object object, final String name) {

		if (object == null) {
			throw new IllegalArgumentException(name + " must be defined.");
		}
	}

}
