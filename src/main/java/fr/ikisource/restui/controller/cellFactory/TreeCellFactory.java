package fr.ikisource.restui.controller.cellFactory;

import java.util.Comparator;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import fr.ikisource.restui.model.Endpoint;
import fr.ikisource.restui.model.Item;
import fr.ikisource.restui.model.Project;
import fr.ikisource.restui.model.Path;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.control.cell.TextFieldTreeCell;
import javafx.scene.image.ImageView;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.TransferMode;

public class TreeCellFactory extends TextFieldTreeCell<Item> {

	private TextField textField;
	private final ContextMenu addMenu = new ContextMenu();
	private final MenuItem menuItemPath;
	private final MenuItem menuItemEndpoint;
	private final MenuItem menuDeleteItem;
	private final MenuItem menuAddBookmarkItem;
	private final MenuItem menuRemoveBookmarkItem;
	private final Node projectImageView;
	private final Node pathImageView;
	private final Node endpointImageView;
	final TreeView<Item> treeView;

	private static Comparator<TreeItem<Item>> itemName = (ti1, ti2) -> {
		if (ti1.getParent() == ti2.getParent()) {
			return ti1.getValue().getName().compareTo(ti2.getValue().getName());
		} else {
			return -1;
		}
	};

	private static Comparator<TreeItem<Item>> endpointType = (ti1, ti2) -> {
		return ti1.getValue().getClass().getName().compareTo(ti2.getValue().getClass().getName());
	};

	public static Comparator<TreeItem<Item>> comparator = (ti1, ti2) -> {
		return endpointType.thenComparing(itemName).compare(ti1, ti2);
	};

	public TreeCellFactory(final TreeView<Item> treeView, final Set<String> bookmarks) {

		this.treeView = treeView;
		menuItemPath = new MenuItem("New path");
		menuItemEndpoint = new MenuItem("New endpoint");
		menuDeleteItem = new MenuItem("Delete");
		menuAddBookmarkItem = new MenuItem("Add bookmark");
		menuRemoveBookmarkItem = new MenuItem("Remove bookmark");
		projectImageView = new ImageView();
		pathImageView = new ImageView();
		endpointImageView = new ImageView();
		projectImageView.setId("imageViewProject");
		endpointImageView.setId("imageViewEndpoint");
		pathImageView.setId("imageViewPath");

		menuItemPath.setOnAction(actionEvent -> {
			final TreeItem<Item> treeItem = treeView.getSelectionModel().getSelectedItem();
			final Item item = treeItem.getValue();

			final Path path = new Path(item, "New path");

			item.addChild(path);
			final TreeItem<Item> newItem = new TreeItem<>(path);
			treeItem.getChildren().add(newItem);
			treeView.getSelectionModel().select(newItem);
			treeItem.setExpanded(true);
		});

		menuItemEndpoint.setOnAction(actionEvent -> {
			final TreeItem<Item> treeItem = treeView.getSelectionModel().getSelectedItem();
			final Item item = treeItem.getValue();

			final Endpoint endPoint = new Endpoint(item, "New endpoint", "GET");
			item.addChild(endPoint);
			final TreeItem<Item> newItem = new TreeItem<>(endPoint);
			treeItem.getChildren().add(newItem);
			treeView.getSelectionModel().select(newItem);
			treeItem.setExpanded(true);
		});

		menuDeleteItem.setOnAction(actionEvent -> {
			final TreeItem<Item> treeItemToDelete = treeView.getSelectionModel().getSelectedItem();
			final Item itemToDelete = treeItemToDelete.getValue();

			final Alert alert = new Alert(AlertType.CONFIRMATION);
			alert.setTitle("Delete the item");
			alert.setHeaderText("Do you want to delete\n" + itemToDelete.getName());
			final Optional<ButtonType> result = alert.showAndWait();
			if (result.get() == ButtonType.OK) {
				if (treeItemToDelete.getParent() != null) {
					treeItemToDelete.getParent().getChildren().remove(treeItemToDelete);
					itemToDelete.getParent().getChildren().remove(itemToDelete);
				} else {
					treeView.setRoot(null);
				}
			}
		});

		menuAddBookmarkItem.setOnAction(actionEvent -> {
			final Item itemToBookmark = treeView.getSelectionModel().getSelectedItem().getValue();
			bookmarks.add(itemToBookmark.getName());
		});

		menuRemoveBookmarkItem.setOnAction(actionEvent -> {
			final Item itemToBookmark = treeView.getSelectionModel().getSelectedItem().getValue();
			bookmarks.remove(itemToBookmark.getName());
		});

		// Drag and drop management

		// (1)
		setOnDragDetected(e -> {
			final Item item = getItem();

			if (!(item instanceof Project)) {
				final Dragboard db = startDragAndDrop(TransferMode.MOVE);
				final ClipboardContent content = new ClipboardContent();
				content.putString(item.getId());
				db.setContent(content);
			}
			e.consume();
		});

		// (2)
		setOnDragOver(new EventHandler<DragEvent>() {

			@Override
			public void handle(final DragEvent event) {
				final Item item = getItem();
				if (event.getGestureSource() != event.getTarget() && !(item instanceof Endpoint)) {
					event.acceptTransferModes(TransferMode.MOVE);
				} else {
				}
				event.consume();
			}
		});

		// (3)
		setOnDragEntered(new EventHandler<DragEvent>() {
			@Override
			public void handle(final DragEvent event) {
				event.consume();
			}
		});

		// (4)
		setOnDragExited(new EventHandler<DragEvent>() {
			@Override
			public void handle(final DragEvent event) {
				event.consume();
			}
		});

		// (5)
		setOnDragDropped(new EventHandler<DragEvent>() {
			@Override
			public void handle(final DragEvent event) {
				final Item item = getItem();
				if (!(item instanceof Endpoint)) {
					final Dragboard db = event.getDragboard();
					boolean success = false;
					if (db.hasString()) {
						final Optional<TreeItem<Item>> source = findTreeItemByItemId(treeView.getRoot(), db.getString());

						if (source.isPresent()) {
							final TreeItem<Item> sourceTreeItem = source.get();
							// removes source from parent
							sourceTreeItem.getParent().getChildren().remove(sourceTreeItem);
							sourceTreeItem.getValue().getParent().getChildren().remove(sourceTreeItem.getValue());

							// add source to target
							final TreeItem<Item> targetTreeItem = getTreeItem();
							targetTreeItem.getChildren().add(sourceTreeItem);
							sourceTreeItem.getValue().setParent(targetTreeItem.getValue());
						}
						success = true;
					}
					event.setDropCompleted(success);
				}
				event.consume();
			}
		});

		// (6)
		setOnDragDone(new EventHandler<DragEvent>() {
			@Override
			public void handle(final DragEvent event) {
				event.consume();
			}
		});

		// contextual menu
		treeView.setContextMenu(addMenu);
		addMenu.getItems().add(menuRemoveBookmarkItem);

		treeView.setOnContextMenuRequested(e -> {
			final Item selectedItem = treeView.getSelectionModel().getSelectedItem().getValue();
			if (selectedItem != null) {
				addMenu.getItems().clear();
				if (selectedItem instanceof Project || selectedItem instanceof Path) {
					addMenu.getItems().add(menuItemPath);
					addMenu.getItems().add(menuItemEndpoint);
					addMenu.getItems().add(menuDeleteItem);
					setGraphic(selectedItem instanceof Project ? projectImageView : pathImageView);
				} else if (selectedItem instanceof Endpoint) {
					addMenu.getItems().add(menuDeleteItem);
					if (bookmarks.contains(selectedItem.getName())) {
						addMenu.getItems().add(menuRemoveBookmarkItem);
					} else {
						addMenu.getItems().add(menuAddBookmarkItem);
					}
					setGraphic(endpointImageView);
				}
			}
		});
	}

