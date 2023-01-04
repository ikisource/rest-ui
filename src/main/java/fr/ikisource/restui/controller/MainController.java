package fr.ikisource.restui.controller;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import fr.ikisource.restui.commons.AlertBuilder;
import fr.ikisource.restui.commons.Strings;
import fr.ikisource.restui.conf.App;
import fr.ikisource.restui.controller.cellFactory.BaseNameCellFactory;
import fr.ikisource.restui.controller.cellFactory.BaseUrlCellFactory;
import fr.ikisource.restui.controller.cellFactory.RadioButtonCell;
import fr.ikisource.restui.controller.cellFactory.TreeCellFactory;
import fr.ikisource.restui.exception.NotFoundException;
import fr.ikisource.restui.exception.TechnicalException;
import fr.ikisource.restui.model.Application;
import fr.ikisource.restui.model.BaseUrl;
import fr.ikisource.restui.model.Endpoint;
import fr.ikisource.restui.model.Item;
import fr.ikisource.restui.model.Parameter;
import fr.ikisource.restui.model.Parameter.Direction;
import fr.ikisource.restui.model.Parameter.Location;
import fr.ikisource.restui.model.Project;
import fr.ikisource.restui.model.Version;
import fr.ikisource.restui.service.ApplicationService;
import fr.ikisource.restui.service.ExchangesService;
import fr.ikisource.restui.service.Logger;
import fr.ikisource.restui.service.ProjectService;
import fr.ikisource.restui.service.RestClient;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.util.Callback;

public class MainController implements Initializable {

	@FXML
	private BottomController bottomController; // bottom controller

	private final Map<String, Node> centerNodes = new HashMap<String, Node>();
	private WebView webView;
	private WebEngine webEngine;

	public static ObjectProperty<BaseUrl> baseUrlProperty = new SimpleObjectProperty<BaseUrl>(new BaseUrl("", "", false)); // selected base url

	@FXML
	private BorderPane rootNode;

	@FXML
	private TreeView<Item> treeView;

	@FXML
	private ComboBox<String> searchItem;

	@FXML
	private VBox vBox;

	@FXML
	private Label searchCount;

	@FXML
	private TabPane topTabPane;

	// settings tab
	@FXML
	private TextField readTimeout;

	@FXML
	private TextField connectionTimeout;

	@FXML
	private ComboBox<String> style;

	@FXML
	private TextField headerName;

	@FXML
	private TextField headerValue;

	// Web tab
	@FXML
	private ComboBox<String> webUrl;

	@FXML
	private TableView<BaseUrl> baseUrlTable;

	@FXML
	private TableColumn<BaseUrl, String> baseUrlNameColumn;

	@FXML
	private TableColumn<BaseUrl, String> baseUrlUrlColumn;

	@FXML
	private TableColumn<BaseUrl, Boolean> baseUrlEnabledColumn;

	@FXML
	private Button importEndpointsButton;

	public static Application application;
	private File projectFile;
	private Set<String> bookmarks;
	TreeCellFactory treeCellFactory;

	int index = 0;

	@Override
	public void initialize(final URL location, final ResourceBundle resources) {

		application = ApplicationService.openApplication();

		Optional<BaseUrl> optionalBaseUrl = application.getEnabledBaseUrl();
		if (optionalBaseUrl.isPresent()) {
			baseUrlProperty.get().enabledProperty().set(optionalBaseUrl.get().getEnabled());
			baseUrlProperty.get().nameProperty().set(optionalBaseUrl.get().getName());
			baseUrlProperty.get().urlProperty().set(optionalBaseUrl.get().getUrl());
		}

		importEndpointsButton.disableProperty().bind(baseUrlProperty.get().enabledProperty().not());

		// load last project
		String lastProjectUri = application.getLastProjectUri();
		if (lastProjectUri != null && !lastProjectUri.isEmpty() && lastProjectUri.startsWith("file")) {
			loadProject(URI.create(application.getLastProjectUri()));
		}

		bookmarks = new HashSet<>();

		treeView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
		treeView.setEditable(true);

		treeView.setCellFactory(new Callback<TreeView<Item>, TreeCell<Item>>() {

			@Override
			public TreeCell<Item> call(final TreeView<Item> param) {
				return new TreeCellFactory(treeView, bookmarks);
			}
		});

		treeView.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<TreeItem<Item>>() {

			@Override
			public void changed(final ObservableValue<? extends TreeItem<Item>> observable, final TreeItem<Item> oldValue, final TreeItem<Item> newValue) {

				if (newValue != null) {
					if (newValue.getValue() instanceof Project) {
						// Project
						ProjectController projectController = ControllerManager.getProjectController();
						HBox rootRootNode = projectController.getRootNode();
						projectController.setProject((Project) newValue.getValue());
						VBox.setVgrow(rootRootNode, Priority.ALWAYS); // webView fill height
						vBox.getChildren().clear();
						vBox.getChildren().add(rootRootNode);
					} else if (newValue.getValue() instanceof Endpoint) {
						// Endpoint
						EndpointController endpointController = ControllerManager.getEndpointController();
						HBox rootRootNode = endpointController.getRootNode();
						endpointController.setEndpoint((Endpoint) newValue.getValue());
						vBox.getChildren().clear();
						vBox.getChildren().add(rootRootNode);
					}
				}
			}
		});

