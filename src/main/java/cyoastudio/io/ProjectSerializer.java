package cyoastudio.io;

import java.awt.Color;
import java.io.*;
import java.nio.file.Path;

import org.zeroturnaround.zip.*;

import com.github.zafarkhaja.semver.Version;
import com.google.gson.*;

import cyoastudio.Application;
import cyoastudio.data.*;
import cyoastudio.templating.Style;

public class ProjectSerializer {
	private static final String PROJECT_JSON_FILENAME = "project.json";
	private static Gson gson;
	
	static {
		GsonBuilder builder = new GsonBuilder();
		builder.registerTypeAdapter(Image.class, new ImageAdapter());
		builder.registerTypeAdapter(Color.class, new ColorAdapter());
		gson = builder.create();
	}
	
	public static byte[] toBytes(Project project) throws IOException {
		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		Writer writer = new OutputStreamWriter(stream);
		
		ExportPackage p = new ExportPackage();
		p.project = project;
		p.version = Application.getVersion().toString();
		gson.toJson(p, writer);
		writer.close();
		return stream.toByteArray();
	}
	
	public static Project fromBytes(byte[] bytes) throws IOException {
		GsonBuilder builder = new GsonBuilder();
		builder.registerTypeAdapter(Image.class, new ImageAdapter());
		Gson gson = builder.create();
		Reader reader = new InputStreamReader(new ByteArrayInputStream(bytes));
		ExportPackage p = gson.fromJson(reader, ExportPackage.class);
		reader.close();
		
		if (!Version.valueOf(p.version).lessThanOrEqualTo(Application.getVersion())) {
			throw new IOException("The project was created with a newer version of this program.");
		}
		
		if (p.project.getStyle() != null)
			p.project.setStyle(Style.parseStringMap(p.project.getStyle()));
		
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
}
