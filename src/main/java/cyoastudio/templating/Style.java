package cyoastudio.templating;

import java.awt.Color;
import java.io.*;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.*;

import org.json.*;

import cyoastudio.data.*;

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

	public static Color parseColor(String data) {
		Matcher m;
		Pattern hexAlpha = Pattern.compile("#([0-9a-fA-F]{2})([0-9a-fA-F]{2})([0-9a-fA-F]{2})([0-9a-fA-F]{2})");
		m = hexAlpha.matcher(data.toUpperCase());
		if (m.matches()) {
			return new Color(
					Integer.valueOf(m.group(1), 16),
					Integer.valueOf(m.group(2), 16),
					Integer.valueOf(m.group(3), 16),
					Integer.valueOf(m.group(4), 16));
		}

		Pattern hex = Pattern.compile("#([0-9a-fA-F]{2})([0-9a-fA-F]{2})([0-9a-fA-F]{2})");
		m = hex.matcher(data.toUpperCase());
		if (m.matches()) {
			return new Color(
					Integer.valueOf(m.group(1), 16),
					Integer.valueOf(m.group(2), 16),
					Integer.valueOf(m.group(3), 16));
		}

		Pattern rgba = Pattern.compile("rgba *\\( *([0-9]+), *([0-9]+), *([0-9]+) *, ([01](.[0-9]+)?) *\\)");
		m = rgba.matcher(data);
		if (m.matches()) {
			return new Color(
					Integer.valueOf(m.group(1)),
					Integer.valueOf(m.group(2)),
					Integer.valueOf(m.group(3)),
					(int) (Double.valueOf(m.group(4)) * 255));
		}

		Pattern rgb = Pattern.compile("rgb *\\( *([0-9]+), *([0-9]+), *([0-9]+) *\\)");
		m = rgb.matcher(data);
		if (m.matches()) {
			return new Color(
					Integer.valueOf(m.group(1)),
					Integer.valueOf(m.group(2)),
					Integer.valueOf(m.group(3)));
		}

		return null;
	}
}
