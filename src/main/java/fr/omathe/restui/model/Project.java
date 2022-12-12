package fr.omathe.restui.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public class Project extends Item {

	private static final long serialVersionUID = 1L;

	private final List<Endpoint> endpoints;

	public Project(final String name) {
		super(null, name);
		this.endpoints = new ArrayList<>();
	}

	public void addEnpoint(final Endpoint endpoint) {
		this.endpoints.add(endpoint);
	}

	public List<Endpoint> getEndpoints() {
		return endpoints;
	}

	//	public List<Exchange> getExchanges() {
	//
	//		List<Exchange> exchanges = new ArrayList<>();
	//
	//		Stream<Item> stream = Stream.concat(Stream.of(this), children.stream().flatMap(Item::getAllChildren));
	//		if (stream != null) {
	//			stream.filter(item -> item instanceof Endpoint).forEach(item -> {
	//				Endpoint endpoint = (Endpoint) item;
	//				exchanges.addAll(endpoint.getExchanges());
	//			});
	//		}
	//		return exchanges;
	//	}

	public Optional<Exchange> findExchangeByNameAndEndpointName(final String name, final String endpointName) {

		Optional<Exchange> exchange = Optional.empty();

		Stream<Item> stream = getAllChildren();

		if (stream != null) {
			Optional<Endpoint> endpoint = stream
					.filter(item -> item instanceof Endpoint)
					.map(item -> (Endpoint) item)
					.filter(e -> e.name.equals(endpointName))
					.findFirst();
			if (endpoint.isPresent()) {
				List<Exchange> exchanges = endpoint.get().getExchanges();
				exchange = exchanges.stream()
						.filter(e -> e.getName().equals(name))
						.findFirst();
			}
		}
		return exchange;
	}

	public Optional<Endpoint> findEndpoint(final String name) {

		return getAllChildren()
				.filter(item -> item instanceof Endpoint)
				.filter(item -> item.getName().equals(name))
				.map(item -> (Endpoint) item)
				.findFirst();
	}
}
