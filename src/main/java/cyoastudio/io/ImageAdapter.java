package cyoastudio.io;

import java.io.IOException;
import java.util.List;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.*;

import cyoastudio.data.Image;

public class ImageAdapter extends TypeAdapter<Image> {
	private List<String> usedIdentifiers;

	public ImageAdapter(List<String> usedIdentifiers) {
		super();
		this.usedIdentifiers = usedIdentifiers;
	}

	@Override
	public Image read(JsonReader reader) throws IOException {
		if (reader.peek() == JsonToken.NULL) {
			reader.nextNull();
			return null;
		}

		String identifier = reader.nextString();
		if (usedIdentifiers != null) {
			usedIdentifiers.add(identifier);
		}
		return Image.fromStorage(identifier);
	}

	@Override
	public void write(JsonWriter writer, Image value) throws IOException {
		if (value == null) {
			writer.nullValue();
			return;
		}
 
		if (usedIdentifiers != null) {
			usedIdentifiers.add(value.getIdentifier());
		}
		writer.value(value.getIdentifier());
	}

}