		// searching for endpoints
		searchItem.getEditor().textProperty().addListener((observable, oldItem, newItem) -> {

			if (!newItem.isEmpty() && treeView.getRoot() != null) {
				treeView.getSelectionModel().clearSelection();
				final List<TreeItem<Item>> search = findChildren(treeView.getRoot(), newItem, false);
				searchCount.setText(String.valueOf(search == null ? 0 : search.size()));
				search.stream().forEach(item -> {
					treeView.getSelectionModel().select(item);
					treeView.scrollTo(treeView.getSelectionModel().getSelectedIndices().get(0));
				});
			} else {
				treeView.getSelectionModel().clearSelection();
			}
		});

		searchItem.setOnMouseClicked(mouseEvent -> {
			if (mouseEvent.getClickCount() == 2) {
			} else {
				searchItem.getItems().clear();
				final List<TreeItem<Item>> endpoints = collectEndpoints();
				endpoints.stream().forEach(e -> searchItem.getItems().add(e.getValue().getName()));
				for (final String bookmark : bookmarks) {
					searchItem.getItems().add(0, bookmark);
				}
				if (bookmarks.size() > 0) {
					searchItem.getItems().add(bookmarks.size(), "____________________________");
				}
			}
		});

		searchItem.addEventFilter(KeyEvent.KEY_RELEASED, new EventHandler<KeyEvent>() {
			@Override
			public void handle(final KeyEvent ke) {

				if (ke.getCode() == KeyCode.ENTER) {
					if (treeView.getRoot() != null) {
						treeView.getSelectionModel().clearSelection();
						final List<TreeItem<Item>> search = findChildren(treeView.getRoot(), searchItem.getValue(), true);

						searchCount.setText(String.valueOf(search == null ? 0 : search.size()));
						search.stream().forEach(item -> {
							treeView.getSelectionModel().select(item);
						});
						treeView.scrollTo(treeView.getSelectionModel().getSelectedIndices().get(0));
					} else {
						treeView.getSelectionModel().clearSelection();
					}
				} else {

				}
			}
		});

