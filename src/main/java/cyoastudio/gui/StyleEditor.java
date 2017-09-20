package cyoastudio.gui;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import org.controlsfx.control.PropertySheet;
import org.controlsfx.control.PropertySheet.Item;
import org.controlsfx.property.editor.PropertyEditor;

import cyoastudio.data.*;
import cyoastudio.templating.Template;
import javafx.animation.*;
import javafx.beans.value.*;
import javafx.fxml.*;
import javafx.scene.control.*;
import javafx.scene.web.WebView;
import javafx.util.*;

public class StyleEditor extends SplitPane {
	@FXML
	private PropertySheet propertySheet;
	@FXML
	private WebView preview;
	@FXML
	private TextArea customCssArea;

	private Project project;

	private Timeline updateTimeLine = new Timeline(new KeyFrame(Duration.millis(100), e -> updatePreview()));

	public StyleEditor() {
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
		Callback<Item, PropertyEditor<?>> oldFactory = propertySheet.getPropertyEditorFactory();
		propertySheet.setPropertyEditorFactory(new Callback<PropertySheet.Item, PropertyEditor<?>>() {
			@Override
			public PropertyEditor<?> call(PropertySheet.Item item) {
				if (item.getValue() instanceof Image) {
					return ImagePropertyEditor.createImageEditor(item);
				}

				return oldFactory.call(item);
			}
		});
		customCssArea.textProperty().addListener(new ChangeListener<String>() {
			@Override
			public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
				if (newValue != null) {
					getProject().setCss(newValue);
					MainWindow.touch();
					queuePreviewUpdate();
				}
			}
		});

		updateFields();
	}

	private void updateFields() {
		if (project == null) {
			propertySheet.getItems().clear();
		} else {
			propertySheet.getItems().clear();
			List<Item> items = getStyleOptions().entrySet().stream().map(e -> {
				String key = e.getKey();
				Object value = e.getValue();
				return new PropertySheet.Item() {
					@Override
					public void setValue(Object value) {
						// When destroying the old list is destroyed, all values are set to null.
						// This has to be interrupted to prevent the insertion of null into the style
						// options.
						if (getStyleOptions().containsKey(key) && value != null) {
							getStyleOptions().put(key, value);
							queuePreviewUpdate();
						}
					}

					@Override
					public Object getValue() {
						return getStyleOptions().get(key);
					}

					@Override
					public Class<?> getType() {
						if (value == null) {
							return Object.class;
						}
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

			customCssArea.setText(project.getCss());
		}
	}

	public void editStyle(Project project) {
		this.project = project;

		updateFields();
	}

	void queuePreviewUpdate() {
		updateTimeLine.playFromStart();
	}

	void updatePreview() {
		if (getTemplate() != null && getStyleOptions() != null) {
			Project previewProject = previewProject();
			previewProject.setStyle(getStyleOptions());
			previewProject.setCss(project.getCss());
			String html = getTemplate().render(previewProject);
			preview.getEngine().loadContent(html);
		}
	}

	private static Project previewProject() {
		Project previewProject = new Project();
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
		s.setDescription(
				"Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet. Lorem ipsum dolor sit amet, consetetur sadipscing elitr, sed diam nonumy eirmod tempor invidunt ut labore et dolore magna aliquyam erat, sed diam voluptua. At vero eos et accusam et justo duo dolores et ea rebum. Stet clita kasd gubergren, no sea takimata sanctus est Lorem ipsum dolor sit amet.");
		previewProject.getSections().add(s);

		return previewProject;
	}

	private Project getProject() {
		return project;
	}

	private Template getTemplate() {
		return project.getTemplate();
	}

	private Map<String, Object> getStyleOptions() {
		return project.getStyleOptions();
	}
}
