package fr.omathe.restui.controller;

import java.net.URL;
import java.util.Optional;
import java.util.ResourceBundle;

import fr.omathe.restui.model.Endpoint;
import fr.omathe.restui.model.Item;
import fr.omathe.restui.model.Project;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.TreeItem;
import javafx.scene.layout.HBox;

public class ProjectController implements Initializable {

	@FXML
	private HBox rootNode;

	@FXML
	private Label nbEndpoints;

	public void setProject(final Project project) {

		MainController mainController = ControllerManager.getMainController();
		Optional<TreeItem<Item>> optionalItem = mainController.getSelectedItem();
		if (optionalItem.isPresent()) {
			final Long endpointsCount = project.getAllChildren().filter(item -> item instanceof Endpoint).count();
			nbEndpoints.setText(endpointsCount.toString());
		}
	}

	@Override
	public void initialize(final URL location, final ResourceBundle resources) {

	}

	public HBox getRootNode() {
		return rootNode;
	}

}
