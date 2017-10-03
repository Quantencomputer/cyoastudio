package cyoastudio.gui;

import java.io.IOException;

import cyoastudio.Preferences;
import javafx.beans.value.*;
import javafx.fxml.*;
import javafx.scene.*;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class OptionsWindow {
	@FXML
	private TextField delayField;
	private Stage stage;

	public OptionsWindow() {
		FXMLLoader loader = new FXMLLoader(getClass().getResource("OptionsWindow.fxml"));
		loader.setController(this);

		try {
			Parent window = loader.load();

			stage = new Stage();
			stage.setScene(new Scene(window));
			stage.show();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@FXML
	void initialize() {
		delayField.setText(Integer.toString(Preferences.preferences.getInt("renderDelay", 100)));
		delayField.textProperty().addListener(new ChangeListener<String>() {
			@Override
			public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
				if (!newValue.matches("\\d*")) {
					delayField.setText(newValue.replaceAll("[^\\d]", ""));
				} else if (newValue.isEmpty()) {
					delayField.setText("0");
				} else {
					int value = Integer.valueOf(newValue);
					if (value > 999999) {
						value = 999999;
						delayField.setText(Integer.toString(value));
					} else {
						Preferences.preferences.putInt("renderDelay", value);
					}
				}
			}
		});
	}

	@FXML
	void close() {
		stage.close();
	}
}
