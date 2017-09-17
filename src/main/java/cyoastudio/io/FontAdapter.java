package cyoastudio.io;

import java.io.IOException;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.*;

import cyoastudio.Application;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.text.Font;

public class FontAdapter extends TypeAdapter<Font> {
	@Override
	public Font read(JsonReader reader) throws IOException {
		if (reader.peek() == JsonToken.NULL) {
			reader.nextNull();
			return null;
		}

		return getFont(reader.nextString());
	}

	@Override
	public void write(JsonWriter writer, Font value) throws IOException {
		if (value == null) {
			writer.nullValue();
			return;
		}

		String s = value.getFamily();

		writer.value(s);
	}

	public static Font getFont(String name) {
		if (!Font.getFamilies().contains(name)) {
			Alert a = new Alert(AlertType.WARNING);
			a.setContentText("Could not find font " + name + ". Defaulting to system font.");
			Application.positionDialog(a);
			a.show();
		}
		return Font.font(name);
	}
}
