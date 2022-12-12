package fr.omathe.restui.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import fr.omathe.restui.model.Parameter;

public class ObjectClipboard {

	private final List<Parameter> parameters;

	private ObjectClipboard() {
		parameters = new ArrayList<>();
	}

	private static class SingletonHolder {
		private final static ObjectClipboard objectClipboard = new ObjectClipboard();
	}

	public static ObjectClipboard getInstance() {
		return SingletonHolder.objectClipboard;
	}

	public List<Parameter> getParameters() {
		return parameters;
	}

	public void setParameters(final List<Parameter> parameters) {

		this.parameters.clear();
		final List<Parameter> copy = parameters.stream()
				.map(p -> new Parameter(p))
				.collect(Collectors.toList());
		this.parameters.addAll(copy);
	}

}
