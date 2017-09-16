package cyoastudio.templating;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import cyoastudio.data.*;

public class ProjectConverter {
	public static Map<String, Object> convert(Project p) {
		Map<String, Object> data = new HashMap<>();

		if (!p.getTitle().isEmpty())
			data.put("title", p.getTitle());
		data.put("sections", p.getSections().stream()
				.map(ProjectConverter::convert).collect(Collectors.toList()));
		data.put("style", convertStyle(p.getStyle()));

		return data;
	}

	public static Map<String, Object> convert(Section s) {
		Map<String, Object> data = new HashMap<String, Object>();

		if (!s.getTitle().isEmpty())
			data.put("title", Markdown.render(s.getTitle()));
		if (!s.getDescription().isEmpty())
			data.put("description", Markdown.render(s.getDescription()));
		data.put("options", s.getOptions().stream()
				.map(ProjectConverter::convert).collect(Collectors.toList()));

		data.put("numPerRow", s.getOptionsPerRow());
		switch (s.getImagePositioning()) {
		case ALTERNATING:
			data.put("imageClass", "alternating-images");
			break;
		case TOP:
			data.put("imageClass", "top-images");
			break;
		case LEFT:
			data.put("imageClass", "left-images");
			break;
		case RIGHT:
			data.put("imageClass", "right-images");
			break;
		}

		return data;
	}

	public static Map<String, Object> convert(Option o) {
		Map<String, Object> data = new HashMap<String, Object>();

		if (!o.getTitle().isEmpty())
			data.put("title", Markdown.render(o.getTitle()));
		if (!o.getDescription().isEmpty())
			data.put("description", Markdown.render(o.getDescription()));
		if (o.getImage() != null)
			data.put("image", convert(o.getImage()));

		return data;
	}

	public static Map<String, Object> convertStyle(Map<String, Object> style) {
		Map<String, Object> data = new HashMap<String, Object>();

		for (Entry<String, Object> i : style.entrySet()) {
			String key = i.getKey();
			Object value = i.getValue();
			String repr;
			if (value instanceof Color) {
				repr = convert((Color) value);
			} else if (value instanceof Image) {
				//if (style.containsKey(key + "Color")) {
					data.put(key + "Blended",
							convert(((Image) value).blend(Color.GREEN)));
					// (Color) style.get(key + "Color")
				//}
				repr = convert((Image) value);
			} else if (value instanceof String) {
				repr = (String) value;
			} else {
				repr = value.toString();
				System.out.println("Unknown value type in style options");
			}
			data.put(key, repr);
		}

		return data;
	}

	public static String convert(Image i) {
		return "data:image/png;base64," + i.toBase64();
	}

	public static String convert(Color c) {
		return "#" +
				pad(Integer.toHexString(c.getRed())) +
				pad(Integer.toHexString(c.getGreen())) +
				pad(Integer.toHexString(c.getBlue())) +
				pad(Integer.toHexString(c.getAlpha()));
	}

	private static final String pad(String s) {
		return (s.length() == 1) ? "0" + s : s;
	}
}
