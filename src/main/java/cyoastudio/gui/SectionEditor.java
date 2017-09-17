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
	private Label nameLabel;

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

	private Section section;

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
			nameLabel.setText(section.getTitle());
			descriptionField.setText(section.getDescription());
			positioningBox.setValue(section.getImagePositioning());
			optionsPerRowSlider.setValue(section.getOptionsPerRow());
			aspectXField.setText(String.valueOf(section.getAspectX()));
			aspectYField.setText(String.valueOf(section.getAspectY()));

			descriptionField.textProperty().addListener(new ChangeListener<String>() {
				@Override
				public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
					section.setDescription(newValue);
					MainWindow.touch();
				}
			});
			positioningBox.valueProperty().addListener(new ChangeListener<ImagePositioning>() {
				@Override
				public void changed(ObservableValue<? extends ImagePositioning> observable, ImagePositioning oldValue,
						ImagePositioning newValue) {
					section.setImagePositioning(newValue);
					MainWindow.touch();
				}
			});
			optionsPerRowSlider.valueProperty().addListener(new ChangeListener<Number>() {
				@Override
				public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
					section.setOptionsPerRow(newValue.intValue());
				}
			});

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
		}
	}
}
