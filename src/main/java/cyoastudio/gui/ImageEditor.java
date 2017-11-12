package cyoastudio.gui;

import java.io.*;
import java.net.URL;
import java.nio.file.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

import javax.imageio.ImageIO;

import org.apache.commons.io.FileUtils;
import org.controlsfx.control.SnapshotView;
import org.controlsfx.dialog.ExceptionDialog;
import org.slf4j.*;

import cyoastudio.Preferences;
import cyoastudio.data.Image;
import javafx.application.Platform;
import javafx.fxml.*;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.image.ImageView;
import javafx.scene.input.*;
import javafx.scene.layout.BorderPane;
import javafx.stage.*;
import javafx.stage.FileChooser.ExtensionFilter;

public class ImageEditor extends BorderPane {
	final Logger logger = LoggerFactory.getLogger(ImageEditor.class);

	@FXML
	private SnapshotView snapshotView;

	private Image image;
	private Consumer<Image> onSuccess;
	private double ratio;

	private ImageView imageView;

	public ImageEditor(Image image, Consumer<Image> onSuccess, double ratio) {
		this.image = image;
		this.onSuccess = onSuccess;
		this.ratio = ratio;

		FXMLLoader loader = new FXMLLoader(getClass().getResource("ImageEditor.fxml"));
		loader.setController(this);
		loader.setRoot(this);
		try {
			loader.load();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@FXML
	void close() {
		Stage stage = (Stage) getScene().getWindow();
		stage.close();
	}

	@FXML
	void confirm() {
		// TODO make a proper dialog out of this
		onSuccess.accept(image);
		MainWindow.touch();
		close();
	}

	@FXML
	void saveImage() {
		if (image == null) {
			Alert a = new Alert(AlertType.ERROR);
			a.setContentText("No image to export!");
			a.show();
		}

		FileChooser fileChooser = new FileChooser();
		fileChooser.setInitialDirectory(Preferences.getPath("lastImageDir").toFile());
		fileChooser.setTitle("Save image");
		fileChooser.getExtensionFilters().addAll(
				new ExtensionFilter("PNG Images", "*.png"));
		File selected = fileChooser.showSaveDialog(getScene().getWindow());
		if (selected != null) {
			Preferences.setPath("lastImageDir", selected.toPath());

			try {
				ImageIO.write(image.toBufferedImage(), "png", selected);
			} catch (IOException e) {
				logger.error("Error while saving the image.", e);

				ExceptionDialog exceptionDialog = new ExceptionDialog(e);
				exceptionDialog.setTitle("Error");
				exceptionDialog.setHeaderText("Error while saving the image.");
				exceptionDialog.show();
			}
		}
	}

	@FXML
	void loadImage() {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setInitialDirectory(Preferences.getPath("lastImageDir").toFile());
		fileChooser.setTitle("Open image");
		fileChooser.getExtensionFilters().addAll(
				new ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.bmp", "*.gif"));
		File selected = fileChooser.showOpenDialog(getScene().getWindow());
		if (selected != null) {
			Preferences.setPath("lastImageDir", selected.toPath().getParent());

			loadImage(selected);
		}
	}

	private void loadImage(File source) {
		try {
			replaceImage(Image.copy(source.toPath()));
		} catch (IOException e) {
			logger.error("Could not load image", e);
		}
	}

	private void replaceImage(Image newImage) {
		try {
			if (image != null)
				image.delete();
			image = newImage;
			updateImage();
		} catch (Exception e) {
			logger.error("Could not cleanup image image", e);
		}
	}

	@FXML
	void clear() {
		replaceImage(null);
	}

	@FXML
	void trim() {
		if (image != null && snapshotView.isSelectionActive()) {
			Rectangle2D area = snapshotView.transformSelectionToNodeCoordinates();
			replaceImage(image.trim(area));
		}
	}

	@FXML
	void initialize() {
		if (ratio != 0) {
			snapshotView.setFixedSelectionRatio(ratio);
			snapshotView.setSelectionRatioFixed(true);
		}

		updateImage();
	}

	@FXML
	void dragDropped(DragEvent event) {
		Dragboard db = event.getDragboard();
		if (db.hasFiles() && db.getFiles().size() == 1) {
			loadImage(db.getFiles().get(0));
		} else if (db.hasImage()) {
			try {
				replaceImage(Image.fromData(db.getImage()));
			} catch (IOException e) {
				logger.error("Could not save image", e);
			}
		} else if (db.hasString()) {
			try {
				this.setDisable(true);
				URL url = new URL(db.getString());
				Path tempFile = Files.createTempFile("download_image", null);

				ExecutorService executor = Executors.newSingleThreadExecutor();
				Future<Boolean> download = executor.submit(() -> {
					try {
						FileUtils.copyURLToFile(url, tempFile.toFile());
						return true;
					} catch (IOException e) {
						logger.error("Error while downloading file", e);
						return false;
					}
				});

				if (download.get(5, TimeUnit.SECONDS)) {
					javafx.scene.image.Image fxImage = new javafx.scene.image.Image(
							tempFile.toUri().toURL().toString());
					Image img = Image.fromData(fxImage);
					Files.delete(tempFile);

					replaceImage(img);
					this.setDisable(false);
					Platform.runLater(ImageEditor.this::updateImage);
				}
			} catch (Exception e) {
				logger.warn("Error while downloading file", e);
				event.setDropCompleted(false);
				this.setDisable(false);
			}
		} else {
			event.setDropCompleted(false);
		}
		event.consume();
	}

	@FXML
	void dragOver(DragEvent event) {
		Dragboard db = event.getDragboard();
		if (db.hasFiles() && db.getFiles().size() == 1) {
			event.acceptTransferModes(TransferMode.COPY);
		} else if (db.hasImage()) {
			event.acceptTransferModes(TransferMode.COPY);
		}
		event.acceptTransferModes(TransferMode.COPY);
	}

	void updateImage() {
		if (image == null) {
			snapshotView.setNode(null);
		} else {
			imageView = new ImageView(image.toFX());
			imageView.setPreserveRatio(true);

			snapshotView.setMaxWidth(imageView.getImage().getWidth());
			snapshotView.setMaxHeight(imageView.getImage().getHeight());

			snapshotView.setNode(imageView);
		}
		snapshotView.setSelectionActive(false);
		snapshotView.setSelection(null);
	}

	public static void show(Window parent, Image image, Consumer<Image> onSuccess, double ratio) {
		Stage stage = new Stage();
		stage.initModality(Modality.WINDOW_MODAL);
		stage.initOwner(parent);
		stage.setScene(new Scene(new ImageEditor(image.copy(), onSuccess, ratio)));
		stage.show();
	}

}
