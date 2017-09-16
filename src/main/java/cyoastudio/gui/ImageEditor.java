package cyoastudio.gui;

import java.io.*;
import java.util.function.Consumer;

import org.controlsfx.control.SnapshotView;

import cyoastudio.data.Image;
import javafx.fxml.*;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.image.ImageView;
import javafx.scene.input.*;
import javafx.scene.layout.BorderPane;
import javafx.stage.*;
import javafx.stage.FileChooser.ExtensionFilter;

public class ImageEditor extends BorderPane {
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
    void loadImage() {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Open image");
		fileChooser.getExtensionFilters().addAll(
				new ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.bmp", "*.gif"));
		File selected = fileChooser.showOpenDialog(getScene().getWindow());
		if (selected != null) {
			loadImage(selected);
		}
    }

	private void loadImage(File source) {
		try {
			image = new Image(source.toPath());
			updateImage();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

    @FXML
    void clear() {
    	image = null;
    	updateImage();
    }

    @FXML
    void trim() {
    	if (image != null && snapshotView.isSelectionActive()) {
    		Rectangle2D area = snapshotView.transformSelectionToNodeCoordinates();
    		image = image.trim(area);
    		updateImage();
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
    		image = new Image(db.getImage());
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
		stage.setScene(new Scene(new ImageEditor(image, onSuccess, ratio)));
		stage.show();
	}

}
