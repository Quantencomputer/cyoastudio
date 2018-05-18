package cyoastudio.io;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.util.*;

import org.apache.commons.io.IOUtils;
import org.slf4j.*;
import org.zeroturnaround.zip.*;

import com.github.zafarkhaja.semver.Version;
import com.google.gson.*;

import cyoastudio.Application;
import cyoastudio.data.*;
import cyoastudio.templating.Style;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

public class ProjectSerializer {
	private static final Logger logger = LoggerFactory.getLogger(ProjectSerializer.class);
	private static final String PROJECT_JSON_FILENAME = "project.json";
	private static final String PROJECT_VERSION_FILENAME = "version";

	private static Gson base64Gson() {
		GsonBuilder builder = new GsonBuilder();
		builder.registerTypeAdapter(Image.class, new Base64ImageAdapter());
		builder.registerTypeAdapter(Color.class, new ColorAdapter());
		builder.registerTypeAdapter(Font.class, new FontAdapter());
		return builder.create();
	}

	private static Gson identifierGson() {
		return identifierGson(null);
	}

	private static Gson identifierGson(List<String> usedIdentifiers) {
		GsonBuilder builder = new GsonBuilder();
		builder.registerTypeAdapter(Image.class, new ImageAdapter(usedIdentifiers));
		builder.registerTypeAdapter(Color.class, new ColorAdapter());
		builder.registerTypeAdapter(Font.class, new FontAdapter());
		return builder.create();
	}

	public enum ImageType {
		BASE64, REFERENCE, SHORT_REFERENCE
	}

	private static Gson buildGson(ImageType imageType) {
		if (imageType == ImageType.BASE64) {
			return base64Gson();
		} else {
			return identifierGson();
		}
	}

	public static byte[] toBytes(Project project, Gson gson) throws IOException {
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		toStream(project, stream, gson);
		return stream.toByteArray();
	}

	public static byte[] toBytes(Project project, ImageType imageType) throws IOException {
		return toBytes(project, buildGson(imageType));
	}

	public static void toStream(Project project, OutputStream stream, Gson gson) throws IOException {
		Writer writer = new OutputStreamWriter(stream, Charset.forName("UTF-8"));

		ExportPackage p = new ExportPackage();
		p.project = project;
		p.version = Application.getVersion().toString();

		gson.toJson(p, writer);
		writer.close();
	}

	public static Project fromBytes(byte[] bytes, Gson gson) throws IOException {
		InputStreamReader reader = new InputStreamReader(new ByteArrayInputStream(bytes), Charset.forName("UTF-8"));
		return fromReader(reader, gson);
	}

	public static Project fromReader(Reader reader, Gson gson) throws IOException {
		ExportPackage p = gson.fromJson(reader, ExportPackage.class);
		reader.close();

		// TODO make this check somewhere else?
		if (Version.valueOf(p.version).greaterThan(Application.getVersion())) {
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

	public static Project fromReader(FileReader reader, ImageType imageType) throws IOException {
		return fromReader(reader, buildGson(imageType));
	}

	public static void writeToZip(Project project, Path target) throws IOException {
		byte[] version = Application.getVersion().toString().getBytes();

		List<String> usedIdentifiers = new ArrayList<>();
		final byte[] jsonBytes = toBytes(project, identifierGson(usedIdentifiers));

		List<ZipEntrySource> entries = new ArrayList<>();
		entries.add(new ByteSource(PROJECT_JSON_FILENAME, jsonBytes));
		entries.add(new ByteSource(PROJECT_VERSION_FILENAME, version));

		for (String iden : usedIdentifiers) {
			Path path = Application.getDatastorage().getFile(iden);
			logger.debug("Adding file " + iden + " to archive");
			entries.add(new FileSource("data/" + iden, path.toFile()));
		}
		ZipUtil.pack(entries.toArray(new ZipEntrySource[entries.size()]), target.toFile());
	}

	public static Project readFromZip(Path target) throws IOException {
		final Version version = readFileVersion(target);
		Application.getDatastorage().flush();
		logger.info("Loading project made with version " + version.toString());
		boolean useBase64Images = version.lessThan(Version.forIntegers(0, 3, 0));
		if (!useBase64Images) {
			// Extract images form the archive
			ZipUtil.unpack(target.toFile(), Application.getDatastorage().getPath().toFile(), new NameMapper() {
				@Override
				public String map(String name) {
					if (name.startsWith("data/")) {
						logger.debug("Found file in archive " + name);
						return name.substring(5);
					} else {
						return null;
					}
				}
			});
		}
		final Gson gson = buildGson(useBase64Images ? ImageType.BASE64 : ImageType.REFERENCE);
		return fromBytes(ZipUtil.unpackEntry(target.toFile(), PROJECT_JSON_FILENAME), gson);
	}

	public static Version readFileVersion(Path target) throws IOException {
		final byte[] data = ZipUtil.unpackEntry(target.toFile(), PROJECT_VERSION_FILENAME);
		if (data == null) {
			// Best guess, since the actual version information is hidden inside the JSON
			return Version.forIntegers(0, 2, 2);
		} else {
			InputStream stream = new ByteArrayInputStream(data);
			return Version.valueOf(IOUtils.toString(stream, Charset.forName("UTF-8")));
		}
	}

	// TODO remove
	@SuppressWarnings("unchecked")
	public static <T> T deepCopy(T source) {
		Gson gson = base64Gson();
		try {
			return (T) gson.fromJson(gson.toJsonTree(source), source.getClass());
		} catch (ClassCastException e) {
			throw new RuntimeException("Couldn't copy object", e);
		}
	}
}
