package fr.omathe.restui.model;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * An host is a name and an URL
 *
 * @author Olivier MATHE
 */
public class Host {

	private static final long serialVersionUID = 1L;

	private final StringProperty name;
	private final StringProperty address;

	public Host(final String name, final String address) {
		this.name = new SimpleStringProperty(name);
		this.address = new SimpleStringProperty(address);
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

	public String getAddress() {
		return address.get();
	}

	public void setAddress(final String address) {
		this.address.set(address);
	}

	public StringProperty addressProperty() {
		return address;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Host other = (Host) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "Host [name=" + name.get() + ", address=" + address.get() + "]";
	}

}
