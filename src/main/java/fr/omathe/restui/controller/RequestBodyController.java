package fr.omathe.restui.controller;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import fr.omathe.restui.commons.Strings;
import fr.omathe.restui.controller.cellFactory.BodyParameterValueCellFactory;
import fr.omathe.restui.model.Endpoint;
import fr.omathe.restui.model.Exchange;
import fr.omathe.restui.model.Exchange.BodyType;
import fr.omathe.restui.model.Parameter;
import fr.omathe.restui.model.Parameter.Direction;
import fr.omathe.restui.model.Parameter.Location;
import fr.omathe.restui.model.Parameter.Type;
import fr.omathe.restui.service.Logger;
import fr.omathe.restui.service.Notifier;
import fr.omathe.restui.service.tools.JsonHelper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.ComboBoxTableCell;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.util.Callback;
import javafx.util.converter.DefaultStringConverter;

public class RequestBodyController implements Initializable {

	private EndpointController endPointController;
	private Exchange exchange;
	private Endpoint endpoint;
	private ContextMenu contextMenu;
	private MenuItem add;
	private MenuItem remove;

	@FXML
	private VBox rootNode;

	@FXML
	private TableView<Parameter> bodyTableView;

	@FXML
	private TableColumn<Parameter, Boolean> bodyEnabledColumn;

	@FXML
	private TableColumn<Parameter, String> bodyTypeColumn;

	@FXML
	private TableColumn<Parameter, String> bodyNameColumn;

	@FXML
	private TableColumn<Parameter, String> bodyValueColumn;

	@FXML
	private TextArea requestBody;

	public RequestBodyController() {
		super();
	}

	@Override
	public void initialize(final URL location, final ResourceBundle resources) {

		contextMenu = new ContextMenu();
		add = new MenuItem("Add");
		remove = new MenuItem("Remove");

		bodyTableView.setContextMenu(contextMenu);
		bodyTableView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

		bodyEnabledColumn.setCellFactory(object -> new CheckBoxTableCell<>());
		bodyEnabledColumn.setCellValueFactory(parameter -> parameter.getValue().enabledProperty());

		final ObservableList<String> types = FXCollections.observableArrayList(Parameter.types);
		bodyTypeColumn.setCellFactory(ComboBoxTableCell.forTableColumn(new DefaultStringConverter(), types));
		bodyTypeColumn.setCellValueFactory(parameter -> parameter.getValue().typeProperty());

		bodyNameColumn.setCellFactory(TextFieldTableCell.forTableColumn());
		bodyNameColumn.setCellValueFactory(parameter -> parameter.getValue().nameProperty());

		bodyValueColumn.setCellFactory(new Callback<TableColumn<Parameter, String>, TableCell<Parameter, String>>() {

			@Override
			public TableCell<Parameter, String> call(final TableColumn<Parameter, String> param) {
				return new BodyParameterValueCellFactory();
			}
		});

		bodyValueColumn.setCellValueFactory(parameter -> parameter.getValue().valueProperty());

		add.setOnAction(e -> {
			if (exchange != null) {
				List<String> parameterNames = bodyTableView.getItems().stream().map(p -> p.getName()).collect(Collectors.toList());
				final Parameter parameter = new Parameter(Boolean.TRUE, Direction.REQUEST, Location.BODY, Type.TEXT, Strings.getNextValue(parameterNames, "name"), "");
				endPointController.addParameter(parameter);
			}
		});

		remove.setOnAction(e -> {
			if (exchange != null) {
				deleteRequestParameters(bodyTableView.getSelectionModel().getSelectedItems());
			}
		});

		bodyTableView.setOnKeyPressed(event -> {
			if (event.getCode().equals(KeyCode.DELETE)) {
				deleteRequestParameters(bodyTableView.getSelectionModel().getSelectedItems());
			}
		});

		requestBody.textProperty().addListener((observable, oldValue, newValue) -> {
			Notifier.clear();
			if (endPointController.isSpecificationMode()) {
				endpoint.setRequestRawBody(newValue);
			} else {
				exchange.setRequestRawBody(newValue);
				// if content-type is json we format it
				if (exchange.findParameter(p -> p.isHeaderParameter()
						&& p.isRequestParameter()
						&& p.getName().equalsIgnoreCase("Content-Type")
						&& p.getValue().toLowerCase().contains("json")
						&& p.getEnabled()).isPresent()) {
					try {
						requestBody.setText(JsonHelper.pretty(newValue));
					} catch (IOException e) {
						Logger.error(e);
						Notifier.notifyError(e.getMessage());
					}
				}
			}
		});
	}

