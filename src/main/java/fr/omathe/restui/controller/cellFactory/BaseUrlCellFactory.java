package fr.omathe.restui.controller.cellFactory;

import fr.omathe.restui.controller.MainController;
import fr.omathe.restui.model.BaseUrl;
import javafx.scene.Node;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableRow;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;

public class BaseUrlCellFactory extends TableCell<BaseUrl, String> {

	private TextField textField;

	public BaseUrlCellFactory() {
		super();
	}

	@Override
	public void startEdit() {
		super.startEdit();

		if (textField == null) {
			createTextField();
		}

		setGraphic(textField);
		setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
		textField.selectAll();
	}

	@Override
	public void cancelEdit() {
		super.cancelEdit();

		setText(String.valueOf(getItem()));
		setContentDisplay(ContentDisplay.TEXT_ONLY);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void updateItem(final String item, final boolean empty) {
		super.updateItem(item, empty);

		if (empty) {
			setText(null);
			setGraphic(textField);
		} else {
			if (isEditing()) {
				if (textField != null) {
					textField.setText(getString());
				}
				setGraphic(textField);
				setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
			} else {
				setText(getString());
				setContentDisplay(ContentDisplay.TEXT_ONLY);

				// Update the baseUrl used in MainController
				Node parent = getParent();
				if (parent != null) {
					TableRow<BaseUrl> tableRow = (TableRow<BaseUrl>) parent;
					BaseUrl baseUrl = tableRow.getItem();
					if (baseUrl != null) {
						baseUrl.setUrl(getString());
						if (baseUrl.getEnabled()) {
							MainController.updateBaseUrlProperty(baseUrl);
						}
					}
				}
			}
		}
	}

	private void createTextField() {
		textField = new TextField(getString());
		textField.setMinWidth(this.getWidth() - this.getGraphicTextGap() * 2);
		textField.setOnKeyPressed(t -> {
			if (t.getCode() == KeyCode.ENTER) {
				commitEdit(textField.getText());
			} else if (t.getCode() == KeyCode.ESCAPE) {
				cancelEdit();
			}
		});
	}

	private String getString() {
		return getItem() == null ? "" : getItem();
	}
}
