package cyoastudio.templating;

import java.io.*;
import java.nio.file.Path;
import java.util.*;

import org.json.*;

import cyoastudio.data.Image;
import javafx.scene.paint.Color;

public class Style {
	public static Map<String, Object> parseStyleDefinition(Path source) throws IOException {
		Map<String, Object> values = new HashMap<>();
		FileInputStream input = new FileInputStream(source.toFile());
		JSONObject o = new JSONObject(new JSONTokener(input));
		for (String key : o.keySet()) {
			values.put(key, parseField(key, o.getString(key), source.getParent()));
		}
		input.close();
		return values;
	}

	public static Map<String, Object> parseStringMap(Map<String, Object> source) throws IOException {
		Map<String, Object> values = new HashMap<>();
		for (String key : source.keySet()) {
			values.put(key, parseField(key, (String) source.get(key), null));
		}
		return values;
	}

	// if source is null all images have to be base64, otherwise file references
	private static Object parseField(String fieldName, String data, Path source) throws IOException {
		if (fieldName.toLowerCase().endsWith("color")) {
			return Color.web(data);
		} else if (fieldName.toLowerCase().endsWith("image")) {
			if (source == null) {
				return new Image(data);
			} else {
				try {
					return new Image(source.resolve(data));
				} catch (IOException e) {
					return new Image();
				}
			}
		}

		return null;
	}

	public static Map<String, Object> defaultStyle() {
		//Path temp = Files.createTempDirectory(null);
		// TODO
		return new HashMap<>();
	}
}
