package cyoastudio.gui;

import java.io.*;
import java.nio.file.*;
import java.util.Optional;
import java.util.stream.Collectors;

import org.controlsfx.dialog.ExceptionDialog;

import cyoastudio.data.*;
import cyoastudio.io.*;
import cyoastudio.templating.*;
import javafx.beans.value.*;
import javafx.collections.FXCollections;
import javafx.event.EventHandler;
import javafx.fxml.*;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ListView.EditEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.web.WebView;
import javafx.stage.*;
import javafx.stage.FileChooser.ExtensionFilter;

public class MainWindow extends BorderPane {

	@FXML
	private BorderPane contentPane;
	@FXML
	private ListView<String> sectionList;
	@FXML
	private ListView<String> optionList;
	@FXML
	private TabPane tabPane;
	@FXML
	private Tab previewTab;
	@FXML
	private WebView preview;

	private Stage stage;
	private Project project = new Project();
	private Path saveLocation;
	private static boolean dirty = false;

	// TODO remove this hack and make dirty not global
	public static void touch() {
		dirty = true;
	}

	public MainWindow(Stage stage) {
		FXMLLoader loader = new FXMLLoader(getClass().getResource("MainWindow.fxml"));
		loader.setController(this);
		loader.setRoot(this);
		try {
			loader.load();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		this.stage = stage;
		stage.setTitle("CYOA Studio");
		Scene scene = new Scene(this, 800, 600);
		stage.setScene(scene);
		stage.show();
		stage.setOnCloseRequest(event -> {
			exitProgram();
			event.consume();
		});
	}

	@FXML
	void initialize() {
		sectionList.setCellFactory(v -> EditCell.createStringEditCell());
		sectionList.setOnEditCommit(new EventHandler<ListView.EditEvent<String>>() {
			@Override
			public void handle(EditEvent<String> event) {
				project.getSections().get(event.getIndex()).setTitle(event.getNewValue());
				refreshSectionList();
				sectionList.getSelectionModel().select(event.getIndex());
			}
		});
		sectionList.getSelectionModel().selectedIndexProperty().addListener(e -> editSection());

		optionList.setCellFactory(v -> EditCell.createStringEditCell());
		optionList.setOnEditCommit(new EventHandler<ListView.EditEvent<String>>() {
			@Override
			public void handle(EditEvent<String> event) {
				getCurrentSection().getOptions().get(event.getIndex()).setTitle(event.getNewValue());
				refreshOptionList();
				optionList.getSelectionModel().select(event.getIndex());
			}
		});
		optionList.getSelectionModel().selectedIndexProperty().addListener(e -> editOption());

		tabPane.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<Tab>() {
			@Override
			public void changed(ObservableValue<? extends Tab> observable, Tab oldValue, Tab newValue) {
				if (newValue == previewTab) {
					updatePreview();
				}
			}
		});
	}

	private void editSection() {
		refreshOptionList();
		SectionEditor editor = new SectionEditor(getCurrentSection());
		contentPane.setCenter(editor);
	}

	private void editOption() {
		OptionEditor editor = new OptionEditor(getCurrentOption(), getCurrentSection());
		contentPane.setCenter(editor);
	}

	@FXML
	void exitProgram() {
		if (!isUserSure("Exit")) {
			return;
		}

		stage.close();
	}

	private boolean isUserSure(String action) {
		if (!dirty) {
			return true;
		}

		ButtonType continu = new ButtonType(action);
		ButtonType saveFirst = new ButtonType("Save");

		Alert alert = new Alert(AlertType.CONFIRMATION);
		alert.setTitle("Are you sure?");
		alert.setHeaderText("Are you sure?");
		// TODO check if there actually are such changes
		alert.setContentText("There may be unsaved changed. Are you sure you want to continue?");

		alert.getButtonTypes().setAll(continu, saveFirst, ButtonType.CANCEL);

		Optional<ButtonType> result = alert.showAndWait();
		if (!result.isPresent()) {
			return false;
		} else if (result.get() == ButtonType.CANCEL) {
			return false;
		} else if (result.get() == continu) {
			return true;
		} else if (result.get() == saveFirst) {
			return saveProject();
		} else {
			return false;
		}
	}

	@FXML
	void exportImage() {

	}

	@FXML
	void newProject() {
		if (!isUserSure("Exit")) {
			return;
		}

		project = new Project();
		refreshSectionList();
		buildStylePane();
		tabPane.getSelectionModel().select(0);
		saveLocation = null;
		dirty = false;
	}

	@FXML
	void openProject() {
		if (!isUserSure("Exit")) {
			return;
		}

		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Open project");
		fileChooser.getExtensionFilters().addAll(
				new ExtensionFilter("CYOA Studio Project", "*.cyoa"),
				new ExtensionFilter("All files", "*"));
		File selected = fileChooser.showOpenDialog(stage);
		if (selected != null) {
			try {
				project = ProjectSerializer.readFromZip(selected.toPath());
				refreshSectionList();
				buildStylePane();
				tabPane.getSelectionModel().select(0);
				saveLocation = selected.toPath();
				dirty = false;
			} catch (IOException e) {
				showError("Couldn't open file", e);
			}
		}
	}

	private void buildStylePane() {
		// TODO Auto-generated method stub

	}

