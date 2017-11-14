package cyoastudio.io;

import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.function.Consumer;

import javax.imageio.ImageIO;

import org.apache.commons.io.IOUtils;
import org.slf4j.*;

import cyoastudio.Preferences;
import cyoastudio.data.*;
import cyoastudio.io.ProjectSerializer.ImageType;
import cyoastudio.templating.ProjectConverter.Bounds;
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
	public static void convert(Project p, Path targetFolder, String prefix, OutputFormat outputFormat,
			Consumer<String> callback) {
		if (callback == null) {
			callback = error -> {
			};
		}
		HtmlImageExporter exporter = new HtmlImageExporter(p, targetFolder, prefix, outputFormat, callback);
		exporter.setHeightLimit(p.getSettings().getMaxImageHeight());
		exporter.export();
	}

	public enum OutputFormat {
		HTML, IMAGE
	}

	final static Logger logger = LoggerFactory.getLogger(HtmlImageExporter.class);

	private Project project;
	private Consumer<String> callback;

	private Stage stage;
	private WebView browser;
	private ProgressBar bar;

	private String prefix;
	private Path targetFoler;

	private Bounds bounds = new Bounds(0, 0);
	private int curSubdivisions = 1;
	private int curSubdivision = 0;
	private int pageNumber = 1;
	private double height;

	private double heightLimit = 8096;

	private ChangeListener<State> listener;

	private OutputFormat outputFormat;

	private HtmlImageExporter(Project project, Path targetFolder, String prefix, OutputFormat outputFormat,
			Consumer<String> callback) {
		this.project = project;
		this.targetFoler = targetFolder;
		this.prefix = prefix;
		this.callback = callback;
		this.outputFormat = outputFormat;

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
		logger.info("Loading " + bounds.toString());
		boolean includeTitle = (bounds.lowerSection == 0 && bounds.lowerOption == 0);
		String source = project.getTemplate().render(project, includeTitle, bounds, ImageType.REFERENCE);

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
		bounds.upperSection += 1;

		logger.info("Probing " + bounds.toString());
		bar.setProgress((double) bounds.lowerSection / (double) project.getSections().size());
		loadSections(() -> {
			logger.info("Finished loading " + bounds.toString());
			if (height > getHeightLimit()) {
				rollback();
			} else if (bounds.upperSection >= project.getSections().size() - 1) {
				render();
			} else {
				probe();
			}
		});
	}

	private Bounds subdivide(int sectionNumber, int subdivisions, int currentPart) {
		Section s = project.getSections().get(sectionNumber);
		final int optionsPerRow = s.getOptionsPerRow();
		final int rows = (s.getOptions().size() - 1) / optionsPerRow + 1;
		if (subdivisions > rows) {
			return null;
		}

		final int rowsPerSubdivision = (rows - 1) / subdivisions + 1;
		final int optionsPerSubdivision = rowsPerSubdivision * optionsPerRow;
		final int lowerOption = optionsPerSubdivision * currentPart;
		final int upperOption = lowerOption + optionsPerSubdivision;

		if (currentPart >= subdivisions - 1) {
			return new Bounds(sectionNumber, sectionNumber, lowerOption, Integer.MAX_VALUE);
		} else {
			return new Bounds(sectionNumber, sectionNumber, lowerOption, upperOption);
		}
	}

	private void rollback() {
		logger.info("Too big, rolling back");
		bounds.upperSection -= 1;
		if (bounds.upperSection < bounds.lowerSection) {
			logger.info("Subdividing section");
			probeSubdivided();
		} else {
			loadSections(this::render);
		}
	}

	private void render() {
		logger.info("Rendering " + bounds.toString() + ", page " + pageNumber);

		browser.setMinHeight(height + 128); // A bit of extra space to avoid scrolling
		browser.setMaxHeight(browser.getMinHeight());

		final PauseTransition pt = new PauseTransition();
		// TODO don't have this depend on time
		int renderDelay = Preferences.preferences.getInt("renderDelay", 100);
		pt.setDuration(new javafx.util.Duration(renderDelay));
		pt.setOnFinished(ev -> {
			try {
				savePage();

				pageNumber += 1;
				bounds.lowerSection = bounds.upperSection + 1;

				if (bounds.upperSection >= project.getSections().size() - 1) {
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

	private void savePage() throws IOException {
		if (outputFormat == OutputFormat.HTML) {
			String source = project.getTemplate().render(project, pageNumber == 1, bounds, ImageType.BASE64);

			String filename = prefix + Integer.toString(pageNumber) + ".html";
			Path target = targetFoler.resolve(filename);
			FileOutputStream stream = new FileOutputStream(target.toFile());
			IOUtils.write(source, stream, Charset.forName("UTF-8"));
			stream.close();
		} else {
			WritableImage image = browser.snapshot(null, null);
			BufferedImage bufferedImage = SwingFXUtils.fromFXImage(image, null);

			String filename = prefix + Integer.toString(pageNumber) + ".png";
			Path target = targetFoler.resolve(filename);
			ImageIO.write(bufferedImage, "png", target.toFile());
		}
	}

	private void probeSubdivided() {
		curSubdivisions += 1;

		logger.info("Probing " + bounds.toString() + ", currently on division " + curSubdivision);
		bar.setProgress((double) bounds.lowerSection / (double) project.getSections().size());

		bounds = subdivide(bounds.lowerSection, curSubdivisions, curSubdivision);
		if (bounds == null) {
			String error = "Could not split section up";
			logger.error(error);
			stage.close();
			callback.accept(error);
		} else {
			loadSections(() -> {
				logger.info("Finished loading " + bounds.toString());
				if (height > getHeightLimit()) {
					probeSubdivided();
				} else {
					renderSubdivided();
				}
			});
		}
	}

	private void renderSubdivided() {
		logger.info("Rendering " + bounds.toString() + ", page " + pageNumber);

		browser.setMinHeight(height + 128); // A bit of extra space to avoid scrolling
		browser.setMaxHeight(browser.getMinHeight());

		final PauseTransition pt = new PauseTransition();
		// TODO don't have this depend on time
		int renderDelay = Preferences.preferences.getInt("renderDelay", 100);
		pt.setDuration(new javafx.util.Duration(renderDelay));
		pt.setOnFinished(ev -> {
			try {
				savePage();

				pageNumber += 1;

				if (bounds.upperOption == Integer.MAX_VALUE) {
					bounds = new Bounds(bounds.lowerSection + 1, bounds.lowerSection + 1);
					if (bounds.upperSection >= project.getSections().size() - 1) {
						stage.close();
						callback.accept(null);
					} else {
						curSubdivisions = 1;
						curSubdivision = 0;
						probe();
					}
				} else {
					curSubdivision += 1;
					bounds = subdivide(bounds.lowerSection, curSubdivisions, curSubdivision);
					if (bounds == null) {
						String error = "Could not split section up";
						logger.error(error);
						stage.close();
						callback.accept(error);
					} else {
						loadSections(this::renderSubdivided);
					}
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
