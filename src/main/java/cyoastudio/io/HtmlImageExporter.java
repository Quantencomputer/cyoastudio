package cyoastudio.io;

import java.awt.image.BufferedImage;
import java.util.function.Consumer;

import org.slf4j.*;

import cyoastudio.data.Image;
import javafx.animation.PauseTransition;
import javafx.beans.value.*;
import javafx.concurrent.Worker;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.WritableImage;
import javafx.scene.text.FontSmoothingType;
import javafx.scene.web.*;
import javafx.stage.Stage;

public class HtmlImageExporter {
	final static Logger logger = LoggerFactory.getLogger(HtmlImageExporter.class);

	public static void convert(String source, Consumer<Image> callback) {
		Stage stage = new Stage();
		WebView browser = new WebView();
		browser.setFontSmoothingType(FontSmoothingType.GRAY);
		ScrollPane pane = new ScrollPane(browser);
		Scene scene = new Scene(pane);
		stage.setScene(scene);
		stage.show();

		WebEngine engine = browser.getEngine();
		engine.loadContent(source);

		engine.getLoadWorker().stateProperty().addListener(new ChangeListener<Worker.State>() {
			@Override
			public void changed(ObservableValue<? extends Worker.State> observable, Worker.State oldValue,
					Worker.State newValue) {
				if (newValue != Worker.State.SUCCEEDED)
					return;

				try {
					String heightText = browser.getEngine()
							.executeScript(
									"window.getComputedStyle(document.body).getPropertyValue('height')")
							.toString();
					double height = Double.valueOf(heightText.replace("px", ""));
					browser.setMinHeight(height + 128); // A bit of extra space to avoid scrolling
					browser.setMinWidth(1280);
				} catch (Exception e) {
					logger.error("Error while setting up screenshot browser", e);
					stage.close();
					callback.accept(null);
				}

				final PauseTransition pt = new PauseTransition();
				// TODO don't have this depend on time
				pt.setDuration(new javafx.util.Duration(100));
				pt.setOnFinished(ev -> {
					try {
						WritableImage image = browser.snapshot(null, null);
						BufferedImage bufferedImage = SwingFXUtils.fromFXImage(image, null);
						callback.accept(new Image(bufferedImage));
					} catch (Exception e) {
						logger.error("Error while taking screenshot", e);
						callback.accept(null);
					} finally {
						stage.close();
					}
				});
				pt.play();
			}
		});
	}
}
