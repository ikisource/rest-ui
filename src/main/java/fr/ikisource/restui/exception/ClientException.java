package fr.ikisource.restui.exception;

public class ClientException extends Exception {

	private static final long serialVersionUID = 1L;

	public ClientException(final String message) {
		super(message);
	}

	public ClientException(final String object, final String value) {

		super(object + " '" + value + "' not found");
	}
}
