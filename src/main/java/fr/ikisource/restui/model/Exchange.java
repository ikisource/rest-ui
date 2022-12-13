package fr.ikisource.restui.model;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import fr.ikisource.restui.model.Parameter.Direction;
import fr.ikisource.restui.model.Parameter.Location;
import fr.ikisource.restui.model.Parameter.Type;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;

public class Exchange implements Parameterisable {

	public enum BodyType {
		RAW, X_WWW_FORM_URL_ENCODED, FORM_DATA;
	}

	private final StringProperty name;
	private final ObjectProperty<Long> date;
	private final IntegerProperty status;
	private final IntegerProperty duration;
	private BodyType requestBodyType;
	private final StringProperty uri;
	private List<Parameter> parameters;

	public Exchange(final String name, final Long date) {
		super();
		this.name = new SimpleStringProperty(name);
		this.date = new SimpleObjectProperty<>(date);
		this.status = new SimpleIntegerProperty();
		this.duration = new SimpleIntegerProperty();
		this.requestBodyType = BodyType.RAW;
		this.uri = new SimpleStringProperty("");
		this.parameters = FXCollections.observableArrayList();
	}

	public Exchange(final String endpointName, final String name, final Long date, final Integer duration, final Integer status, final BodyType requestBodyType) {
		super();
		this.name = new SimpleStringProperty(name);
		this.date = new SimpleObjectProperty<>(date);
		this.status = new SimpleIntegerProperty(status);
		this.duration = new SimpleIntegerProperty(duration);
		this.requestBodyType = requestBodyType;
		this.uri = new SimpleStringProperty("");
		this.parameters = FXCollections.observableArrayList();
	}

	public Exchange duplicate(final String name) {
		final Exchange duplicate = new Exchange(name, Instant.now().toEpochMilli());

		duplicate.addParameters(parameters.stream().map(p -> p.duplicate()).collect(Collectors.toList()));
		duplicate.setUri(uri.get());
		duplicate.setDuration(duration.get());
		duplicate.setStatus(status.get());

		return duplicate;
	}

	public static Exchange buildWorkingExchange() {

		return new Exchange("", Instant.now().toEpochMilli());
	}

	public void updateValues(final Exchange source) {
		setDate(source.getDate());
		setStatus(source.getStatus());
		setDuration(source.getDuration());
		setRequestBodyType(source.getRequestBodyType());
		setUri(source.getUri());
		setParameters(source.getParameters());
	}

	public String getName() {
		return name.get();
	}

	public void setName(final String name) {
		this.name.set(name);
	}

	public StringProperty nameProperty() {
		return name;
	}

	public Long getDate() {
		return date.get();
	}

	public void setDate(final Long date) {
		this.date.set(date);
	}

	public ObjectProperty<Long> dateProperty() {
		return date;
	}

	@Override
	public List<Parameter> getParameters() {
		return parameters;
	}

	public void setParameters(final List<Parameter> parameters) {
		this.parameters = parameters;
	}

	public String getResponseBody() {

		String responseBody = "";
		final Optional<Parameter> rawBody = parameters.stream()
				.filter(p -> p.getDirection().equals(Direction.RESPONSE.name()))
				.filter(p -> p.getLocation().equals(Location.BODY.name()))
				.filter(p -> p.getType().equals(Type.TEXT.name()))
				.findFirst();
		if (rawBody.isPresent()) {
			responseBody = rawBody.get().getValue();
		}
		return responseBody;
	}

	public void clearResponseParameters() {
		parameters.removeIf(p -> p.isResponseParameter());
	}

	public Optional<Parameter> findParameter(final Direction direction, final Location location, final String name) {

		return parameters.stream()
				.filter(p -> p.getDirection().equals(direction.name()) && p.getLocation().equals(location.name()) && p.getName().equalsIgnoreCase(name))
				.findFirst();
	}

	public BodyType getRequestBodyType() {
		return requestBodyType;
	}

	public void setRequestBodyType(final BodyType requestBodyType) {
		this.requestBodyType = requestBodyType;
	}

	public String getUri() {
		return uri.get();
	}

	public void setUri(final String uri) {
		this.uri.set(uri);
	}

	public StringProperty uriProperty() {
		return uri;
	}

	public IntegerProperty statusProperty() {
		return status;
	}

	public Integer getStatus() {
		return status.get();
	}

	public void setStatus(final Integer status) {
		this.status.set(status);
	}

	public IntegerProperty durationProperty() {
		return duration;
	}

	public Integer getDuration() {
		return duration.get();
	}

	public void setDuration(final Integer duration) {
		this.duration.set(duration);
	}

	public boolean isEmpty() {
		return parameters == null || parameters.isEmpty();
	}

	public boolean isWorking() {
		return name.get().isEmpty();
	}

	public boolean isFinalized() {

		return status.get() != 0 && !name.get().isEmpty();
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		Exchange other = (Exchange) obj;
		if (name == null) {
			if (other.name != null) {
				return false;
			}
		} else if (!name.get().equalsIgnoreCase(other.name.get())) {
			return false;
		}
		return true;
	}

}
