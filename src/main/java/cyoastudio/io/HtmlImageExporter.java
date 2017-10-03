package cyoastudio.io;

import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.function.Consumer;

import javax.imageio.ImageIO;

import org.slf4j.*;

import cyoastudio.Preferences;
import cyoastudio.data.Project;
import javafx.animation.PauseTransition;
import javafx.beans.value.*;
import javafx.concurrent.Worker;
import javafx.concurrent.Worker.State;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.VBox;
import javafx.scene.text.FontSmoothingType;
import javafx.scene.web.*;
import javafx.stage.Stage;

public class HtmlImageExporter {
	public static void convert(Project p, Path targetFolder, String prefix, Consumer<String> callback) {
		if (callback == null) {
			callback = error -> {
			};
		}
		HtmlImageExporter exporter = new HtmlImageExporter(p, targetFolder, prefix, callback);
		exporter.setHeightLimit(p.getSettings().getMaxImageHeight());
		exporter.export();
	}

	final static Logger logger = LoggerFactory.getLogger(HtmlImageExporter.class);

	private Project project;
	private Consumer<String> callback;

	private Stage stage;
	private WebView browser;
	private ProgressBar bar;

	private String prefix;
	private Path targetFoler;

	private int start = 0;
	private int end = 0;
	private int pageNumber = 1;
	private double height;

	private double heightLimit = 8096;

	private ChangeListener<State> listener;

	private HtmlImageExporter(Project project, Path targetFolder, String prefix, Consumer<String> callback) {
		this.project = project;
		this.targetFoler = targetFolder;
		this.prefix = prefix;
		this.callback = callback;

		stage = new Stage();
		browser = new WebView();
		browser.setFontSmoothingType(FontSmoothingType.GRAY);
		ScrollPane scrollPane = new ScrollPane(browser);
		bar = new ProgressBar();
		bar.setMaxWidth(Double.MAX_VALUE);
		bar.setPadding(new Insets(8));
		bar.setProgress(0);
		VBox vBox = new VBox(bar, scrollPane);
		Scene scene = new Scene(vBox);
		stage.setScene(scene);
		stage.show();
	}

	private void export() {
		probe();
	}

	private void loadSections(Runnable nextCall) {
		logger.info("Loading " + start + ", " + end);
		boolean includeTitle = (start == 0);
		String source = project.getTemplate().render(project, includeTitle, start, end);

		WebEngine engine = browser.getEngine();
		engine.loadContent(source);

		if (listener != null) {
			engine.getLoadWorker().stateProperty().removeListener(listener);
		}
		listener = new ChangeListener<Worker.State>() {

			@Override
			public void changed(ObservableValue<? extends Worker.State> observable, Worker.State oldValue,
					Worker.State newValue) {
				if (newValue != Worker.State.SUCCEEDED)
					return;

				try {
					engine.executeScript("document.body.style.overflow = 'hidden'");

					String heightText = engine
							.executeScript("window.getComputedStyle(document.body).getPropertyValue('height')")
							.toString();
					height = Double.valueOf(heightText.replace("px", ""));
					browser.setMinHeight(height + 128); // A bit of extra space to avoid scrolling
					browser.setMaxHeight(browser.getMinHeight());
					browser.setMinWidth(1280);
					browser.setMaxWidth(browser.getMinWidth());

					nextCall.run();
				} catch (Exception e) {
					logger.error("Error while setting up screenshot browser", e);
					stage.close();
					callback.accept("Error while taking screenshot");
				}
			}
		};
		engine.getLoadWorker().stateProperty().addListener(listener);
	}

	private void probe() {
		end += 1;

		logger.info("Probing " + start + ", " + end);
		bar.setProgress((double) end / (double) project.getSections().size());
		loadSections(() -> {
			logger.info("Finished loading " + start + ", " + end);
			if (height > getHeightLimit()) {
				rollback();
			} else if (end >= project.getSections().size()) {
				render();
			} else {
				probe();
			}
		});
	}

	private void rollback() {
		end -= 1;
		if (start == end) {
			String error = "Section \"" + project.getSections().get(start).getTitle() + "\" too long to fit on a page";
			logger.error(error);
			stage.close();
			callback.accept(error);
		} else {
			loadSections(HtmlImageExporter.this::render);
		}
	}

	private void render() {
		logger.info("Rendering " + start + ", " + end);

		final PauseTransition pt = new PauseTransition();
		// TODO don't have this depend on time
		int renderDelay = Preferences.preferences.getInt("renderDelay", 100);
		pt.setDuration(new javafx.util.Duration(renderDelay));
		pt.setOnFinished(ev -> {
			try {
				WritableImage image = browser.snapshot(null, null);
				BufferedImage bufferedImage = SwingFXUtils.fromFXImage(image, null);

				String filename = prefix + Integer.toString(pageNumber) + ".png";
				Path target = targetFoler.resolve(filename);
				ImageIO.write(bufferedImage, "png", target.toFile());

				pageNumber += 1;
				start = end;

				if (end >= project.getSections().size()) {
					stage.close();
					callback.accept(null);
				} else {
					probe();
				}
			} catch (Exception e) {
				logger.error("Error while taking screenshot", e);
				stage.close();
				callback.accept("Error while taking screenshot");
			}
		});
		pt.play();
	}

	public double getHeightLimit() {
		return heightLimit;
	}

	public void setHeightLimit(double heightLimit) {
		this.heightLimit = heightLimit;
	}
}
