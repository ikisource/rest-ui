package fr.ikisource.restui.controller;

import fr.ikisource.restui.commons.AlertBuilder;
import fr.ikisource.restui.commons.Strings;
import fr.ikisource.restui.exception.ClientException;
import fr.ikisource.restui.model.Endpoint;
import fr.ikisource.restui.model.Exchange;
import fr.ikisource.restui.model.Exchange.BodyType;
import fr.ikisource.restui.model.Parameter;
import fr.ikisource.restui.model.Parameter.Direction;
import fr.ikisource.restui.model.Parameter.Location;
import fr.ikisource.restui.model.Parameter.Type;
import fr.ikisource.restui.model.Path;
import fr.ikisource.restui.service.Logger;
import fr.ikisource.restui.service.Notifier;
import fr.ikisource.restui.service.RestClient;
import fr.ikisource.restui.service.Tools;
import fr.ikisource.restui.service.tools.JsonHelper;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.TableColumn.CellEditEvent;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.util.converter.DefaultStringConverter;

//import javax.ws.rs.core.Response;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.*;
import java.net.URL;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

public class EndpointController implements Initializable {

	@FXML
	private HBox rootNode;

	// endpoint
	@FXML
	private Label endpointName;
	@FXML
	private ComboBox<String> method;
	@FXML
	private TextField path;

	// request
	@FXML
	private TableView<Parameter> requestParameters;
	@FXML
	private TableColumn<Parameter, Boolean> parameterEnabledColumn;
	@FXML
	private TableColumn<Parameter, String> parameterLocationColumn;
	@FXML
	private TableColumn<Parameter, String> parameterNameColumn;
	@FXML
	private TableColumn<Parameter, String> parameterValueColumn;
	@FXML
	private SplitPane requestResponseSplitPane;

	@FXML
	private TextField uri;

	// response
	@FXML
	private TableView<Parameter> responseParameters;
	@FXML
	private TableColumn<Parameter, String> headerNameColumn;
	@FXML
	private TableColumn<Parameter, String> headerValueColumn;
	@FXML
	private TextArea responseBody;
	@FXML
	private Label responseStatus;
	@FXML
	private Label responseDuration;

	// exchanges
	@FXML
	private TableView<Exchange> exchanges;
	@FXML
	private TableColumn<Exchange, String> exchangeNameColumn;
	@FXML
	private TableColumn<Exchange, Long> exchangeDateColumn;
	@FXML
	private TableColumn<Exchange, Integer> exchangeDurationColumn;
	@FXML
	private TableColumn<Exchange, Integer> exchangeStatusColumn;
	@FXML
	private TableColumn<Exchange, String> exchangeUriColumn;

	@FXML
	private Button execute;

	@FXML
	private RadioButton rawBody;

	@FXML
	private RadioButton formEncodedBody;

	@FXML
	private RadioButton formDataBody;

	@FXML
	private VBox bodyVBox;

	@FXML
	private HBox bodyHBox;

	@FXML
	private Circle statusCircle;

	@FXML
	private HBox endpointSpecificationHBox;

	@FXML
	private RadioButton radioButtonExecutionMode;

	@FXML
	private AnchorPane anchorPaneExecute;

	private final StringProperty baseUrl;
	private Endpoint endpoint;
	private Exchange currentExchange;
	private BodyType specificationBodyType;

	public EndpointController() {
		super();
		baseUrl = new SimpleStringProperty();
	}

