package cyoastudio.templating;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;

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
			return parseColor(data);
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

	public static Color parseColor(String data) {
		Matcher m;
		Pattern hexAlpha = Pattern.compile("#([0-9a-fA-F]{2})([0-9a-fA-F]{2})([0-9a-fA-F]{2})([0-9a-fA-F]{2})");
		m = hexAlpha.matcher(data.toUpperCase());
		if (m.matches()) {
			return new Color(
					Integer.valueOf(m.group(1), 16) / 255.0,
					Integer.valueOf(m.group(2), 16) / 255.0,
					Integer.valueOf(m.group(3), 16) / 255.0,
					Integer.valueOf(m.group(4), 16) / 255.0);
		}

		Pattern hex = Pattern.compile("#([0-9a-fA-F]{2})([0-9a-fA-F]{2})([0-9a-fA-F]{2})");
		m = hex.matcher(data.toUpperCase());
		if (m.matches()) {
			return new Color(
					Integer.valueOf(m.group(1), 16) / 255.0,
					Integer.valueOf(m.group(2), 16) / 255.0,
					Integer.valueOf(m.group(3), 16) / 255.0,
					1.0);
		}

		Pattern rgba = Pattern.compile("rgba *\\( *([0-9]+), *([0-9]+), *([0-9]+) *, ([01](.[0-9]+)?) *\\)");
		m = rgba.matcher(data);
		if (m.matches()) {
			return new Color(
					Integer.valueOf(m.group(1)) / 255.0,
					Integer.valueOf(m.group(2)) / 255.0,
					Integer.valueOf(m.group(3)) / 255.0,
					Double.valueOf(m.group(4)));
		}

		Pattern rgb = Pattern.compile("rgb *\\( *([0-9]+), *([0-9]+), *([0-9]+) *\\)");
		m = rgb.matcher(data);
		if (m.matches()) {
			return new Color(
					Integer.valueOf(m.group(1)) / 255.0,
					Integer.valueOf(m.group(2)) / 255.0,
					Integer.valueOf(m.group(3)) / 255.0,
					1.0);
		}

		return null;
	}
}
