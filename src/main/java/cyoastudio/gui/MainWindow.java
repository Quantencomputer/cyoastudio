package cyoastudio.gui;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.controlsfx.dialog.ExceptionDialog;
import org.slf4j.*;
import org.zeroturnaround.zip.ZipUtil;

import cyoastudio.Application;
import cyoastudio.data.*;
import cyoastudio.io.ProjectSerializer;
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

	final Logger logger = LoggerFactory.getLogger(MainWindow.class);

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
	@FXML
	private TextField projectTitleBox;
	@FXML
	private Tab styleTab;

	private Stage stage;
	private Project project = new Project();
	private Path saveLocation;
	private static boolean dirty = false;
	private Section selectedSection;
	private Option selectedOption;
	private StyleEditor editor;

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
		sectionList.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<String>() {
			@Override
			public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
				if (newValue != null) {
					editSection();
				}
			}
		});

		optionList.setCellFactory(v -> EditCell.createStringEditCell());
		optionList.setOnEditCommit(new EventHandler<ListView.EditEvent<String>>() {
			@Override
			public void handle(EditEvent<String> event) {
				selectedSection.getOptions().get(event.getIndex()).setTitle(event.getNewValue());
				refreshOptionList();
				optionList.getSelectionModel().select(event.getIndex());
			}
		});
		optionList.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<String>() {
			@Override
			public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
				if (newValue != null) {
					editOption();
				}
			}
		});

		tabPane.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<Tab>() {
			@Override
			public void changed(ObservableValue<? extends Tab> observable, Tab oldValue, Tab newValue) {
				if (newValue == previewTab) {
					updatePreview();
				}
			}
		});

		projectTitleBox.textProperty().addListener(new ChangeListener<String>() {
			@Override
			public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
				if (!newValue.equals(project.getTitle())) {
					touch();
					project.setTitle(newValue);
				}
			}
		});

		editor = new StyleEditor();
		styleTab.setContent(editor);

		cleanUp();
	}

	private void editSection() {
		int selectedIndex = sectionList.getSelectionModel().getSelectedIndex();
		if (selectedIndex >= 0)
			selectedSection = project.getSections().get(selectedIndex);
		else
			selectedSection = null;

		refreshOptionList();
		SectionEditor editor = new SectionEditor(selectedSection);
		contentPane.setCenter(editor);

		optionList.getSelectionModel().clearSelection();
	}

	private void editOption() {
		int selectedIndex = optionList.getSelectionModel().getSelectedIndex();
		if (selectedIndex >= 0)
			selectedOption = selectedSection.getOptions().get(selectedIndex);
		else
			selectedOption = null;

		OptionEditor editor = new OptionEditor(selectedOption, selectedSection);
		contentPane.setCenter(editor);

		sectionList.getSelectionModel().clearSelection();
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
		alert.setContentText("There may be unsaved changed. Are you sure you want to continue?");
		centerDialog(alert);

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
		// TODO
	}

	@FXML
	void exportHTML() {
		FileChooser fileChooser = new FileChooser();
		try {
			if (Application.preferences.get("lastDir", null) != null && Files.isDirectory(Paths.get(Application.preferences.get("lastDir", null))))
				fileChooser.setInitialDirectory(Paths.get(Application.preferences.get("lastDir", "")).toFile());
		} catch (Exception e) {
			logger.warn("Coulnd't access preferences", e);
		}
		fileChooser.setTitle("Export HTML");
		fileChooser.getExtensionFilters().addAll(
				new ExtensionFilter("HTML files", "*.html", ".htm"),
				new ExtensionFilter("All files", "*"));
		File selected = fileChooser.showSaveDialog(stage);
		if (selected != null) {
			try {
				Application.preferences.put("lastDir", selected.toPath().getParent().toAbsolutePath().toString());
				String text = project.getTemplate().render(project);

				FileUtils.writeStringToFile(selected, text, Charset.forName("UTF-8"));
			} catch (Exception e) {
				showError("Error while exporting", e);
			}
		}
	}

	@FXML
	void exportJson() {
		FileChooser fileChooser = new FileChooser();
		try {
			if (Application.preferences.get("lastDir", null) != null && Files.isDirectory(Paths.get(Application.preferences.get("lastDir", null))))
				fileChooser.setInitialDirectory(Paths.get(Application.preferences.get("lastDir", "")).toFile());
		} catch (Exception e) {
			logger.warn("Coulnd't access preferences", e);
		}
		fileChooser.setTitle("Export plain text");
		fileChooser.getExtensionFilters().addAll(
				new ExtensionFilter("JSON files", "*.json"),
				new ExtensionFilter("All files", "*"));
		File selected = fileChooser.showSaveDialog(stage);
		if (selected != null) {
			try {
				Application.preferences.put("lastDir", selected.toPath().getParent().toAbsolutePath().toString());
				byte[] jsonData = ProjectSerializer.toBytes(project);

				FileUtils.writeByteArrayToFile(selected, jsonData);
			} catch (Exception e) {
				showError("Error while exporting", e);
			}
		}
	}

	@FXML
	void newProject() {
		if (!isUserSure("New project")) {
			return;
		}

		project = new Project();
		cleanUp();
		saveLocation = null;
	}

	private void cleanUp() {
		refreshSectionList();
		updateStyleEditor();
		updatePreview();
		dirty = false;
		selectedOption = null;
		selectedSection = null;
		projectTitleBox.setText(project.getTitle());
	}

	@FXML
	void openProject() {
		if (!isUserSure("Open project")) {
			return;
		}

		FileChooser fileChooser = new FileChooser();
		try {
			if (Application.preferences.get("lastDir", null) != null && Files.isDirectory(Paths.get(Application.preferences.get("lastDir", null))))
				fileChooser.setInitialDirectory(Paths.get(Application.preferences.get("lastDir", "")).toFile());
		} catch (Exception e) {
			logger.warn("Coulnd't access preferences", e);
		}
		fileChooser.setTitle("Open project");
		fileChooser.getExtensionFilters().addAll(
				new ExtensionFilter("CYOA Studio Project", "*.cyoa"),
				new ExtensionFilter("All files", "*"));
		File selected = fileChooser.showOpenDialog(stage);
		if (selected != null) {
			try {
				Application.preferences.put("lastDir", selected.toPath().getParent().toAbsolutePath().toString());
				project = ProjectSerializer.readFromZip(selected.toPath());
				saveLocation = selected.toPath();
				cleanUp();

			} catch (IOException e) {
				showError("Couldn't open file", e);
			}
		}
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
			Application.preferences.put("lastDir", saveLocation.getParent().toAbsolutePath().toString());
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
		try {
			if (Application.preferences.get("lastDir", null) != null && Files.isDirectory(Paths.get(Application.preferences.get("lastDir", null))))
				fileChooser.setInitialDirectory(Paths.get(Application.preferences.get("lastDir", "")).toFile());
		} catch (Exception e) {
			logger.warn("Coulnd't access preferences", e);
		}
		fileChooser.setTitle("Save");
		fileChooser.getExtensionFilters().addAll(
				new ExtensionFilter("CYOA Studio Project", "*.cyoa"),
				new ExtensionFilter("All files", "*"));
		File selected = fileChooser.showSaveDialog(stage);
		if (selected == null) {
			return false;
		} else {
			Application.preferences.put("lastDir", selected.toPath().getParent().toAbsolutePath().toString());
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
		centerDialog(a);
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

	@FXML
	void newOption() {
		touch();
		if (selectedSection != null) {
			selectedSection.getOptions().add(new Option());
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
		Section cur = selectedSection;
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
		centerDialog(a);
		Optional<ButtonType> result = a.showAndWait();
		if (result.get() != ButtonType.YES)
			return;

		touch();
		if (selectedSection != null) {
			int i = optionList.getSelectionModel().getSelectedIndex();
			if (i >= 0) {
				selectedSection.getOptions().remove(i);
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
		String website = project.getTemplate().render(project);
		preview.getEngine().loadContent(website);

		saveTemp(website);
	}

	public static void saveTemp(String website) {
		// TODO remove
		try {
			File tempFile = File.createTempFile("rendered_site", ".html");
			FileUtils.writeStringToFile(tempFile, website, Charset.forName("UTF-8"));
			System.out.println(tempFile.getAbsolutePath());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void updateStyleEditor() {
		editor.editStyle(project.getStyle(), project.getTemplate());
	}

	private void showError(String message, Exception ex) {
		logger.error(message, ex);

		ExceptionDialog exceptionDialog = new ExceptionDialog(ex);
		centerDialog(exceptionDialog);
		exceptionDialog.show();
	}

	@FXML
	void templateFromFile() {
		FileChooser fileChooser = new FileChooser();
		try {
			if (Application.preferences.get("lastDir", null) != null && Files.isDirectory(Paths.get(Application.preferences.get("lastDir", null))))
				fileChooser.setInitialDirectory(Paths.get(Application.preferences.get("lastDir", "")).toFile());
		} catch (Exception e) {
			logger.warn("Coulnd't access preferences", e);
		}
		fileChooser.setTitle("Import template");
		fileChooser.getExtensionFilters().addAll(
				new ExtensionFilter("CYOA Studio Project", "*.cyoatemplate"),
				new ExtensionFilter("All files", "*"));
		File selected = fileChooser.showOpenDialog(stage);
		if (selected != null) {
			try {
				Application.preferences.put("lastDir", selected.toPath().getParent().toAbsolutePath().toString());
				Path tempDirectory = Files.createTempDirectory(null);
				ZipUtil.unpack(selected, tempDirectory.toFile());

				loadTemplate(tempDirectory);

				FileUtils.deleteDirectory(tempDirectory.toFile());
			} catch (IOException e) {
				showError("Could not load template", e);
			}
		}
	}

	@FXML
	void templateFromFolder() {
		DirectoryChooser directoryChooser = new DirectoryChooser();
		try {
			if (Application.preferences.get("lastDir", null) != null && Files.isDirectory(Paths.get(Application.preferences.get("lastDir", null))))
				directoryChooser.setInitialDirectory(Paths.get(Application.preferences.get("lastDir", "")).toFile());
		} catch (Exception e) {
			logger.warn("Coulnd't access preferences", e);
		}
		directoryChooser.setTitle("Import template");
		File selected = directoryChooser.showDialog(stage);
		if (selected != null) {
			try {
				Application.preferences.put("lastDir", selected.toPath().toAbsolutePath().toString());
				loadTemplate(selected.toPath());
			} catch (IOException e) {
				showError("Could not load template", e);
			}
		}
	}

	private void loadTemplate(Path path) throws IOException {
		FileInputStream stream = new FileInputStream(path.resolve("page.html.mustache").toFile());
		Template template = new Template(stream);
		stream.close();
		project.changeTemplate(template,
				Style.parseStyleDefinition(path.resolve("style_options.json")));
		touch();
		updatePreview();
		updateStyleEditor();
	}

	@FXML
	void defaultTemplate() {
		project.changeTemplate(Template.defaultTemplate(), Style.defaultStyle());
		updatePreview();
		updateStyleEditor();
		touch();
	}

	private void centerDialog(Dialog<?> dialog) {
		double stageCenterX = stage.getX() + stage.getWidth() / 2;
		double stageCenterY = stage.getY() + stage.getHeight() / 2;

		dialog.widthProperty().addListener(new ChangeListener<Number>() {
			@Override
			public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
				dialog.setX(stageCenterX - dialog.getWidth() / 2);
			}
		});
		dialog.heightProperty().addListener(new ChangeListener<Number>() {
			@Override
			public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
				dialog.setY(stageCenterY - dialog.getHeight() / 2);
			}
		});
	}

}
