package cyoastudio.gui;

import java.io.IOException;

import cyoastudio.data.*;
import javafx.beans.value.*;
import javafx.event.*;
import javafx.fxml.*;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;

public class OptionEditor extends GridPane {
	@FXML
	private TextArea descriptionField;
	@FXML
	private Label nameLabel;
	@FXML
	private Button imageButton;
	@FXML
	private TextField styleClassesField;

	private Option option;
	private Section section;

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
		if (option == null) {
			this.setDisable(true);
		} else {
			nameLabel.setText(option.getTitle());

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
}