	public VBox getRootNode() {
		return rootNode;
	}

	public void display(final EndpointController endPointController, final Node node, final BodyType type) {

		this.endPointController = endPointController;

		ObservableList<Parameter> parameterData = null;
		endpoint = endPointController.getEndpoint();

		if (endPointController.isSpecificationMode()) {
			parameterData = FXCollections.observableArrayList(endpoint.getParameters()).filtered(p -> p.isRequestParameter());
			requestBody.setEditable(true);
			bodyTableView.setEditable(true);
		}
		if (endPointController.isExecutionMode()) {
			exchange = endPointController.getCurrentExchange();
			parameterData = FXCollections.observableArrayList(exchange.getParameters()).filtered(p -> p.isRequestParameter());

			// a finalized exchange is not editable
			requestBody.setEditable(!exchange.isFinalized());
			bodyTableView.setEditable(!exchange.isFinalized());

			contextMenu.getItems().clear();
			if (!exchange.isFinalized()) {
				contextMenu.getItems().addAll(add, remove);
			}
		}

		if (type.equals(BodyType.RAW)) {
			// RAW
			String body = endPointController.isSpecificationMode() ? endpoint.getRequestRawBody() : exchange.getRequestRawBody();
			requestBody.setText(body);
			if (body != null) {
				requestBody.setText(body);
			}
			endPointController.getBodyVBox().getChildren().clear();
			endPointController.getBodyVBox().getChildren().add(endPointController.getBodyHBox());
			if (!endPointController.getBodyVBox().getChildren().contains(node)) {
				endPointController.getBodyVBox().getChildren().add(node);
			}
			rootNode.getChildren().clear();
			rootNode.getChildren().addAll(requestBody);
			VBox.setVgrow(rootNode, Priority.ALWAYS);

		} else if (type.equals(BodyType.FORM_DATA)) {
			// FORM_DATA
			bodyTableView.setItems(parameterData.filtered(p -> p.isBodyParameter()));

			endPointController.getBodyVBox().getChildren().clear();
			endPointController.getBodyVBox().getChildren().add(endPointController.getBodyHBox());
			if (!endPointController.getBodyVBox().getChildren().contains(node)) {
				endPointController.getBodyVBox().getChildren().add(node);
			}
			bodyTypeColumn.setVisible(type.equals(BodyType.FORM_DATA));
			rootNode.getChildren().clear();
			rootNode.getChildren().add(bodyTableView);
			VBox.setVgrow(rootNode, Priority.ALWAYS);
			bodyTableView.refresh();
		} else if (type.equals(BodyType.X_WWW_FORM_URL_ENCODED)) {
			// X_WWW_FORM_URL_ENCODED
			bodyTableView.setItems(parameterData.filtered(p -> p.isBodyParameter() && p.isTypeText()));

			endPointController.getBodyVBox().getChildren().clear();
			endPointController.getBodyVBox().getChildren().add(endPointController.getBodyHBox());
			if (!endPointController.getBodyVBox().getChildren().contains(node)) {
				endPointController.getBodyVBox().getChildren().add(node);
			}
			bodyTypeColumn.setVisible(type.equals(BodyType.FORM_DATA));
			rootNode.getChildren().clear();
			rootNode.getChildren().add(bodyTableView);
			VBox.setVgrow(rootNode, Priority.ALWAYS);
			bodyTableView.refresh();
		}
	}

	private void deleteRequestParameters(final List<Parameter> parameters) {

		endPointController.deleteParameters(parameters);
	}

}
