package cyoastudio.gui;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import org.controlsfx.control.PropertySheet;
import org.controlsfx.control.PropertySheet.Item;

import cyoastudio.data.*;
import cyoastudio.templating.Template;
import javafx.beans.value.ObservableValue;
import javafx.fxml.*;
import javafx.scene.control.SplitPane;
import javafx.scene.paint.Color;
import javafx.scene.web.WebView;

public class StyleEditor extends SplitPane {
	@FXML
	private PropertySheet propertySheet;
	@FXML
	private WebView preview;

	private Map<String, Object> styleOptions;
	private Template template;
	
	private static Project previewProject;

	public StyleEditor(Map<String, Object> styleOptions, Template template) {
		this.styleOptions = styleOptions;
		this.template = template;

		FXMLLoader loader = new FXMLLoader(getClass().getResource("StyleEditor.fxml"));
		loader.setController(this);
		loader.setRoot(this);
		try {
			loader.load();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@FXML
	void initialize() {
		if (styleOptions.containsKey("optionColor")) {
			System.out.println(((Color) styleOptions.get("optionColor")));
		}
		
		if (styleOptions != null) {
			List<Item> items = styleOptions.entrySet().stream().map(e -> {
				String key = e.getKey();
				Object value = e.getValue();
				return new PropertySheet.Item() {
					@Override
					public void setValue(Object value) {
						styleOptions.put(key, value);
					}

					@Override
					public Object getValue() {
						return styleOptions.get(key);
					}

					@Override
					public Class<?> getType() {
						return value.getClass();
					}

					@Override
					public Optional<ObservableValue<? extends Object>> getObservableValue() {
						return Optional.empty();
					}

					@Override
					public String getName() {
						return key;
					}

					@Override
					public String getDescription() {
						return null;
					}

					@Override
					public String getCategory() {
						return null;
					}
				};
			}).collect(Collectors.toList());
			propertySheet.getItems().addAll(items);
		}
	}
	
	@FXML
	void updatePreview() {
		previewProject.setStyle(styleOptions);
		String html = template.render(previewProject);
		preview.getEngine().loadContent(html);
	}
	
	static {
		previewProject = new Project();
		previewProject.setTitle("Lorem ipsum");
		Option o = new Option();
		o.setTitle("Dolor sit");
		o.setDescription("Lorem ipsum et dolor sit amet");
		Section s = new Section();
		s.getOptions().add(o);
		s.getOptions().add(o);
		s.getOptions().add(o);
		s.getOptions().add(o);
		s.setOptionsPerRow(4);
		s.setDescription("Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet. Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet.");
		previewProject.getSections().add(s);
	}
}
