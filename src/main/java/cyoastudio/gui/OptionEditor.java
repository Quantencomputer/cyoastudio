package cyoastudio.gui;

import java.io.IOException;

import cyoastudio.data.*;
import javafx.beans.value.*;
import javafx.event.*;
import javafx.fxml.*;
import javafx.scene.control.*;
import javafx.scene.control.SpinnerValueFactory.IntegerSpinnerValueFactory;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.util.StringConverter;

public class OptionEditor extends GridPane {
	@FXML
	private TextArea descriptionField;
	@FXML
	private TextField costField;
	@FXML
	private Spinner<Integer> rollSpinner;
	@FXML
	private TextField nameField;
	@FXML
	private Button imageButton;
	@FXML
	private TextField styleClassesField;

	private Option option;
	private Section section;
	private Runnable onNameChange;

	public OptionEditor(Option option, Section section) {
		this.option = option;
		this.section = section;

		FXMLLoader loader = new FXMLLoader(getClass().getResource("OptionEditor.fxml"));
		loader.setController(this);
		loader.setRoot(this);
		try {
			loader.load();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@FXML
	void initialize() {
		rollSpinner.setValueFactory(new IntegerSpinnerValueFactory(0, 999, 0, 1));

		if (option == null) {
			this.setDisable(true);
		} else {
			nameField.setText(option.getTitle());
			nameField.textProperty().addListener(new ChangeListener<String>() {
				@Override
				public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
					option.setTitle(newValue);
					MainWindow.touch();
					onNameChange.run();
				}
			});

			costField.setText(option.getCost());
			costField.textProperty().addListener(new ChangeListener<String>() {
				@Override
				public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
					option.setCost(newValue);
					MainWindow.touch();
				}
			});

			rollSpinner.getValueFactory().setValue(option.getRollRange());
			rollSpinner.valueProperty().addListener(new ChangeListener<Integer>() {
				@Override
				public void changed(ObservableValue<? extends Integer> observable, Integer oldValue, Integer newValue) {
					option.setRollRange(newValue);
					MainWindow.touch();
				}
			});
			if (!section.isRollable()) {
				rollSpinner.setDisable(true);
				rollSpinner.setTooltip(new Tooltip("The section does not allow for rolling."));
			}
			// Only allow numeric input
			rollSpinner.getEditor().textProperty().addListener(new ChangeListener<String>() {
				@Override
				public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
					if (newValue.length() >= 3) {
						rollSpinner.getEditor().setText(newValue.substring(0, 3));
					} else if (!newValue.matches("\\d*")) {
						rollSpinner.getEditor().setText(newValue.replaceAll("[^\\d]", ""));
					}
				}
			});
			// Hack to force the control to commit its new value on focus loss.
			// From https://stackoverflow.com/questions/32340476/
			rollSpinner.focusedProperty().addListener(new ChangeListener<Boolean>() {
				@Override
				public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue) {
					if (newValue == false) {
						String text = rollSpinner.getEditor().getText();
						SpinnerValueFactory<Integer> valueFactory = rollSpinner.getValueFactory();
						if (valueFactory != null) {
							StringConverter<Integer> converter = valueFactory.getConverter();
							if (converter != null) {
								try {
									Integer value = converter.fromString(text);
									valueFactory.setValue(value);
								} catch (Exception e) {
									valueFactory.setValue(valueFactory.getValue());
								}
							}
						}
					}
				}
			});

			descriptionField.setText(option.getDescription());
			descriptionField.textProperty().addListener(new ChangeListener<String>() {
				@Override
				public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
					option.setDescription(newValue);
					MainWindow.touch();
				}
			});

			styleClassesField.setText(option.getClasses());
			styleClassesField.textProperty().addListener(new ChangeListener<String>() {
				@Override
				public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
					option.setClasses(newValue);
					MainWindow.touch();
				}
			});

			updateImage();
			imageButton.setOnAction(new EventHandler<ActionEvent>() {
				@Override
				public void handle(ActionEvent event) {
					ImageEditor.show(getScene().getWindow(), option.getImage(), img -> {
						try {
							if (option.getImage() != null)
								option.getImage().delete();
						} catch (IOException e) {
							// TODO handle more responsibly
							e.printStackTrace();
						}
						option.setImage(img);
						updateImage();
					}, section.getAspectRatio());
				}
			});
		}
	}

	private void updateImage() {
		ImageView image = (ImageView) imageButton.getGraphic();
		if (option.getImage() == null) {
			image.setImage(null);
		} else {
			image.setImage(option.getImage().toFX());
		}
	}

	public void setOnNameChange(Runnable onNameChange) {
		this.onNameChange = onNameChange;
	}

	public void focusNameField() {
		nameField.requestFocus();
	}
}
