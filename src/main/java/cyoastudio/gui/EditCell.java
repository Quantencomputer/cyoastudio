package cyoastudio.gui;

import javafx.event.Event;
import javafx.scene.control.*;
import javafx.scene.control.ListView.EditEvent;
import javafx.scene.input.KeyEvent;
import javafx.util.StringConverter;

public class EditCell<T> extends ListCell<T> {

	// Text field for editing
	private final TextField textField = new TextField();

	// Converter for converting the text in the text field to the user type, and
	// vice-versa:
	private final StringConverter<T> converter;

	public EditCell(StringConverter<T> converter) {
		this.converter = converter;

		itemProperty().addListener((obx, oldItem, newItem) -> {
			if (newItem == null) {
				setText(null);
			} else {
				setText(converter.toString(newItem));
			}
		});
		setGraphic(textField);
		setContentDisplay(ContentDisplay.TEXT_ONLY);
		textField.setMaxWidth(180);
		textField.setStyle("-fx-padding: 0.166667em 0.333333em");

		textField.setOnAction(evt -> {
			commitEdit(this.converter.fromString(textField.getText()));
		});
		textField.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
			if (!isNowFocused) {
				commitEdit(this.converter.fromString(textField.getText()));
			}
		});
		textField.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
			// if (event.getCode() == KeyCode.ESCAPE) {
			// textField.setText(converter.toString(getItem()));
			// cancelEdit();
			// event.consume();
			// } else if (event.getCode() == KeyCode.RIGHT) {
			// getTableView().getSelectionModel().selectRightCell();
			// event.consume();
			// } else if (event.getCode() == KeyCode.LEFT) {
			// getTableView().getSelectionModel().selectLeftCell();
			// event.consume();
			// } else if (event.getCode() == KeyCode.UP) {
			// getTableView().getSelectionModel().selectAboveCell();
			// event.consume();
			// } else if (event.getCode() == KeyCode.DOWN) {
			// getTableView().getSelectionModel().selectBelowCell();
			// event.consume();
			// }
		});
	}

	/**
	 * Convenience converter that does nothing (converts Strings to themselves and
	 * vice-versa...).
	 */
	public static final StringConverter<String> IDENTITY_CONVERTER = new StringConverter<String>() {

		@Override
		public String toString(String object) {
			return object;
		}

		@Override
		public String fromString(String string) {
			return string;
		}

	};

	/**
	 * Convenience method for creating an EditCell for a String value.
	 * 
	 * @return
	 */
	public static EditCell<String> createStringEditCell() {
		return new EditCell<String>(IDENTITY_CONVERTER);
	}

	// set the text of the text field and display the graphic
	@Override
	public void startEdit() {
		super.startEdit();
		textField.setText(converter.toString(getItem()));
		setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
		textField.requestFocus();
	}

	// revert to text display
	@Override
	public void cancelEdit() {
		super.cancelEdit();
		setContentDisplay(ContentDisplay.TEXT_ONLY);
	}

	// commits the edit. Update property if possible and revert to text display
	@Override
	public void commitEdit(T item) {

		// This block is necessary to support commit on losing focus, because the
		// baked-in mechanism
		// sets our editing state to false before we can intercept the loss of focus.
		// The default commitEdit(...) method simply bails if we are not editing...
		if (!isEditing() && !item.equals(getItem())) {
			ListView<T> list = getListView();
			if (list != null) {
				EditEvent<T> event = new EditEvent<>(list,
						ListView.editCommitEvent(), item, getIndex());
				Event.fireEvent(list, event);
			}
		}

		super.commitEdit(item);

		setContentDisplay(ContentDisplay.TEXT_ONLY);
	}

}