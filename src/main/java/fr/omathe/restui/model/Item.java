package fr.omathe.restui.model;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

public class Item implements Serializable {

	private static final long serialVersionUID = 1L;

	protected String uuid;
	protected Item parent;
	protected String name;
	protected Set<Item> children;

	public Item() {
		super();
		this.uuid = UUID.randomUUID().toString();
		this.children = new HashSet<>();
	}

	public Item(final Item parent, final String name) {
		super();
		this.parent = parent;
		this.uuid = UUID.randomUUID().toString();
		this.name = name;
		this.children = new HashSet<>();
	}

	public String getId() {
		return uuid;
	}

	public String getName() {
		return name;
	}

	public void setName(final String name) {
		this.name = name;
	}

	public Item getParent() {
		return parent;
	}

	public void setParent(final Item parent) {
		this.parent = parent;
	}

	public Set<Item> getChildren() {
		return children;
	}

	public void setChildren(final Set<Item> children) {
		this.children = children;
	}

	public void addChild(final Item child) {

		if (children == null) {
			children = new HashSet<>();
		}
		children.add(child);
		child.setParent(this);
	}

	public boolean hasChildren() {
		return children != null && !children.isEmpty();
	}

	public boolean hasChild(final String name) {

		return children != null && children.stream().filter(item -> item.getName().equalsIgnoreCase(name)).findFirst().isPresent();
	}

	public Stream<Item> getAllChildren() {
		return Stream.concat(Stream.of(this), children.stream().flatMap(Item::getAllChildren));
	}

	public Optional<Item> findChild(final String name) {

		return children.stream()
				.filter(item -> item.getName().equalsIgnoreCase(name))
				.findFirst();
	}

	@Override
	public String toString() {
		return name;
	}

}