		// topTabPane
		topTabPane.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<Tab>() {
			@Override
			public void changed(final ObservableValue<? extends Tab> ov, final Tab oldValue, final Tab newValue) {
				// store previous center node used by borderPane
				centerNodes.put(oldValue.getId(), rootNode.getCenter());

				// set borderPane center node
				rootNode.setCenter(getCenterNode(newValue.getId()));
			}
		});

		centerNodes.put("projectTab", rootNode.getCenter());
		centerNodes.put("editTab", rootNode.getCenter());
		centerNodes.put("styleTab", rootNode.getCenter());
		centerNodes.put("settingsTab", rootNode.getCenter());

		// Base URL table
		baseUrlTable.setItems(application.getBaseUrls());

		baseUrlNameColumn.setCellValueFactory(new PropertyValueFactory<BaseUrl, String>("name"));
		baseUrlNameColumn.setCellFactory(new Callback<TableColumn<BaseUrl, String>, TableCell<BaseUrl, String>>() {
			@Override
			public TableCell<BaseUrl, String> call(final TableColumn<BaseUrl, String> param) {
				return new BaseNameCellFactory();
			}
		});

		baseUrlUrlColumn.setCellValueFactory(new PropertyValueFactory<BaseUrl, String>("url"));
		baseUrlUrlColumn.setCellFactory(new Callback<TableColumn<BaseUrl, String>, TableCell<BaseUrl, String>>() {
			@Override
			public TableCell<BaseUrl, String> call(final TableColumn<BaseUrl, String> param) {
				return new BaseUrlCellFactory();
			}
		});

		baseUrlEnabledColumn.setCellValueFactory(new PropertyValueFactory<BaseUrl, Boolean>("enabled"));
		ToggleGroup group = new ToggleGroup();

		baseUrlEnabledColumn.setCellFactory(new Callback<TableColumn<BaseUrl, Boolean>, TableCell<BaseUrl, Boolean>>() {

			@Override
			public TableCell<BaseUrl, Boolean> call(final TableColumn<BaseUrl, Boolean> param) {
				return new RadioButtonCell(group);
			}
		});

		final ContextMenu contextMenu = new ContextMenu();
		baseUrlTable.setOnKeyPressed(event -> {
			if (event.getCode().equals(KeyCode.DELETE)) {
				BaseUrl baseUrl = baseUrlTable.getSelectionModel().getSelectedItem();
				removeBaseUrl(baseUrl);
			}
		});

		baseUrlTable.setOnMousePressed(mouseEvent -> {
			if (mouseEvent.isSecondaryButtonDown()) {
				final MenuItem duplicate = new MenuItem("Duplicate");
				final MenuItem delete = new MenuItem("Delete");
				BaseUrl baseUrl = baseUrlTable.getSelectionModel().getSelectedItem();

				if (baseUrl == null) {
					duplicate.setDisable(true);
					delete.setDisable(true);
				}
				contextMenu.getItems().clear();
				final MenuItem add = new MenuItem("Add");
				contextMenu.getItems().add(add);
				// add
				add.setOnAction(e -> {
					List<String> baseUrlNames = application.getBaseUrls().stream().map(b -> b.getName()).collect(Collectors.toList());
					application.addBaseUrl(new BaseUrl(Strings.getNextValue(baseUrlNames, "name"), "url", false));
				});
				contextMenu.getItems().addAll(duplicate, new SeparatorMenuItem(), delete);
				// duplicate
				duplicate.setOnAction(e -> {
					application.addBaseUrl(new BaseUrl("copy of " + baseUrl.getName(), baseUrl.getUrl(), baseUrl.getEnabled()));
				});
				// delete
				delete.setOnAction(e -> {
					removeBaseUrl(baseUrl);
					BaseUrl selectedBaseUrl = baseUrlTable.getSelectionModel().getSelectedItem();
					if (selectedBaseUrl == null || (selectedBaseUrl != null && !selectedBaseUrl.getEnabled())) {
						baseUrlProperty.get().enabledProperty().set(false);
						baseUrlProperty.get().urlProperty().set("");
						baseUrlProperty.get().nameProperty().set("");
					}
				});
				baseUrlTable.setContextMenu(contextMenu);
			}
		});

		// style
		style.setItems(App.getStyles());
		App.getStyleUri(application.getStyle()).ifPresent(uri -> {
			setStyle(uri);
			style.getSelectionModel().select(application.getStyle());
		});
		style.valueProperty().addListener((observable, oldValue, newValue) -> {
			App.getStyleUri(newValue).ifPresent(uri -> {
				application.setStyle(newValue);
				setStyle(uri);
			});
		});

		// connection timeout
		connectionTimeout.setText(application.getConnectionTimeout().toString());
		RestClient.setConnectionTimeout(application.getConnectionTimeout());
		connectionTimeout.textProperty().addListener((observable, oldValue, newValue) -> {
			if (newValue.matches("\\d+")) { // new value is an integer
				Integer timeout = Integer.valueOf(newValue);
				connectionTimeout.setText(timeout.toString());
				application.setConnectionTimeout(timeout);
				RestClient.setConnectionTimeout(timeout);
			} else { // new value is not an integer
				if (newValue.isEmpty()) {// empty value : reset to default value
					connectionTimeout.setText(App.DEFAULT_CONNECTION_TIMEOUT.toString());
					application.setConnectionTimeout(App.DEFAULT_CONNECTION_TIMEOUT);
					RestClient.setConnectionTimeout(App.DEFAULT_CONNECTION_TIMEOUT);
				} else {
					connectionTimeout.setText(oldValue);
				}
			}
		});
		// read timeout
		readTimeout.setText(application.getReadTimeout().toString());
