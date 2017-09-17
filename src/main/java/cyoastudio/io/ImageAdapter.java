package cyoastudio.io;

import java.io.IOException;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.*;

import cyoastudio.data.Image;

public class ImageAdapter extends TypeAdapter<Image> {

	@Override
	public Image read(JsonReader reader) throws IOException {
		if (reader.peek() == JsonToken.NULL) {
			reader.nextNull();
			return null;
		}

		return new Image(reader.nextString());
	}

	@Override
	public void write(JsonWriter writer, Image value) throws IOException {
		if (value == null) {
			writer.nullValue();
			return;
		}

		writer.value(value.toBase64());
	}

}
