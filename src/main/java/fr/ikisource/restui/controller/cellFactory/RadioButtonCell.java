package fr.ikisource.restui.controller.cellFactory;

import fr.ikisource.restui.controller.MainController;
import fr.ikisource.restui.model.BaseUrl;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TableCell;
import javafx.scene.control.ToggleGroup;

public class RadioButtonCell extends TableCell<BaseUrl, Boolean> {

	private final RadioButton radioButton;

	public RadioButtonCell(final ToggleGroup group) {
		radioButton = new RadioButton();

		radioButton.selectedProperty().addListener(new ChangeListener<Boolean>() {

			@Override
			public void changed(final ObservableValue<? extends Boolean> arg0, final Boolean oldValue, final Boolean newValue) {

				final BaseUrl baseUrl = (BaseUrl) getTableRow().getItem();

				if (baseUrl != null) {

					baseUrl.setEnabled(newValue);
					if (newValue) {
						// Update the baseUrl used in MainController
						MainController.updateBaseUrlProperty(baseUrl);
					} else {
						MainController.baseUrlProperty.get().urlProperty().set("");
						MainController.baseUrlProperty.get().nameProperty().set("");
					}
				}
			}
		});
		radioButton.setToggleGroup(group);
	}

	@Override
	public void updateItem(final Boolean item, final boolean empty) {
		super.updateItem(item, empty);

		if (empty) {
			setText(null);
			setGraphic(null);
		} else {
			radioButton.setSelected(item);
			setGraphic(radioButton);
		}
	}

}