	@Override
	public void initialize(final URL location, final ResourceBundle resources) {

		// listen to method control to set endpoint
		method.valueProperty().addListener((observable, oldValue, newValue) -> endpoint.setMethod(newValue));

		// bind base URL property to baseUrlProperty of MainController
		baseUrl.bind(MainController.baseUrlProperty.get().urlProperty());
		// listen to baseUrl control to build the URI
		baseUrl.addListener((observable, oldValue, newValue) -> buildUri());

		path.setTooltip(new Tooltip("Endpoint path value"));

		// request parameters
		requestParameters.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

		requestParameters.setOnKeyPressed(event -> {
			if (event.getCode().equals(KeyCode.DELETE)) {
				deleteParameters(requestParameters.getSelectionModel().getSelectedItems());
			}
		});

		requestParameters.setOnMousePressed(new EventHandler<MouseEvent>() {

			@Override
			public void handle(final MouseEvent event) {

				final Optional<Parameter> parameter = getSelectedRequestParameter();

				if (event.isSecondaryButtonDown()) { // right clic
					final ContextMenu requestParametersContextMenu = new ContextMenu();
					requestParametersContextMenu.getItems().clear();
					final Menu add = new Menu("Add");
					final MenuItem addHeader = new MenuItem(Parameter.Location.HEADER.name());
					final MenuItem addQuery = new MenuItem(Parameter.Location.QUERY.name());
					final MenuItem addPath = new MenuItem(Parameter.Location.PATH.name());
					add.getItems().addAll(addHeader, addQuery, addPath);

					final MenuItem copy = new MenuItem("Copy");
					final MenuItem paste = new MenuItem("Paste");
					final MenuItem delete = new MenuItem("Delete");
					copy.setDisable(!parameter.isPresent());
					paste.setDisable(ObjectClipboard.getInstance().getParameters().isEmpty());
					delete.setDisable(!parameter.isPresent());

					requestParametersContextMenu.getItems().clear();
					if (currentExchange.isFinalized()) {
						requestParametersContextMenu.getItems().addAll(copy);
					} else {
						requestParametersContextMenu.getItems().addAll(add, copy, paste, new SeparatorMenuItem(), delete);
					}
					requestParameters.setContextMenu(requestParametersContextMenu);

					addHeader.setOnAction(e -> {
						List<String> parameterNames = requestParameters.getItems().stream().map(p -> p.getName()).collect(Collectors.toList());
						addParameter(new Parameter(true, Direction.REQUEST, Location.HEADER, Type.TEXT, Strings.getNextValue(parameterNames, "name"), ""));
					});
					addQuery.setOnAction(e -> {
						List<String> parameterNames = requestParameters.getItems().stream().map(p -> p.getName()).collect(Collectors.toList());
						addParameter(new Parameter(true, Direction.REQUEST, Location.QUERY, Type.TEXT, Strings.getNextValue(parameterNames, "name"), ""));
					});
					addPath.setOnAction(e -> {
						List<String> parameterNames = requestParameters.getItems().stream().map(p -> p.getName()).collect(Collectors.toList());
						addParameter(new Parameter(true, Direction.REQUEST, Location.PATH, Type.TEXT, Strings.getNextValue(parameterNames, "name"), ""));
					});
					paste.setOnAction(e -> {
						final List<Parameter> parameters = ObjectClipboard.getInstance().getParameters();
						for (final Parameter p : parameters) {
							addParameter(p);
						}
					});
					if (parameter.isPresent()) {
						delete.setOnAction(e -> {
							deleteParameters(requestParameters.getSelectionModel().getSelectedItems());
						});
						copy.setOnAction(e -> {
							final List<Parameter> selectedParameters = requestParameters.getSelectionModel().getSelectedItems();
							ObjectClipboard.getInstance().setParameters(selectedParameters);
						});
					}
				}
			}
		});

		parameterEnabledColumn.setCellFactory(object -> new CheckBoxTableCell<>());
		parameterEnabledColumn.setCellValueFactory(parameter -> {
			buildUri();
			return parameter.getValue().enabledProperty();
		});
		final ObservableList<String> locations = FXCollections.observableArrayList(Parameter.locations);
		parameterLocationColumn.setCellFactory(ComboBoxTableCell.forTableColumn(new DefaultStringConverter(), locations));
		parameterLocationColumn.setCellValueFactory(parameter -> {
			buildUri();
			return parameter.getValue().locationProperty();
		});
		parameterNameColumn.setCellFactory(TextFieldTableCell.forTableColumn());
		parameterNameColumn.setCellValueFactory(parameter -> {
			buildUri();
			return parameter.getValue().nameProperty();
		});
		parameterValueColumn.setCellFactory(TextFieldTableCell.forTableColumn());
		parameterValueColumn.setCellValueFactory(parameter -> {
			buildUri();
			return parameter.getValue().valueProperty();
		});

		// response parameters
		headerNameColumn.setCellValueFactory(new PropertyValueFactory<Parameter, String>("name"));

		headerValueColumn.setCellValueFactory(new PropertyValueFactory<Parameter, String>("value"));

		headerValueColumn.setCellFactory(TextFieldTableCell.forTableColumn());
		final ContextMenu responseHeadersContextMenu = new ContextMenu();
		final MenuItem responseHeadersContextMenuItemCopy = new MenuItem("Copy to clipboard");
		responseHeadersContextMenu.getItems().add(responseHeadersContextMenuItemCopy);
		responseParameters.setContextMenu(responseHeadersContextMenu);

		responseHeadersContextMenuItemCopy.setOnAction(e -> {
			final Parameter parameter = responseParameters.getSelectionModel().getSelectedItem();
			@SuppressWarnings("unchecked")
			final TablePosition<Parameter, ?> position = responseParameters.getFocusModel().getFocusedCell();

			final TableColumn<Parameter, ?> column = position.getTableColumn();
			final String data = (String) column.getCellObservableValue(parameter).getValue();

			final Clipboard clipboard = Clipboard.getSystemClipboard();
			final ClipboardContent content = new ClipboardContent();
			content.putString(data);
			clipboard.setContent(content);
		});

		// exchanges
		exchangeNameColumn.setCellFactory(TextFieldTableCell.forTableColumn());
		exchangeNameColumn.setCellValueFactory(new PropertyValueFactory<Exchange, String>("name"));

		exchangeNameColumn.setOnEditCommit(new EventHandler<CellEditEvent<Exchange, String>>() {

			@Override
			public void handle(final CellEditEvent<Exchange, String> event) {
				Exchange exchange = event.getTableView().getItems().get(event.getTablePosition().getRow());

				Optional<Exchange> optionalExchange = endpoint.findExchangeByName(event.getNewValue());
				if (optionalExchange.isPresent()) {
					final ButtonType response = AlertBuilder.yesNo("Exchange already exists", "Replace the existing exchange '" + event.getNewValue() + "' ?");
					if (response.getButtonData().equals(ButtonType.YES.getButtonData())) {
						endpoint.removeExchange(optionalExchange.get());
						exchange.setName(event.getNewValue());
					} else {
						exchange.setName(event.getOldValue());
					}
				} else {
					exchange.setName(event.getNewValue());
				}
				displayExecutionMode();
			}
		});

		exchangeDateColumn.setCellFactory(column -> {
			return new TableCell<Exchange, Long>() {
				@Override
				protected void updateItem(final Long item, final boolean empty) {
					super.updateItem(item, empty);

					if (item == null || empty) {
						setText(null);
						setStyle("");
					} else {
						final SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
						// retrieve date from Exchange
						setText(formatter.format(new Date(item)));
					}
				}
			};
		});

		exchangeDateColumn.setCellValueFactory(cellData -> cellData.getValue().dateProperty());
		exchangeDurationColumn.setCellValueFactory(new PropertyValueFactory<Exchange, Integer>("duration"));
		exchangeStatusColumn.setCellValueFactory(new PropertyValueFactory<Exchange, Integer>("status"));
		exchangeUriColumn.setCellValueFactory(new PropertyValueFactory<Exchange, String>("uri"));

		final ContextMenu exchangesContextMenu = new ContextMenu();
		exchanges.setOnKeyPressed(event -> {
			if (event.getCode().equals(KeyCode.DELETE)) {
				final Exchange exchange = exchanges.getSelectionModel().getSelectedItem();
				deleteExchange(exchange);
			}
		});

		exchanges.setOnMousePressed(new EventHandler<MouseEvent>() {
			@Override
			public void handle(final MouseEvent event) {

				final Optional<Exchange> exchange = getSelectedExchange();
				if (event.isSecondaryButtonDown()) {
					exchangesContextMenu.getItems().clear();

					if (exchange.isPresent()) {
						final MenuItem delete = new MenuItem("Delete");
						exchangesContextMenu.getItems().addAll(delete);

						// delete exchange
						delete.setOnAction(e -> {
							deleteExchange(exchange.get());
						});
					}
					exchanges.setContextMenu(exchangesContextMenu);
				}
			}
		});

		exchanges.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
			Optional<Exchange> selectedExchange = getSelectedExchange();
			if (selectedExchange.isPresent()) {
				currentExchange = selectedExchange.get();
				display();
			}
		});

		execute.textProperty().bind(method.valueProperty());

		// status tooltip
		responseStatus.setOnMouseEntered(event -> {
			Tooltip tooltip = null;
			if (currentExchange != null) {
				if (currentExchange.getStatus().equals(0)) {
					if (currentExchange.getDuration().equals(0)) {
						tooltip = new Tooltip("Never executed");
					} else {
						tooltip = new Tooltip("Timeout");
					}
				} else {
					tooltip = new Tooltip(String.valueOf(currentExchange.getStatus()));
				}
			}
			responseStatus.setTooltip(tooltip);
		});
	}

	public HBox getRootNode() {
		return rootNode;
	}

	public void setEndpoint(final Endpoint endpoint) {
		this.endpoint = endpoint;

		endpoint.buildPath();

		// method
		method.setValue(endpoint.getMethod());

		// endpoint name
		endpointName.setText(endpoint.getName());
		endpointName.setTooltip(new Tooltip(endpoint.getDescription()));

		// path
		path.setText(endpoint.getPath());

		// set execution mode
		modeExecution(null);
	}

	public HBox getBodyHBox() {
		return bodyHBox;
	}

	public VBox getBodyVBox() {
		return bodyVBox;
	}

	public boolean isExecutionMode() {
		return radioButtonExecutionMode.isSelected();
	}

	public boolean isSpecificationMode() {
		return !radioButtonExecutionMode.isSelected();
	}

	public Endpoint getEndpoint() {
		return endpoint;
	}

	public Exchange getCurrentExchange() {
		return currentExchange;
	}

	private void deleteExchange(final Exchange exchange) {

		String exchangeName = exchange.getName().isEmpty() ? "the current exchange" : "the exchange '" + exchange.getName() + "'";
		final ButtonType response = AlertBuilder.confirm("Delete the exchange", "Do you want to delete " + exchangeName);
		if (response.equals(ButtonType.OK)) {
			endpoint.removeExchange(exchange);
			if (!endpoint.hasExchanges()) {
				currentExchange = null;
			}
			display();
		}
	}

	@FXML
	protected void execute(final ActionEvent event) {

		Notifier.clear();

		if (endpoint.hasExchanges()) {
			Optional<Exchange> selectedExchange = getSelectedExchange();
			if (selectedExchange.isPresent()) {
				if (selectedExchange.get().isWorking()) {
					// if the selected exchange is the working exchange it becomes the currentExchange
					currentExchange = selectedExchange.get();
				} else {
					// if the selected exchange is not the working exchange, we update the working exchange
					currentExchange = endpoint.updateWorkingExchange(selectedExchange.get());
				}
			}
		} else {
			// no exchanges, we build the working exchange
			currentExchange = endpoint.buildWorkingExchange();
		}

		// clear response parameter
		currentExchange.clearResponseParameters();

		currentExchange.setUri(uri.getText());
		currentExchange.setStatus(0);
		final long t0 = System.currentTimeMillis();

		HttpResponse<byte[]> response;
		Integer duration = 0;
		try {
			response = RestClient.execute(method.getValue(), currentExchange);
			duration = (int) (System.currentTimeMillis() - t0);
			if (response != null) {
				currentExchange.setStatus(response.statusCode());
				// build response headers
				response.headers().map().entrySet().stream().forEach(e -> {
					for (final String value : e.getValue()) {
						final Parameter header = new Parameter(Boolean.TRUE, Direction.RESPONSE, Location.HEADER, Type.TEXT, e.getKey(), value);
						currentExchange.addParameter(header);
					}
				});
				buildResponseBody(response);
			}
		} catch (ClientException e) {
			duration = (int) (System.currentTimeMillis() - t0);
			Logger.error(e);
		} finally {
			currentExchange.setDuration(duration);
			currentExchange.setDate(Instant.now().toEpochMilli());
		}

		// select the working exchange if it exists
		exchanges.getSelectionModel().select(currentExchange);

		display();
	}

	@FXML
	protected void rawBodySelected(final MouseEvent event) {
		if (isSpecificationMode()) {
			specificationBodyType = Exchange.BodyType.RAW;
		} else {
			currentExchange.setRequestBodyType(Exchange.BodyType.RAW);
		}
		requestBody(BodyType.RAW);
	}

	@FXML
	protected void formEncodedBodySelected(final MouseEvent event) {
		if (isSpecificationMode()) {
			specificationBodyType = Exchange.BodyType.X_WWW_FORM_URL_ENCODED;
		} else {
			currentExchange.setRequestBodyType(Exchange.BodyType.X_WWW_FORM_URL_ENCODED);
		}
		requestBody(BodyType.X_WWW_FORM_URL_ENCODED);
	}

	@FXML
	protected void formDataBodySelected(final MouseEvent event) {
		if (isSpecificationMode()) {
			specificationBodyType = Exchange.BodyType.FORM_DATA;
		} else {
			currentExchange.setRequestBodyType(Exchange.BodyType.FORM_DATA);
		}
		requestBody(BodyType.FORM_DATA);
	}

	private void buildResponseBody(final HttpResponse<byte[]> response) {
		responseBody.setText("");
		if (response != null && response.statusCode() != 204) {
			byte[] bytes = response.body();
			if (bytes != null && bytes.length > 0) {
				final String output = new String(bytes, StandardCharsets.UTF_8);

				if (output != null && !output.isEmpty()) {

					Optional<Parameter> contentDisposition = currentExchange.findParameter(Direction.RESPONSE, Location.HEADER, "Content-Disposition");

					if (contentDisposition.isPresent() && contentDisposition.get().getValue().toLowerCase().contains("attachment")) {
						// the response contains the Content-Disposition header and attachment key word

						final FileChooser fileChooser = new FileChooser();
						fileChooser.setTitle("Save the file");

						final File initialDirectory = new File(System.getProperty("user.home"));
						fileChooser.setInitialDirectory(initialDirectory);

						String fileName = Tools.findFileName(contentDisposition.get().getValue());
						fileChooser.setInitialFileName(fileName);
						final File file = fileChooser.showSaveDialog(null);
						Tools.writeBytesToFile(file, bytes);
					} else {
						// response body
						final Parameter responseBody = new Parameter(Boolean.TRUE, Direction.RESPONSE, Location.BODY, Type.TEXT, null, output);
						currentExchange.addParameter(responseBody);
					}
				}
			}
		}
	}

	private void displayStatusCircle(final Exchange exchange) {

		if (exchange == null) {
			statusCircle.setFill(Color.GRAY);
		} else {
			if (exchange.getStatus().toString().startsWith("0") || exchange.getStatus().toString().isEmpty()) {
				if (exchange.getDuration() == 0) {
					statusCircle.setFill(Color.GRAY);
				} else {
					statusCircle.setFill(Color.MAGENTA);
				}
			} else if (exchange.getStatus().toString().startsWith("2")) {
				statusCircle.setFill(Color.LIGHTGREEN);
			} else if (exchange.getStatus().toString().startsWith("3")) {
				statusCircle.setFill(Color.BLUE);
			} else if (exchange.getStatus().toString().startsWith("4")) {
				statusCircle.setFill(Color.ORANGE);
			} else if (exchange.getStatus().toString().startsWith("5")) {
				statusCircle.setFill(Color.RED);
			}
		}
	}

	private void requestBody(BodyType bodyType) {

		RequestBodyController requestBodyController = ControllerManager.getRequestBodyController();
		if (isSpecificationMode()) {
			bodyType = specificationBodyType;
		}
		requestBodyController.display(this, requestBodyController.getRootNode(), bodyType);
	}

	private Optional<Exchange> getSelectedExchange() {

		Optional<Exchange> optional = Optional.empty();

		if (exchanges != null && exchanges.getSelectionModel().getSelectedItem() != null) {
			optional = Optional.of(exchanges.getSelectionModel().getSelectedItem());
		}
		return optional;
	}

	private Optional<Parameter> getSelectedRequestParameter() {

		Optional<Parameter> optional = Optional.empty();
		if (requestParameters != null && requestParameters.getSelectionModel().getSelectedItem() != null) {
			optional = Optional.of(requestParameters.getSelectionModel().getSelectedItem());
		}
		return optional;
	}

	List<String> getEndpointExchangeNames() {
		return endpoint.getExchanges().stream().map(e -> e.getName()).collect(Collectors.toList());

	}

	public void addParameter(final Parameter parameter) {

		if (isSpecificationMode()) {
			// add the parameter to the endpoint
			endpoint.addParameter(parameter);
		} else {
			if (currentExchange == null) {
				currentExchange = endpoint.buildWorkingExchange();
			}
			// add the parameter to the current exchange
			currentExchange.addParameter(parameter.duplicateValue());
		}
		display();
	}

	public void deleteParameters(final List<Parameter> parameters) {

		if (parameters != null && !parameters.isEmpty()) {
			final String message = parameters.size() == 1 ? "Do you want to delete the parameter " + parameters.get(0).getName() + " ?" : "Do you want to delete the " + parameters.size() + " selected parameters ?";
			final ButtonType response = AlertBuilder.confirm("Delete request parameters", message);

			if (response.equals(ButtonType.OK)) {
				if (isSpecificationMode()) {
					// remove the parameters from the endpoint
					endpoint.removeParameters(parameters);
				}
				// remove the parameters from the exchange
				if (currentExchange != null) {
					currentExchange.removeParameters(parameters);
				}
				display();
			}
		}
	}

	@FXML
	protected void modeSpecification(final ActionEvent event) {

		// enable specification
		endpointSpecificationHBox.setDisable(false);
		// disable execute
		anchorPaneExecute.setDisable(true);

		display();
	}

	@FXML
	protected void modeExecution(final ActionEvent event) {

		// create the current exchange if it does not exist
		if (!endpoint.findWorkingExchange().isPresent()) {
			currentExchange = endpoint.buildWorkingExchange();
		}

		radioButtonExecutionMode.setSelected(true);

		// enable execute
		anchorPaneExecute.setDisable(false);
		// disable specification
		endpointSpecificationHBox.setDisable(true);

		// exchanges
		exchanges.setItems((ObservableList<Exchange>) endpoint.getExchanges());

		currentExchange = getWorkingExchangeOrSelectFirstExchange();
		if (currentExchange == null) {
			currentExchange = endpoint.buildWorkingExchange();
		}
		// select the current exchange
		exchanges.getSelectionModel().select(currentExchange);

		display();
	}

	private void display() {

		if (radioButtonExecutionMode.isSelected()) {
			displayExecutionMode();
		} else {
			displaySpecificationMode();
		}
	}

	private void displaySpecificationMode() {

		// request parameters
		requestParameters.setItems(FXCollections.observableArrayList(endpoint.getParameters())
				.filtered(p -> p.isRequestParameter() && (p.isPathParameter() || p.isQueryParameter() || p.isHeaderParameter())));
		requestParameters.refresh();

		// response parameters
		responseParameters.setItems(null);

		// request body
		displayRequestBody();

		// response body
		responseBody.clear();

		responseStatus.setText("0");
		responseDuration.setText("0");

		buildUri();
	}

	private void displayExecutionMode() {

		if (currentExchange == null) {
			responseDuration.setText("0");
			responseStatus.setText("0");
			displayStatusCircle(null);
		} else {

			// request parameters

			// add endpoint parameters if they are not in the working exchange parameter
			if (currentExchange.isWorking()) {
				endpoint.getParameters().stream()
						.filter(p -> p.isRequestParameter())
						.map(p -> p.duplicate())
						.forEach(p -> currentExchange.addParameter(p));
			}

			requestParameters.setItems(FXCollections.observableArrayList(currentExchange.getParameters())
					.filtered(p -> p.isRequestParameter() && (p.isPathParameter() || p.isQueryParameter() || p.isHeaderParameter())));
			requestParameters.refresh();

			// response parameters
			responseParameters.setItems(FXCollections.observableArrayList(currentExchange.getParameters())
					.filtered(p -> p.isResponseParameter() && p.isHeaderParameter()));
			responseParameters.refresh();

			// request body
			displayRequestBody();
			// request parameters are not editable for a finalized exchange
			requestParameters.setEditable(!currentExchange.isFinalized());

			buildUri();

			// response body
			displayResponseBody();

			// response status
			responseStatus.setText(currentExchange.getStatus().toString());

			// response duration
			responseDuration.setText(currentExchange.getDuration().toString());

			// status circle
			displayStatusCircle(currentExchange);

			// workaround to refresh the table
			exchanges.getColumns().get(0).setVisible(false);
			exchanges.getColumns().get(0).setVisible(true);
		}
	}

	private void displayRequestBody() {

		if (isSpecificationMode()) {
			if (specificationBodyType == null) {
				specificationBodyType = Exchange.BodyType.RAW;
			}
			if (specificationBodyType.equals(Exchange.BodyType.RAW)) {
				rawBody.setSelected(true);
				rawBodySelected(null);
			} else if (specificationBodyType.equals(Exchange.BodyType.X_WWW_FORM_URL_ENCODED)) {
				formEncodedBody.setSelected(true);
				formEncodedBodySelected(null);
			} else if (specificationBodyType.equals(Exchange.BodyType.FORM_DATA)) {
				formDataBody.setSelected(true);
				formDataBodySelected(null);
			}
		} else {
			if (currentExchange.getRequestBodyType().equals(Exchange.BodyType.RAW)) {
				rawBody.setSelected(true);
				rawBodySelected(null);
			} else if (currentExchange.getRequestBodyType().equals(Exchange.BodyType.X_WWW_FORM_URL_ENCODED)) {
				formEncodedBody.setSelected(true);
				formEncodedBodySelected(null);
			} else if (currentExchange.getRequestBodyType().equals(Exchange.BodyType.FORM_DATA)) {
				formDataBody.setSelected(true);
				formDataBodySelected(null);
			}
		}
	}

	private void displayResponseBody() {

		responseBody.clear();

		currentExchange.findParameter(Direction.RESPONSE, Location.HEADER, "Content-Type").ifPresent(p -> {

			String body = currentExchange.getResponseBody();
			if (p.getValue().toLowerCase().contains("json")) {
				try {
					body = currentExchange.getResponseBody();
					if (body != null && !body.isEmpty()) {
						responseBody.setText(JsonHelper.pretty(body));
					}
				} catch (final IOException e) {
					Logger.error(e);
					Notifier.notifyError(e.getMessage());
				}
			} else if (p.getValue().toLowerCase().contains("xml")) {
				try (StringWriter stringWriter = new StringWriter()) {
					final Source xmlInput = new StreamSource(new StringReader(body));
					final StreamResult xmlOutput = new StreamResult(stringWriter);
					final TransformerFactory transformerFactory = TransformerFactory.newInstance();
					final Transformer transformer = transformerFactory.newTransformer();
					transformer.setOutputProperty(OutputKeys.INDENT, "yes");
					// transformer.setOutputProperty(OutputPropertiesFactory.S_KEY_INDENT_AMOUNT, "6");
					transformer.transform(xmlInput, xmlOutput);
					responseBody.setText(xmlOutput.getWriter().toString());
				} catch (final Exception e) {
					Logger.error(e);
					Notifier.notifyError(e.getMessage());
				}
			} else if (p.getValue().toLowerCase().contains("html")) {
				responseBody.setText(body);
			} else {
				responseBody.setText(body);
			}
		});
	}

	private void buildUri() {

		boolean validUri = true;

		String valuedUri = baseUrl.get() + path.getText();

		Set<String> queryParams = new HashSet<String>();
		if (!valuedUri.toLowerCase().startsWith("http")) {
			validUri = false;
		}

		for (Parameter parameter : requestParameters.getItems()) {
			if (parameter.isRequestParameter() && parameter.isPathParameter()) {
				if (!parameter.getEnabled() || !parameter.isValid()) {
					validUri = false;
					continue;
				} else {
					valuedUri = valuedUri.replace(Path.ID_PREFIX + parameter.getName() + Path.ID_SUFFIX, parameter.getValue());
				}
			} else if (parameter.isQueryParameter()) {
				if (parameter.getEnabled() && !parameter.isValid()) {
					validUri = false;
				} else if (parameter.getEnabled()) {
					queryParams.add(parameter.getName() + "=" + parameter.getValue());
				}
			}
		}
		if (!queryParams.isEmpty()) {
			valuedUri += "?" + String.join("&", queryParams);
		}
		uri.setText(valuedUri);

		execute.setDisable(!validUri);
	}

	private Exchange getWorkingExchangeOrSelectFirstExchange() {

		Exchange exchange = null;

		Optional<Exchange> optionalWorkingExchange = endpoint.findWorkingExchange();
		if (optionalWorkingExchange.isPresent()) {
			// working exchange exists
			exchange = optionalWorkingExchange.get();
		} else {
			Optional<Exchange> selectedExchange = getSelectedExchange();
			if (!selectedExchange.isPresent()) {
				exchanges.getSelectionModel().select(0); // select first exchange
				exchange = exchanges.getSelectionModel().getSelectedItem();
			}
		}
		return exchange;
	}

}