	@FXML
	boolean saveProject() {
		if (saveLocation == null) {
			selectSaveLocation();
		}
		if (saveLocation != null) {
			save();
			return true;
		} else {
			return false;
		}
	}

	@FXML
	void saveProjectAs() {
		if (selectSaveLocation()) {
			save();
		}
	}

	private void save() {
		assert saveLocation != null;
		try {
			Files.deleteIfExists(saveLocation);
			Files.createFile(saveLocation);
			ProjectSerializer.writeToZip(project, saveLocation);
			dirty = false;
		} catch (IOException e) {
			saveLocation = null;
			showError("Error while saving.", e);
		}
	}

	private boolean selectSaveLocation() {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setTitle("Save");
		fileChooser.getExtensionFilters().addAll(
				new ExtensionFilter("CYOA Studio Project", "*.cyoa"),
				new ExtensionFilter("All files", "*"));
		File selected = fileChooser.showSaveDialog(stage);
		if (selected == null) {
			return false;
		} else {
			saveLocation = selected.toPath();
			return true;
		}
	}

	@FXML
	void newSection() {
		touch();
		project.getSections().add(new Section());
		refreshSectionList();

		// Make the new entry editable
		sectionList.layout();
		int i = sectionList.getItems().size() - 1;
		sectionList.scrollTo(i);
		sectionList.getSelectionModel().select(i);
		sectionList.edit(i);
	}

	private void refreshSectionList() {
		sectionList.setItems(FXCollections.observableList(
				project.getSections().stream()
						.map(x -> x.getTitle())
						.collect(Collectors.toList())));
	}

	@FXML
	void deleteSection() {
		Alert a = new Alert(AlertType.CONFIRMATION);
		a.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO, ButtonType.CANCEL);
		a.setTitle("Are you sure?");
		a.setHeaderText("Are you sure?");
		a.setContentText("Are you sure you want to delete this section?");
		Optional<ButtonType> result = a.showAndWait();
		if (result.get() != ButtonType.YES)
			return;

		touch();
		int i = sectionList.getSelectionModel().getSelectedIndex();
		if (i >= 0) {
			project.getSections().remove(i);
			refreshSectionList();
			if (i < sectionList.getItems().size()) {
				sectionList.getSelectionModel().select(i);
			} else if (i - 1 >= 0 && i - 1 < sectionList.getItems().size()) {
				sectionList.getSelectionModel().select(i - 1);
			}
		}
	}

	private Section getCurrentSection() {
		int selectedIndex = sectionList.getSelectionModel().getSelectedIndex();
		if (selectedIndex >= 0)
			return project.getSections().get(selectedIndex);
		else
			return null;
	}

	private Option getCurrentOption() {
		int selectedIndex = optionList.getSelectionModel().getSelectedIndex();
		if (selectedIndex >= 0)
			return getCurrentSection().getOptions().get(selectedIndex);
		else
			return null;
	}

	@FXML
	void newOption() {
		touch();
		if (getCurrentSection() != null) {
			getCurrentSection().getOptions().add(new Option());
			refreshOptionList();

			// Make the new entry editable
			optionList.layout();
			int i = optionList.getItems().size() - 1;
			optionList.scrollTo(i);
			optionList.getSelectionModel().select(i);
			optionList.edit(i);
		}
	}

	private void refreshOptionList() {
		Section cur = getCurrentSection();
		if (cur == null) {
			optionList.setItems(FXCollections.emptyObservableList());
		} else {
			optionList.setItems(FXCollections.observableList(
					cur.getOptions().stream()
							.map(x -> x.getTitle())
							.collect(Collectors.toList())));
		}
	}

	@FXML
	void deleteOption() {
		Alert a = new Alert(AlertType.CONFIRMATION);
		a.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO, ButtonType.CANCEL);
		a.setTitle("Are you sure?");
		a.setHeaderText("Are you sure?");
		a.setContentText("Are you sure you want to delete this option?");
		Optional<ButtonType> result = a.showAndWait();
		if (result.get() != ButtonType.YES)
			return;

		touch();
		if (getCurrentSection() != null) {
			int i = optionList.getSelectionModel().getSelectedIndex();
			if (i >= 0) {
				getCurrentSection().getOptions().remove(i);
				refreshOptionList();
				if (i < optionList.getItems().size()) {
					optionList.getSelectionModel().select(i);
				} else if (i - 1 >= 0 && i - 1 < optionList.getItems().size()) {
					optionList.getSelectionModel().select(i - 1);
				}
			}
		}
	}

	private void updatePreview() {
		try {
			// TODO change to actual implementation
			Template t = new Template(new FileInputStream("./templates/quantum/page.html.mustache"));
			preview.getEngine().loadContent(t.render(project));
			System.out.println(t.render(project));
		} catch (IOException e) {
			// TODO something?
			e.printStackTrace();
		}
	}

	@FXML
	void setStyle() throws FileNotFoundException, IOException {
		project.setTemplate(new Template(new FileInputStream("./templates/quantum/page.html.mustache")),
				Style.parseStyleDefinition(Paths.get("./templates/quantum/style_options.json")));
		System.out.println(project.getStyle().get("backgroundImage").getClass());
	}

	private void showError(String message, IOException ex) {
		System.out.println(message);
		ex.printStackTrace();

		ExceptionDialog exceptionDialog = new ExceptionDialog(ex);
		exceptionDialog.show();
	}
}
