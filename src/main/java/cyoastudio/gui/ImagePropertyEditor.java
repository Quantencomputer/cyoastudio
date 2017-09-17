package cyoastudio.gui;

import org.controlsfx.control.PropertySheet.Item;
import org.controlsfx.property.editor.*;

import cyoastudio.data.Image;
import javafx.beans.property.*;
import javafx.event.*;
import javafx.scene.control.Button;

public class ImagePropertyEditor extends Button {
	private SimpleObjectProperty<Image> valueProperty = new SimpleObjectProperty<>();

	public ImagePropertyEditor() {
		setText("Edit image...");
		setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent event) {
				ImageEditor.show(getScene().getWindow(), valueProperty.get(), img -> {
					valueProperty.set(img);
				}, 0);
			}
		});
	}

	public ObjectProperty<Image> valueProperty() {
		return valueProperty;
	}

	public Image getValue() {
		return valueProperty.get();
	}

	public void setValue(Image value) {
		valueProperty.set(value);
	}

	public static PropertyEditor<?> createImageEditor(Item property) {
		return new AbstractPropertyEditor<Image, ImagePropertyEditor>(property, new ImagePropertyEditor()) {
			@Override
			protected ObjectProperty<Image> getObservableValue() {
				return getEditor().valueProperty();
			}

			@Override
			public void setValue(Image value) {
				getEditor().setValue(value);
			}
		};

	}
}
