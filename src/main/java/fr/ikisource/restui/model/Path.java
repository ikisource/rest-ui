package fr.ikisource.restui.model;

public class Path extends Item {

	private static final long serialVersionUID = 1L;

	public static final String ID_PREFIX = "{";
	public static final String ID_SUFFIX = "}";

	public Path() {
		super();
	}

	public Path(final Item parent, final String name) {
		super(parent, name);
	}

}