//		RestClient.setRequestTimeout(application.getReadTimeout());
		readTimeout.textProperty().addListener((observable, oldValue, newValue) -> {
			if (newValue.matches("\\d+")) { // new value is an integer
				Integer timeout = Integer.valueOf(newValue);
				readTimeout.setText(timeout.toString());
				application.setReadTimeout(timeout);
//				RestClient.setRequestTimeout(timeout);
			} else { // new value is not an integer
				if (newValue.isEmpty()) {// empty value : reset to default value
					readTimeout.setText(App.DEFAULT_READ_TIMEOUT.toString());
					application.setReadTimeout(App.DEFAULT_READ_TIMEOUT);
//					RestClient.setRequestTimeout(App.DEFAULT_READ_TIMEOUT);
				} else {
					readTimeout.setText(oldValue);
				}
			}
		});
	}

	Optional<TreeItem<Item>> getSelectedItem() {

		Optional<TreeItem<Item>> optionalItem = Optional.empty();
		if (treeView.getSelectionModel().getSelectedItem() != null) {
			optionalItem = Optional.of(treeView.getSelectionModel().getSelectedItem());
		}
		return optionalItem;
	}

	public BorderPane getRootNode() {
		return rootNode;
	}

	public File getProjectFile() {
		return projectFile;
	}

	public BottomController getBottomController() {
		return bottomController;
	}

	public static void updateBaseUrlProperty(final BaseUrl baseUrl) {
		baseUrlProperty.get().enabledProperty().set(baseUrl.getEnabled());
		baseUrlProperty.get().nameProperty().set(baseUrl.getName());
		baseUrlProperty.get().urlProperty().set(baseUrl.getUrl());
	}

	private Node getCenterNode(final String tabId) {

		Node center = centerNodes.get(tabId);

		if (center == null) {
			if (tabId.equals("webTab")) {
				getWebEngine().load("https://www.qwant.com/?l=fr");
				center = webView;
			}
		}
		if (tabId.equals("logsTab")) {
			center = getLogsVBox();
		}
		return center;
	}

	private VBox getLogsVBox() {

		LogsController logsController = ControllerManager.getLogsController();
		return logsController.getRootNode();
	}

	private WebView getWebView() {
		if (webView == null) {
			webView = new WebView();
		}
		return webView;
	}

	private WebEngine getWebEngine() {

		if (webEngine == null) {
			webEngine = getWebView().getEngine();
			webEngine.setJavaScriptEnabled(true);
		}
		return webEngine;
	}

	@FXML
	protected void newProject(final ActionEvent event) {

		final ButtonData choice = confirmSaveProject();
		if (choice.equals(ButtonData.YES)) {
			save(null);
		}

		final Project project = new Project("New project");
		final TreeItem<Item> projectItem = new TreeItem<>(project);
		treeView.setRoot(projectItem);

		projectFile = null;
		bottomController.setFileName("");
	}

	@FXML
	protected void open(final ActionEvent event) {

		final ButtonData choice = confirmSaveProject();
		if (choice.equals(ButtonData.YES)) {
			save(null);
		}

		final FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Open a project");

		final File initialDirectory = projectFile == null ? new File(App.getApplicationHome()) : projectFile.getParentFile();
		fileChooser.setInitialDirectory(initialDirectory);

		final File file = fileChooser.showOpenDialog(null);
		if (file != null) {
			loadProject(file.toURI());
		}
	}

	private void loadProject(final URI uri) {

		try {
			final Project project = ProjectService.openProject(uri);
			if (project != null) {
				final TreeItem<Item> projectItem = new TreeItem<>(project);
				builTree(projectItem);
				treeView.setRoot(projectItem);

				sort(projectItem);

				collapseTreeView(projectItem);
				projectItem.setExpanded(true);

				projectFile = new File(uri);
				bottomController.setFileName(projectFile.getAbsolutePath());

				application.setLastProjectUri(uri.toString());

				// load exchanges
				ExchangesService.loadExchanges(uri, project);
			}
		} catch (final NotFoundException e) {
			Logger.info("Project not found " + e.getMessage());
			final Alert alert = new Alert(AlertType.ERROR);
			alert.setTitle("Loading last project");
			alert.setHeaderText("Project not found");
			alert.setContentText(e.getMessage());
			alert.showAndWait();
		} catch (TechnicalException e) {
			Logger.error(e);
			final Alert alert = new Alert(AlertType.ERROR);
			alert.setTitle("Loading last project");
			alert.setHeaderText("Technical error");
			alert.setContentText(e.getMessage());
			alert.showAndWait();
		} catch (Exception e) {
			Logger.error(e);
			final Alert alert = new Alert(AlertType.ERROR);
			alert.setTitle("Loading last project");
			alert.setHeaderText("Technical error");
			alert.setContentText(e.getMessage());
			alert.showAndWait();
		}
	}

	@FXML
	protected void save(final ActionEvent event) {

		if (treeView.getRoot() != null) {
			final Project project = (Project) treeView.getRoot().getValue();

			if (projectFile == null) {
				final FileChooser fileChooser = new FileChooser();
				fileChooser.setTitle("Save the project " + project.getName());

				final File initialDirectory = Strings.isNullOrEmpty(application.getLastProjectUri()) ? new File(App.getApplicationHome()) : new File(URI.create(application.getLastProjectUri())).getParentFile();
				fileChooser.setInitialDirectory(initialDirectory);
				fileChooser.setInitialFileName(project.getName() + ".xml");

				final File file = fileChooser.showSaveDialog(rootNode.getScene().getWindow());

				if (file != null) {
					if (file.equals(new File(App.APPLICATION_FILE))) {
						final Alert alert = new Alert(AlertType.ERROR);
						alert.setTitle("Save the project");
						alert.setHeaderText("The application file cannot be overriden.");
						alert.showAndWait();
						projectFile = null;
					} else {
						projectFile = file.getName().endsWith(".xml") ? file : new File(file.getAbsolutePath() + ".xml");
					}
				}
			}
			if (projectFile != null) {
				try {
					ProjectService.saveProject(project, projectFile.toURI());
				} catch (TechnicalException e) {
					Logger.error(e);
					final Alert alert = new Alert(AlertType.ERROR);
					alert.setTitle("Save the project");
					alert.setHeaderText("An error occured");
					alert.setContentText(e.getMessage());
					alert.showAndWait();
				}
				application.setLastProjectUri(projectFile.toURI().toString());
				bottomController.setFileName(projectFile.getAbsolutePath());

				// save the exchanges
				try {
					ExchangesService.saveExchanges(project, projectFile.toURI());
				} catch (TechnicalException e) {
					Logger.error(e);
					final Alert alert = new Alert(AlertType.ERROR);
					alert.setTitle("Save the exchanges");
					alert.setHeaderText("An error occured");
					alert.setContentText(e.getMessage());
					alert.showAndWait();
				}
			}
		}
	}

	@FXML
	protected void saveAs(final ActionEvent event) {

		if (projectFile != null) {
			final FileChooser fileChooser = new FileChooser();
			fileChooser.setTitle("Save the project as");

			final File initialDirectory = Strings.isNullOrEmpty(application.getLastProjectUri()) ? new File(App.getApplicationHome()) : new File(URI.create(application.getLastProjectUri())).getParentFile();
			fileChooser.setInitialDirectory(initialDirectory);
			fileChooser.setInitialFileName("projectCopy.xml");

			final File file = fileChooser.showSaveDialog(rootNode.getScene().getWindow());
			if (file != null) {
				if (file.equals(new File(App.APPLICATION_FILE))) {
					final Alert alert = new Alert(AlertType.ERROR);
					alert.setTitle("Save the project as");
					alert.setHeaderText("The application file cannot be overriden.");
					alert.showAndWait();
					projectFile = null;
				} else {
					try {
						Files.copy(projectFile.toPath(), file.toPath(), StandardCopyOption.COPY_ATTRIBUTES);
						loadProject(file.toURI());
					} catch (final IOException e) {
						Logger.error(e);
						final Alert alert = new Alert(AlertType.ERROR);
						alert.setTitle("Save as project");
						alert.setHeaderText("Copy error");
						alert.setContentText(e.getMessage());
						alert.showAndWait();
					}
				}
			}
		}
	}

	@FXML
	protected void delete(final ActionEvent event) {

		final Alert alert = new Alert(AlertType.CONFIRMATION);

		alert.setTitle("Delete the project");
		alert.setHeaderText("Confirm your choice");
		final Project project = (Project) treeView.getRoot().getValue();
		alert.setContentText("Are you sure \nto delete the project " + project.getName() + " ?\n\n");
		final ButtonType yesButton = new ButtonType("Yes");
		final ButtonType noButton = new ButtonType("No");

		alert.getButtonTypes().setAll(noButton, yesButton);

		final Optional<ButtonType> result = alert.showAndWait();
		if (result.get() == yesButton) {
			if (projectFile != null) {
				projectFile.delete();
			}
			projectFile = null;
			treeView.setRoot(null);
		}
	}

	@FXML
	public void exit(final ActionEvent event) {

		ApplicationService.saveApplication(application);

		final ButtonData choice = confirmSaveProject();
		if (choice.equals(ButtonData.YES)) {
			save(null);
		}
		if (!choice.equals(ButtonData.CANCEL_CLOSE)) {
			Platform.exit();
		}
	}

	@FXML
	public void launchWebPage(final ActionEvent event) {

		getWebEngine().load(webUrl.getValue());
		rootNode.setCenter(webView);
	}

	@FXML
	protected void collapse(final ActionEvent event) {

		collapseTreeView(treeView.getRoot());
	}

	private void collapseTreeView(final TreeItem<?> item) {
		if (item != null && !item.isLeaf()) {
			item.setExpanded(false);
			for (final TreeItem<?> child : item.getChildren()) {
				collapseTreeView(child);
			}
		}
	}

	private void setStyle(final String uri) {

		rootNode.getStylesheets().clear();
		rootNode.getStylesheets().add(uri);
	}

	private void builTree(final TreeItem<Item> parent) {

		final Item currentItem = parent.getValue();
		for (final Item childItem : currentItem.getChildren()) {
			final TreeItem<Item> childTreeItem = new TreeItem<>(childItem);
			childTreeItem.setExpanded(true);
			parent.getChildren().add(childTreeItem);
			builTree(childTreeItem);
		}
	}

	public List<TreeItem<Item>> collectEndpoints() {

		return treeView.getRoot() == null ? new ArrayList<>()
				: flattened(treeView.getRoot()).filter(ti -> ti.getValue().getClass().equals(Endpoint.class)).sorted((ti1, ti2) -> ti1.getValue().getName().compareTo(ti2.getValue().getName())).collect(Collectors.toList());
	}

	public List<TreeItem<Item>> findChildren(final TreeItem<Item> parent, final String name, final boolean contains) {

		final Predicate<TreeItem<Item>> predicate = contains ? ti -> ti.getValue().getName().toLowerCase().contains(name.toLowerCase()) : ti -> ti.getValue().getName().toLowerCase().equals(name.toLowerCase());
		return parent == null ? null : flattened(parent).filter(predicate).filter(ti -> ti.getValue().getClass().equals(Endpoint.class)).collect(Collectors.toList());
	}

	public Optional<TreeItem<Item>> findChild(final TreeItem<Item> parent, final String name) {

		return flattened(parent).filter(ti -> ti.getValue().getName().toLowerCase().contains(name.toLowerCase())).filter(ti -> ti.getValue().getClass().equals(Endpoint.class)).findFirst();
	}

	private static Stream<TreeItem<Item>> flattened(final TreeItem<Item> parent) {

		return Stream.concat(Stream.of(parent), parent.getChildren().stream().flatMap(a -> MainController.flattened(a)));
	}

	private ButtonData confirmSaveProject() {

		ButtonData buttonData = ButtonData.OTHER;

		if (treeView.getRoot() != null) {
			final Project project = (Project) treeView.getRoot().getValue();
			final Alert alert = new Alert(AlertType.CONFIRMATION);
			alert.setTitle("Save the project");
			alert.setHeaderText("Do you want to save the project\n" + project.getName() + " ?");
			final ButtonType yesButton = new ButtonType("Yes", ButtonData.YES);
			final ButtonType noButton = new ButtonType("No", ButtonData.NO);
			final ButtonType cancelButton = new ButtonType("Cancel", ButtonData.CANCEL_CLOSE);

			alert.getButtonTypes().setAll(yesButton, noButton, cancelButton);
			final Optional<ButtonType> result = alert.showAndWait();
			buttonData = result.get().getButtonData();
		}
		return buttonData;
	}

	private void sort(final TreeItem<Item> parent) {

		parent.getChildren().sort(TreeCellFactory.comparator);
		for (final TreeItem<Item> child : parent.getChildren()) {
			sort(child);
		}
	}

	@FXML
	protected void closeWebView(final ActionEvent event) {

		getWebEngine().load(null);
		webEngine = null;
	}

	private void removeBaseUrl(final BaseUrl baseUrl) {

		final ButtonType response = AlertBuilder.confirm("Delete the base url", "Do you want to delete\n" + baseUrl.getName());
		if (response.equals(ButtonType.OK)) {
			application.removeBaseUrl(baseUrl);
		}
	}

	@SuppressWarnings("resource")
	@FXML
	void importEndpoints(final ActionEvent event) {

/*TODO		ClientResponse response = null;
		try {
			Optional<BaseUrl> optionalBaseUrl = application.getEnabledBaseUrl();
			if (optionalBaseUrl.isPresent()) {
				String gmsWebServiceUri = optionalBaseUrl.get().getUrl() + "/application/webServices/restUI?download=false";
				Exchange exchange = new Exchange("", Instant.now().toEpochMilli());
				exchange.setUri(gmsWebServiceUri);
				response = RestClient.execute("GET", exchange);
				if (response != null) {
					final InputStream inputStream = response.getEntityInputStream();

					Project project = ProjectService.openProject(inputStream);
					final TreeItem<Item> projectItem = new TreeItem<>(project);
					builTree(projectItem);
					sort(projectItem);

					treeView.setRoot(projectItem);

					collapseTreeView(projectItem);
					projectItem.setExpanded(true);

					// load exchanges
					if (projectFile != null) {
						ExchangesService.loadExchanges(projectFile.toURI(), project);
					}
				}
			}
		} catch (ClientException | TechnicalException e) {
			Logger.error(e);
			final Alert alert = new Alert(AlertType.ERROR);
			alert.setTitle("Import endpoints");
			alert.setHeaderText("An error occured");
			alert.setContentText(e.getMessage());
			alert.showAndWait();
		} catch (NotFoundException e) {
			Logger.error(e);
			final Alert alert = new Alert(AlertType.ERROR);
			alert.setTitle("Import endpoints");
			alert.setHeaderText("An error occured");
			alert.setContentText(e.getMessage());
			alert.showAndWait();
		}

 */
	}

	@FXML
	protected void clearLogs(final ActionEvent event) {
		ControllerManager.getLogsController().clearLogs();
	}

	@FXML
	protected void about(final ActionEvent event) {

		final Alert alert = new Alert(AlertType.INFORMATION);
		alert.setTitle("About RestUI");
		alert.setHeaderText("Version : " + Version.getName() + "\nBuild date : " + Version.getDate(ZoneId.systemDefault().getId()));
		alert.setContentText("(C) Olivier MATHE");
		alert.showAndWait();
	}

	@FXML
	protected void setHeader(final ActionEvent event) {

		final Item item = treeView.getRoot().getValue();
		if (item != null && !headerName.getText().isEmpty() && !headerValue.getText().isEmpty()) {
			setItemsAuthorizationHeader(item, headerName.getText(), headerValue.getText());
		}
	}

	private void setItemsAuthorizationHeader(final Item parent, final String name, final String value) {

		for (final Item child : parent.getChildren()) {
			if (child instanceof Endpoint) {
				final Endpoint endpoint = (Endpoint) child;

				endpoint.getExchanges().stream().forEach(exchange -> {
					Optional<Parameter> authorizationParameter = exchange.findParameter(Direction.REQUEST, Location.HEADER, name);
					if (authorizationParameter.isPresent()) {
						authorizationParameter.get().setValue(value);
					}
				});
			} else {
				setItemsAuthorizationHeader(child, name, value);
			}
		}
	}

}
