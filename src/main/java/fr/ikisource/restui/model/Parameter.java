package fr.ikisource.restui.model;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class Parameter {

	public static Set<String> locations = Arrays.stream(Location.values()).map(e -> e.name()).collect(Collectors.toSet());
	public static Set<String> types = Arrays.stream(Type.values()).map(e -> e.name()).collect(Collectors.toSet());
	public static Set<String> headerNames = Stream.of("Accept", "Authorization", "Content-Type").collect(Collectors.toSet());

	public enum Location {
		BODY, HEADER, PATH, QUERY;
	}

	public enum Direction {
		REQUEST, RESPONSE;
	}

	public enum Type {
		TEXT, FILE;
	}

	private final BooleanProperty enabled;
	private final StringProperty type;
	private final StringProperty location;
	private final StringProperty direction;
	private final StringProperty name;
	private StringProperty value;

	public Parameter(final Boolean enabled, final Direction direction, final Location location, final Type type, final String name, final String value) {
		super();
		this.enabled = new SimpleBooleanProperty(enabled);
		this.direction = new SimpleStringProperty(direction.name());
		this.location = new SimpleStringProperty(location.name());
		this.type = new SimpleStringProperty(type.name());
		this.name = new SimpleStringProperty(name);
		this.value = new SimpleStringProperty(value);
	}

	public Parameter(final Parameter parameter) {
		super();
		this.enabled = new SimpleBooleanProperty(parameter.getEnabled());
		this.direction = new SimpleStringProperty(parameter.getDirection());
		this.location = new SimpleStringProperty(parameter.getLocation());
		this.type = new SimpleStringProperty(parameter.getType());
		this.name = new SimpleStringProperty(parameter.getName());
		this.value = new SimpleStringProperty(parameter.getValue());
	}

	public Parameter duplicate() {
		return new Parameter(enabled.get(), Direction.valueOf(direction.get()), Location.valueOf(location.get()), Type.valueOf(type.get()), name.get(), value.get());
	}

	public Parameter duplicateValue() {
		Parameter parameter = this;
		parameter.value = new SimpleStringProperty(parameter.getValue());
		return this;
	}

	public Boolean getEnabled() {
		return enabled.get();
	}

	public StringProperty typeProperty() {
		return type;
	}

	public String getType() {
		return type.get();
	}

	public void setType(final Type type) {
		this.type.set(type.name());
	}

	public void setEnabled(final Boolean enabled) {
		if (!isPathParameter()) {
			this.enabled.set(enabled);
		} else {
			this.enabled.set(true);
		}
	}

	public BooleanProperty enabledProperty() {
		return enabled;
	}

	public String getLocation() {
		return location.get();
	}

	public void setLocation(final Location location) {
		this.location.set(location.name());
	}

	public StringProperty locationProperty() {
		return location;
	}

	public String getDirection() {
		return direction.get();
	}

	public void setDirection(final Direction direction) {
		this.direction.set(direction.name());
	}

	public StringProperty directionProperty() {
		return direction;
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

	public String getValue() {
		return value.get();
	}

	public void setValue(final String value) {
		this.value.set(value);
	}

	public StringProperty valueProperty() {
		return value;
	}

	public boolean isRequestParameter() {
		return direction != null && direction.get().equals(Direction.REQUEST.name());
	}

	public boolean isResponseParameter() {
		return direction != null && direction.get().equals(Direction.RESPONSE.name());
	}

	public boolean isPathParameter() {
		return location != null && location.get().equals(Location.PATH.name());
	}

	public boolean isQueryParameter() {
		return location != null && location.get().equals(Location.QUERY.name());
	}

	public boolean isBodyParameter() {
		return location != null && location.get().equals(Location.BODY.name()) && getName() != null && !getName().trim().isEmpty();
	}

	public boolean isRawBodyParameter() {
		return location != null && location.get().equals(Location.BODY.name());
	}

	public boolean isHeaderParameter() {
		return location != null && location.get().equals(Location.HEADER.name());
	}

	public boolean isTypeText() {
		return type != null && type.get().equals(Type.TEXT.name());
	}

	public boolean isTypeFile() {
		return type != null && type.get().equals(Type.FILE.name());
	}

	public boolean hasNameNullOrEmpty() {
		return name.get() == null || (name.get() != null && name.get().trim().isEmpty());
	}

	public boolean queryParameterValid() {
		if (!getLocation().equals(Location.QUERY.name())) {
			return true;
		}
		return getName() != null && !getName().trim().isEmpty() && getValue() != null && !getValue().trim().isEmpty();
	}

	public boolean isValid() {

		boolean valid = false;
		if (getLocation().equals(Location.QUERY.name()) || getLocation().equals(Location.HEADER.name())) {
			valid = getName() != null && !getName().trim().isEmpty() && getValue() != null && !getValue().trim().isEmpty();
		} else if (getLocation().equals(Location.PATH.name())) {
			valid = getEnabled() && getValue() != null && !getValue().trim().isEmpty();
		} else if (getLocation().equals(Location.BODY.name())) {
			valid = getValue() != null && !getValue().trim().isEmpty();
		}
		return valid;
	}

	public boolean nameIs(final String name) {

		return getName() != null && getName().equalsIgnoreCase(name);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((direction.get() == null) ? 0 : direction.get().hashCode());
		result = prime * result + ((location.get() == null) ? 0 : location.get().hashCode());
		result = prime * result + ((name.get() == null) ? 0 : name.get().hashCode());
		result = prime * result + ((type.get() == null) ? 0 : type.get().hashCode());
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

		Parameter other = (Parameter) obj;

		if (direction.get() == null) {
			if (other.direction.get() != null) {
				return false;
			}
		} else if (!direction.get().equals(other.direction.get())) {
			return false;
		}
		if (location.get() == null) {
			if (other.location.get() != null) {
				return false;
			}
		} else if (!location.get().equals(other.location.get())) {
			return false;
		}
		if (name.get() == null) {
			if (other.name.get() != null) {
				return false;
			}
		} else if (!name.get().equals(other.name.get())) {
			return false;
		}
		if (type.get() == null) {
			if (other.type.get() != null) {
				return false;
			}
		} else if (!type.get().equals(other.type.get())) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return "Parameter [enabled=" + enabled.get() + ", direction=" + direction.get() + ", location=" + location.get() + ", type=" + type.get() + ", name=" + name.get() + ", value=" + value.get() + "]";
	}

}
