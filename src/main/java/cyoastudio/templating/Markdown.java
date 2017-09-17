package cyoastudio.templating;

import com.vladsch.flexmark.ast.Document;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;

public class Markdown {
	private static Parser parser = Parser.builder().build();
	private static HtmlRenderer renderer = HtmlRenderer.builder().build();

	public static String render(String source) {
		Document document = parser.parse(source);
		return renderer.render(document);
	}
}
