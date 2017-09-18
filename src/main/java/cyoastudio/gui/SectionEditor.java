package cyoastudio.gui;

import java.io.IOException;

import cyoastudio.data.Section;
import cyoastudio.data.Section.ImagePositioning;
import javafx.beans.value.*;
import javafx.collections.FXCollections;
import javafx.fxml.*;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.util.StringConverter;

public class SectionEditor extends GridPane {
	@FXML
	private TextField nameField;
	@FXML
	private TextArea descriptionField;
	@FXML
	private ChoiceBox<ImagePositioning> positioningBox;
	@FXML
	private Slider optionsPerRowSlider;
	@FXML
	private TextField aspectXField;
	@FXML
	private TextField aspectYField;
	@FXML
	private TextField styleClassesField;

	private Section section;
	private Runnable onNameChange;

	public SectionEditor(Section section) {
		this.section = section;
		FXMLLoader loader = new FXMLLoader(getClass().getResource("SectionEditor.fxml"));
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
		positioningBox.setItems(FXCollections.observableArrayList(
				ImagePositioning.TOP, ImagePositioning.ALTERNATING, ImagePositioning.LEFT, ImagePositioning.RIGHT));
		positioningBox.setConverter(new StringConverter<ImagePositioning>() {
			@Override
			public String toString(ImagePositioning p) {
				switch (p) {
				case TOP:
					return "On top";
				case RIGHT:
					return "On the right";
				case LEFT:
					return "On the left";
				case ALTERNATING:
					return "Alternating left and right";
				default:
					throw new RuntimeException();
				}
			}

			@Override
			public ImagePositioning fromString(String string) {
				return null;
			}
		});

		if (section == null) {
			this.setDisable(true);
		} else {
			nameField.setText(section.getTitle());
			nameField.textProperty().addListener(new ChangeListener<String>() {
				@Override
				public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
					section.setTitle(newValue);
					MainWindow.touch();
					onNameChange.run();
				}
			});

			descriptionField.setText(section.getDescription());
			descriptionField.textProperty().addListener(new ChangeListener<String>() {
				@Override
				public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
					section.setDescription(newValue);
					MainWindow.touch();
				}
			});

			positioningBox.setValue(section.getImagePositioning());
			positioningBox.valueProperty().addListener(new ChangeListener<ImagePositioning>() {
				@Override
				public void changed(ObservableValue<? extends ImagePositioning> observable, ImagePositioning oldValue,
						ImagePositioning newValue) {
					section.setImagePositioning(newValue);
					MainWindow.touch();
				}
			});

			optionsPerRowSlider.setValue(section.getOptionsPerRow());
			optionsPerRowSlider.valueProperty().addListener(new ChangeListener<Number>() {
				@Override
				public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
					section.setOptionsPerRow(newValue.intValue());
				}
			});

			aspectXField.setText(String.valueOf(section.getAspectX()));
			aspectXField.textProperty().addListener(new ChangeListener<String>() {
				@Override
				public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
					if (!newValue.matches("\\d*")) {
						aspectXField.setText(newValue.replaceAll("[^\\d]", ""));
					} else {
						section.setAspectX(Integer.valueOf(newValue));
					}
				}
			});

			aspectYField.setText(String.valueOf(section.getAspectY()));
			aspectYField.textProperty().addListener(new ChangeListener<String>() {
				@Override
				public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
					if (!newValue.matches("\\d*")) {
						aspectYField.setText(newValue.replaceAll("[^\\d]", ""));
					} else {
						section.setAspectY(Integer.valueOf(newValue));
					}
				}
			});

			styleClassesField.setText(section.getClasses());
			styleClassesField.textProperty().addListener(new ChangeListener<String>() {
				@Override
				public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
					section.setClasses(newValue);
					MainWindow.touch();
				}
			});
		}
	}

	public void setOnNameChange(Runnable onNameChange) {
		this.onNameChange = onNameChange;
	}

	public void focusNameField() {
		nameField.requestFocus();
	}
}
