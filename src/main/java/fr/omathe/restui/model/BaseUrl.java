package fr.omathe.restui.model;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * An BasUrl represents the first part of an absolute URL Example :
 * http://192.168.0.5:8080/api is the base URL of the absolute URL
 * http://192.168.0.5:8080/api/users/20
 *
 * @author Olivier MATHÉ
 */
public class BaseUrl {

	private static final long serialVersionUID = 1L;

	private final StringProperty name;
	private final StringProperty url;
	private final BooleanProperty enabled;

	public BaseUrl(final String name, final String url, Boolean enabled) {
		this.name = new SimpleStringProperty(name);
		this.url = new SimpleStringProperty(url);
		this.enabled = new SimpleBooleanProperty(enabled);
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

	public String getUrl() {
		return url.get();
	}

	public void setUrl(final String url) {
		this.url.set(url);
	}

	public StringProperty urlProperty() {
		return url;
	}

	public Boolean getEnabled() {
		return enabled.get();
	}

	public void setEnabled(final Boolean enabled) {
		this.enabled.set(enabled);
	}

	public BooleanProperty enabledProperty() {
		return enabled;
	}

	@Override
	public String toString() {
		return "BaseUrl [name=" + name.get() + ", url=" + url.get() + ", enabled=" + enabled.get() + "]";
	}

}
