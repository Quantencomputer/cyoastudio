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

	private String pageSource;
	private String styleSource;

	public Template(String pageSource, String styleSource) {
		this.pageSource = pageSource;
		this.styleSource = styleSource;
	}

	public String render(Project project) {
		return render(project, true, 0, project.getSections().size());
	}

	public String render(Project project, boolean includeTitle, int start, int end) {
		Map<String, Object> data = ProjectConverter.convert(project, includeTitle, start, end);
		Map<String, Object> styleData = ProjectConverter.convertStyle(project.getStyleOptions());

		String style = renderTemplateFromString(styleSource, styleData);

		data.put("style", style);

		return renderTemplateFromString(pageSource, data);
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
			String page = defaultPageSource();
			String style = defaultStyleSource();
			return new Template(page, style);
		} catch (Exception e) {
			logger.error("Could not load default template", e);
			return new Template("", "");
		}
	}

	public static String defaultStyleSource() throws IOException {
		return IOUtils.toString(Template.class.getResource("defaultTemplate/style.css.mustache"),
				Charset.forName("UTF-8"));
	}

	public static String defaultPageSource() throws IOException {
		return IOUtils.toString(Template.class.getResource("defaultTemplate/page.html.mustache"),
				Charset.forName("UTF-8"));
	}

}
