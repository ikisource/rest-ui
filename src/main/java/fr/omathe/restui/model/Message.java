package fr.omathe.restui.model;

import java.util.List;
import java.util.Optional;

import fr.omathe.restui.model.Parameter.Direction;
import fr.omathe.restui.model.Parameter.Location;
import fr.omathe.restui.model.Parameter.Type;
import javafx.collections.FXCollections;

public class Message {

	protected List<Parameter> parameters;

	public Message() {
		super();
		this.parameters = FXCollections.observableArrayList();
	}

	public String getRawBody() {

		final Optional<String> body = parameters.stream()
				.filter(p -> p.getLocation().equals(Location.BODY.name()) && p.getName() == null)
				.map(p -> p.getValue())
				.findFirst();

		return body.orElse(null);
	}

	public void setRawBody(final String value) {

		final Optional<Parameter> rawBody = parameters.stream()
				.filter(p -> p.getLocation().equals(Location.BODY.name()))
				.filter(p -> p.getType().equals(Type.TEXT.name()))
				.filter(p -> p.getName() == null)
				.findFirst();
		if (rawBody.isPresent()) {
			rawBody.get().setValue(value);
		}
		else {
//			final Parameter parameter = new Parameter(Boolean.TRUE, Type.TEXT, Location.BODY, null, value);
			final Parameter parameter = new Parameter(Boolean.TRUE, Direction.REQUEST, Location.BODY, Type.TEXT, null, value);
			addParameter(parameter);
		}
	}

	public List<Parameter> getParameters() {
		return parameters;
	}

	public void addParameter(final Parameter parameter) {

		if (!parameters.contains(parameter)) {
			parameters.add(parameter);
		}
	}

	public Optional<Parameter> findParameter(final Location location, final String name) {

		return parameters.stream()
				.filter(p -> p.isHeaderParameter() && p.getName().equalsIgnoreCase(name))
				.findFirst();
	}
}
