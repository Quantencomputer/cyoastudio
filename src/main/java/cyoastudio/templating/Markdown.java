package cyoastudio.templating;

import java.util.Arrays;

import com.vladsch.flexmark.ast.Document;
import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.ext.typographic.TypographicExtension;
import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.superscript.SuperscriptExtension;
import com.vladsch.flexmark.util.options.*;

public class Markdown {
	private static final MutableDataHolder options = new MutableDataSet()
			.set(Parser.EXTENSIONS, Arrays.asList(
					TablesExtension.create(),
					StrikethroughExtension.create(),
					TypographicExtension.create(),
					SuperscriptExtension.create()));
	private static final Parser parser = Parser.builder(options).build();
	private static final HtmlRenderer renderer = HtmlRenderer.builder(options).build();

	public static String render(String source) {
		Document document = parser.parse(source);
		return renderer.render(document);
	}
}
