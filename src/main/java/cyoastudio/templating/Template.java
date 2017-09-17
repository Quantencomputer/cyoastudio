package cyoastudio.templating;

import java.io.*;
import java.nio.charset.Charset;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.slf4j.*;

import com.github.mustachejava.*;

import cyoastudio.data.Project;

public class Template {
	final static Logger logger = LoggerFactory.getLogger(Template.class);

	private String source;

	public Template(String source) {
		this.source = source;
	}

	public Template(InputStream stream) throws IOException {
		this.source = IOUtils.toString(stream, Charset.forName("UTF-8"));
	}

	public String render(Project project) {
		Map<String, Object> data = ProjectConverter.convert(project);

		return renderTemplateFromString(source, data);
	}

	public static String renderTemplateFromStream(Reader stream, Object data) {
		MustacheFactory mf = new DefaultMustacheFactory();
		Mustache mustache = mf.compile(stream, "template");
		StringWriter w = new StringWriter();
		try {
			mustache.execute(w, data).flush();
		} catch (IOException e) {
			throw new RuntimeException("Something went wrong in templating", e);
		}
		return w.toString();
	}

	public static String renderTemplateFromString(String template, Object data) {
		return renderTemplateFromStream(new StringReader(template), data);
	}

	public static Template defaultTemplate() {
		try {
			return new Template(Template.class.getResource("defaultTemplate/page.html.mustache").openStream());
		} catch (Exception e) {
			logger.error("Could not load default template", e);
			return new Template("");
		}
	}

}