	@Override
	public void updateItem(final Item item, final boolean empty) {
		super.updateItem(item, empty);

		if (empty) {
			setText(null);
			setGraphic(null);
		} else {
			if (isEditing()) {
				if (textField != null) {
					textField.setText(getString());
				}
				setText(null);
				setGraphic(textField);
			} else {
				if (textField != null) {
					textField.setText(getString());
				}
				setText(getString());
				setGraphic(getTreeItem().getGraphic());

				// icons
				if (item instanceof Project) {
					setGraphic(projectImageView);
				} else if (item instanceof Endpoint) {
					setGraphic(endpointImageView);
				}
			}
		}
	}

	@Override
	public void startEdit() {
		super.startEdit();

		if (textField == null) {
			createTextField();
		}
		setText(null);
		setGraphic(textField);
		textField.selectAll();
	}

	private void createTextField() {

		textField = new TextField(getString());
		textField.focusedProperty().addListener((observableValue, oldValue, newValue) -> {
			Platform.runLater(new Runnable() {
				@Override
				public void run() {
					if (textField.isFocused() && !textField.getText().isEmpty()) {
						textField.selectAll();
					}
				}
			});
		});

		textField.setOnKeyReleased(new EventHandler<KeyEvent>() {

			@Override
			public void handle(final KeyEvent e) {
				if (e.getCode().equals(KeyCode.ENTER)) {

					if (getTreeItem().getParent() != null) {
						Item parent = getTreeItem().getParent().getValue();
						if (parent.hasChild(textField.getText())) {
							Alert alert = new Alert(AlertType.ERROR);
							alert.setTitle("Error" );
							alert.setHeaderText("Failed to rename");
							alert.setContentText("'" + textField.getText() + "' already exists for parent \n'"
							+ getTreeItem().getParent().getValue().getName() + "'");
							alert.showAndWait();
						}
						else {
							getItem().setName(textField.getText());
							commitEdit(getItem());
							// sort the items
							getTreeItem().getParent().getChildren().sort(comparator);
						}
					} else {
						// project item
						getItem().setName(textField.getText());
						commitEdit(getItem());
					}

					if (getItem() instanceof Path) { // renaming all the endpoints path
						Item currentItem = getItem();
						while (currentItem.hasChildren()) {
							for (final Item child : currentItem.getChildren()) {
								if (child instanceof Endpoint) {
									final Endpoint endpoint = (Endpoint) child;
									endpoint.buildPath();
								}
								currentItem = child;
							}
						}
					}
				} else if (e.getCode() == KeyCode.ESCAPE) {
					cancelEdit();
				}
			}
		});
	}

	private Optional<TreeItem<Item>> findTreeItemByItemId(final TreeItem<Item> parent, final String itemId) {

		return flattened(treeView.getRoot())
				.filter(ti -> ti.getValue().getId().equals(itemId))
				.findFirst();
	}

	private static Stream<TreeItem<Item>> flattened(final TreeItem<Item> parent) {

		return Stream.concat(
				Stream.of(parent),
				parent.getChildren().stream().flatMap(a -> TreeCellFactory.flattened(a)));
	}

	private String getString() {
		return getItem() == null ? "" : getItem().toString();
	}
}
