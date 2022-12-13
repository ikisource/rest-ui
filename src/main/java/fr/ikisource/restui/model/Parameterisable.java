package fr.ikisource.restui.model;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import fr.ikisource.restui.model.Parameter.Direction;
import fr.ikisource.restui.model.Parameter.Location;
import fr.ikisource.restui.model.Parameter.Type;

public interface Parameterisable {

	List<Parameter> getParameters();

	default void addParameter(final Parameter parameter) {

		if (!getParameters().contains(parameter)) {
			getParameters().add(parameter);
		}
	}

	default void addParameters(final List<Parameter> parameterList) {

		parameterList.stream().forEach(p -> {
			addParameter(p);
		});
	}

	default String getRequestRawBody() {

		Optional<String> body = getParameters().stream()
				.filter(p -> p.isRequestParameter())
				.filter(p -> p.isRawBodyParameter())
				.map(p -> p.getValue()).findFirst();

		return body.orElse(null);
	}

	default void setRequestRawBody(final String value) {

		final Optional<Parameter> rawBody = getParameters().stream()
				.filter(p -> p.isRequestParameter())
				.filter(p -> p.isRawBodyParameter())
				.filter(p -> p.isTypeText())
				.findFirst();
		if (rawBody.isPresent()) {
			if (value == null || value.isEmpty()) {
				getParameters().remove(rawBody.get());
			} else {
				rawBody.get().setValue(value);
			}
		} else if (value != null && !value.isEmpty()) {
			final Parameter parameter = new Parameter(Boolean.TRUE, Direction.REQUEST, Location.BODY, Type.TEXT, null, value);
			addParameter(parameter);
		}
	}

	default void removeParameters(final List<Parameter> parameters) {

		if (parameters != null) {
			getParameters().removeAll(parameters);
		}
	}

	default boolean hasParameters() {
		return getParameters() != null && !getParameters().isEmpty();
	}

	default boolean containsParameter(final Parameter parameter) {

		return getParameters().contains(parameter);
	}

	default Optional<Parameter> findParameter(final Predicate<Parameter> predicate) {

		return predicate == null ? Optional.empty() : getParameters().stream().filter(predicate).findFirst();
	}
}
