package cyoastudio.io;

import java.io.*;
import java.nio.file.Path;

import org.zeroturnaround.zip.*;

import com.github.zafarkhaja.semver.Version;
import com.google.gson.*;

import cyoastudio.Application;
import cyoastudio.data.*;
import cyoastudio.templating.Style;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

public class ProjectSerializer {
	private static final String PROJECT_JSON_FILENAME = "project.json";
	private static Gson gson;

	static {
		GsonBuilder builder = new GsonBuilder();
		builder.registerTypeAdapter(Image.class, new ImageAdapter());
		builder.registerTypeAdapter(Color.class, new ColorAdapter());
		builder.registerTypeAdapter(Font.class, new FontAdapter());
		gson = builder.create();
	}

	public static byte[] toBytes(Project project) throws IOException {
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		toStream(project, stream);
		return stream.toByteArray();
	}

	public static void toStream(Project project, OutputStream stream) throws IOException {
		Writer writer = new OutputStreamWriter(stream);

		ExportPackage p = new ExportPackage();
		p.project = project;
		p.version = Application.getVersion().toString();
		gson.toJson(p, writer);
		writer.close();
	}

	public static Project fromBytes(byte[] bytes) throws IOException {
		InputStreamReader reader = new InputStreamReader(new ByteArrayInputStream(bytes));
		return fromReader(reader);
	}

	public static Project fromReader(Reader reader) throws IOException {
		ExportPackage p = gson.fromJson(reader, ExportPackage.class);
		reader.close();

		if (!Version.valueOf(p.version).lessThanOrEqualTo(Application.getVersion())) {
			throw new IOException("The project was created with a newer version of this program.");
		}

		if (p.project.getStyleOptions() != null)
			p.project.setStyle(Style.parseStringMap(p.project.getStyleOptions()));

		// Remove null values, just in case
		p.project.getSections().removeIf(s -> s == null);
		for (Section s : p.project.getSections()) {
			s.getOptions().removeIf(o -> o == null);
		}

		return p.project;
	}

	public static void writeToZip(Project project, Path target) throws IOException {
		ZipEntrySource[] entries = new ZipEntrySource[] {
				new ByteSource(PROJECT_JSON_FILENAME, toBytes(project))
		};
		ZipUtil.pack(entries, target.toFile());
	}

	public static Project readFromZip(Path target) throws IOException {
		return fromBytes(ZipUtil.unpackEntry(target.toFile(), PROJECT_JSON_FILENAME));
	}

	// TODO remove
	@SuppressWarnings("unchecked")
	public static <T> T deepCopy(T source) {
		try {
			return (T) gson.fromJson(gson.toJsonTree(source), source.getClass());
		} catch (ClassCastException e) {
			throw new RuntimeException("Couldn't copy object", e);
		}
	}
}
