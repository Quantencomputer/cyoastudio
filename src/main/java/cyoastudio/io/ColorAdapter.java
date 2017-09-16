package cyoastudio.io;

import java.io.IOException;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.*;

import cyoastudio.templating.ProjectConverter;
import javafx.scene.paint.Color;

public class ColorAdapter extends TypeAdapter<Color> {
	@Override
	public Color read(JsonReader reader) throws IOException {
		if (reader.peek() == JsonToken.NULL) {
			reader.nextNull();
			return null;
		}

		return Color.web(reader.nextString());
	}

	@Override
	public void write(JsonWriter writer, Color value) throws IOException {
		if (value == null) {
			writer.nullValue();
			return;
		}

		String s = ProjectConverter.convert(value);

		writer.value(s);
	}
}
