package cyoastudio.gui;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

import org.apache.commons.io.*;
import org.controlsfx.dialog.ExceptionDialog;
import org.slf4j.*;
import org.zeroturnaround.zip.ZipUtil;

import cyoastudio.Application;
import cyoastudio.data.*;
import cyoastudio.io.*;
import cyoastudio.templating.*;
import javafx.beans.value.*;
import javafx.collections.*;
import javafx.fxml.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.BorderPane;
import javafx.scene.web.WebView;
import javafx.stage.*;
import javafx.stage.FileChooser.ExtensionFilter;

public class MainWindow extends BorderPane {

	final Logger logger = LoggerFactory.getLogger(MainWindow.class);

	@FXML
	private BorderPane contentPane;
	@FXML
	private ListView<Section> sectionList;
	private ObservableList<Section> sectionObsList;
	@FXML
	private ListView<Option> optionList;
	private ObservableList<Option> optionObsList;
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
	@FXML
	private TextField imageHeightField;

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
		MultipleSelectionModel<Section> sectionModel = sectionList.getSelectionModel();
		MultipleSelectionModel<Option> optionModel = optionList.getSelectionModel();

		sectionModel.setSelectionMode(SelectionMode.MULTIPLE);
		sectionList.setCellFactory(list -> {
			return new DragDropCell<Section>() {
				@Override
				protected String stringify(Section value) {
					if (value.getTitle().trim().isEmpty())
						return "[Unnamed section]";
					return value.getTitle();
				}

				@Override
				protected boolean receive(DragDropInfo info) {
					if (isSourceOf(info)) {
						// Ordering sections
						Section targetItem = getItem();

						ArrayList<Section> movingSections = new ArrayList<>(sectionModel.getSelectedItems());
						sectionObsList.removeAll(movingSections);

						movingSections.removeIf(s -> s == null);

						int target = sectionObsList.indexOf(targetItem) + 1;
						sectionObsList.addAll(target, movingSections);

						touch();
						return true;
					} else {
						// Moving options between sections
						ArrayList<Option> movingOptions = new ArrayList<>(optionModel.getSelectedItems());
						optionObsList.removeAll(movingOptions);
						getItem().getOptions().addAll(movingOptions);

						touch();
						return true;
					}
				}

				@Override
				protected String getIdentifier() {
					return "section";
				}

				@Override
				protected boolean isCompatible(DragDropInfo info) {
					return (info.getSource().equals("section") && !sectionModel.isSelected(getIndex()))
							|| info.getSource().equals("option");
				}
			};
		});
		sectionModel.selectedItemProperty().addListener(new ChangeListener<Section>() {
			@Override
			public void changed(ObservableValue<? extends Section> observable, Section oldValue, Section newValue) {
				if (newValue != null) {
					editSection(newValue);
				}
			}
		});

		optionModel.setSelectionMode(SelectionMode.MULTIPLE);
		optionList.setCellFactory(list -> {
			return new DragDropCell<Option>() {
				@Override
				protected String stringify(Option value) {
					if (value.getTitle().trim().isEmpty())
						return "[Unnamed option]";
					return value.getTitle();
				}

				@Override
				protected boolean receive(DragDropInfo info) {
					if (isSourceOf(info)) {
						Option targetItem = getItem();

						ArrayList<Option> movingOptions = new ArrayList<>(optionModel.getSelectedItems());
						optionObsList.removeAll(movingOptions);

						movingOptions.removeIf(o -> o == null);

						int target = optionObsList.indexOf(targetItem) + 1;
						optionObsList.addAll(target, movingOptions);

						touch();
						return true;
					} else {
						throw new RuntimeException("Invalid drag'n'drop received");
					}
				}

				@Override
				protected String getIdentifier() {
					return "option";
				}

				@Override
				protected boolean isCompatible(DragDropInfo info) {
					return (info.getSource().equals("option") && !optionModel.isSelected(getIndex()));
				}
			};
		});
		optionModel.selectedItemProperty().addListener(new ChangeListener<Option>() {
			@Override
			public void changed(ObservableValue<? extends Option> observable, Option oldValue, Option newValue) {
				if (newValue != null) {
					editOption(newValue);
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

		imageHeightField.textProperty().addListener(new ChangeListener<String>() {
			@Override
			public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
				if (!newValue.matches("\\d*")) {
					imageHeightField.setText(newValue.replaceAll("[^\\d]", ""));
				} else if (newValue.isEmpty()) {
					imageHeightField.setText("0");
				} else {
					int value = Integer.valueOf(newValue);
					if (value > 999999) {
						value = 999999;
						imageHeightField.setText(Integer.toString(value));
					} else {
						project.getSettings().setMaxImageHeight(value);
					}
				}
			}
		});

		editor = new StyleEditor();
		styleTab.setContent(editor);

		cleanUp();
	}

	private void editSection(Section section) {
		selectedSection = section;

		refreshOptionList();
		SectionEditor editor = new SectionEditor(selectedSection);
		editor.setOnNameChange(() -> {
			sectionList.refresh();
		});
		contentPane.setCenter(editor);

		optionList.getSelectionModel().clearSelection();
	}

	private void editOption(Option option) {
		selectedOption = option;

		OptionEditor editor = new OptionEditor(selectedOption, selectedSection);
		editor.setOnNameChange(() -> {
			optionList.refresh();
		});
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
		DirectoryChooser directoryChooser = new DirectoryChooser();
		try {
			if (Application.preferences.get("lastDir", null) != null
					&& Files.isDirectory(Paths.get(Application.preferences.get("lastDir", null))))
				directoryChooser.setInitialDirectory(Paths.get(Application.preferences.get("lastDir", "")).toFile());
		} catch (Exception e) {
			logger.warn("Coulnd't access preferences", e);
		}
		directoryChooser.setTitle("Image export folder");

		File selected = directoryChooser.showDialog(stage);
		if (selected != null) {
			try {
				Application.preferences.put("lastDir", selected.toPath().getParent().toAbsolutePath().toString());

				setDisable(true);
				String prefix = project.getTitle();
				HtmlImageExporter.convert(project, selected.toPath(), prefix, error -> {
					if (error != null) {
						showError(error);
					}
					setDisable(false);
				});
			} catch (Exception e) {
				showError("Error while exporting", e);
			}
		}
	}

	@FXML
	void exportHTML() {
		FileChooser fileChooser = new FileChooser();
		try {
			if (Application.preferences.get("lastDir", null) != null
					&& Files.isDirectory(Paths.get(Application.preferences.get("lastDir", null))))
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
	void importJson() {
		if (!isUserSure("Import project")) {
			return;
		}

		FileChooser fileChooser = new FileChooser();
		try {
			if (Application.preferences.get("lastDir", null) != null
					&& Files.isDirectory(Paths.get(Application.preferences.get("lastDir", null))))
				fileChooser.setInitialDirectory(Paths.get(Application.preferences.get("lastDir", "")).toFile());
		} catch (Exception e) {
			logger.warn("Coulnd't access preferences", e);
		}
		fileChooser.setTitle("Import project");
		fileChooser.getExtensionFilters().addAll(
				new ExtensionFilter("JSON file", "*.json"),
				new ExtensionFilter("All files", "*"));
		File selected = fileChooser.showOpenDialog(stage);
		if (selected != null) {
			try {
				Application.preferences.put("lastDir", selected.toPath().getParent().toAbsolutePath().toString());

				FileReader reader = new FileReader(selected);
				project = ProjectSerializer.fromReader(reader);
				reader.close();

				saveLocation = selected.toPath();
				cleanUp();
			} catch (Exception e) {
				showError("Couldn't import file", e);
			}
		}
	}

	@FXML
	void exportJson() {
		FileChooser fileChooser = new FileChooser();
		try {
			if (Application.preferences.get("lastDir", null) != null
					&& Files.isDirectory(Paths.get(Application.preferences.get("lastDir", null))))
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
		selectedOption = null;
		selectedSection = null;
		refreshSectionList();
		refreshOptionList();
		updateStyleEditor();
		updatePreview();
		dirty = false;
		projectTitleBox.setText(project.getTitle());
		imageHeightField.setText(Integer.toString(project.getSettings().getMaxImageHeight()));
	}

	@FXML
	void openProject() {
		if (!isUserSure("Open project")) {
			return;
		}

		FileChooser fileChooser = new FileChooser();
		try {
			if (Application.preferences.get("lastDir", null) != null
					&& Files.isDirectory(Paths.get(Application.preferences.get("lastDir", null))))
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
			} catch (Exception e) {
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
		} catch (Exception e) {
			saveLocation = null;
			showError("Error while saving.", e);
		}
	}

	private boolean selectSaveLocation() {
		FileChooser fileChooser = new FileChooser();
		try {
			if (Application.preferences.get("lastDir", null) != null
					&& Files.isDirectory(Paths.get(Application.preferences.get("lastDir", null))))
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
		int targetIndex;
		MultipleSelectionModel<Section> selectionModel = sectionList.getSelectionModel();
		if (selectionModel.isEmpty()) {
			targetIndex = sectionObsList.size();
		} else {
			targetIndex = selectionModel.getSelectedIndex() + 1;
		}
		project.getSections().add(targetIndex, new Section());
		refreshSectionList();

		// Make the new entry editable
		sectionList.layout();
		sectionList.scrollTo(targetIndex);
		selectionModel.clearAndSelect(targetIndex);

		SectionEditor editor = (SectionEditor) contentPane.getCenter();
		editor.focusNameField();
	}

	private void refreshSectionList() {
		sectionObsList = FXCollections.observableList(project.getSections());
		sectionList.setItems(sectionObsList);
	}

	@FXML
	void deleteSections() {
		Alert a = new Alert(AlertType.CONFIRMATION);
		a.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO, ButtonType.CANCEL);
		a.setTitle("Are you sure?");
		a.setHeaderText("Are you sure?");
		a.setContentText("Are you sure you want to delete the selected sections?");
		Optional<ButtonType> result = a.showAndWait();
		if (result.get() != ButtonType.YES)
			return;

		touch();
		sectionObsList.removeAll(sectionList.getSelectionModel().getSelectedItems());
	}

	@FXML
	void duplicateSections() {
		MultipleSelectionModel<Section> selection = sectionList.getSelectionModel();
		List<Integer> indices = selection.getSelectedIndices();
		int targetPosition = indices.get(indices.size() - 1);
		List<Section> copies = selection.getSelectedItems().parallelStream()
				.map(s -> ProjectSerializer.deepCopy(s)).collect(Collectors.toList());
		sectionObsList.addAll(targetPosition + 1, copies);
		touch();
	}

	@FXML
	void sortSections() {
		sectionObsList.sort((a, b) -> a.getTitle().compareTo(b.getTitle()));
		touch();
	}

	@FXML
	void newOption() {
		touch();
		int targetIndex;
		MultipleSelectionModel<Option> selectionModel = optionList.getSelectionModel();
		if (selectionModel.isEmpty()) {
			targetIndex = sectionObsList.size();
		} else {
			targetIndex = selectionModel.getSelectedIndex() + 1;
		}
		if (selectedSection != null) {
			selectedSection.getOptions().add(targetIndex, new Option());
			refreshOptionList();

			// Make the new entry editable
			optionList.layout();
			optionList.scrollTo(targetIndex);
			selectionModel.clearAndSelect(targetIndex);

			OptionEditor editor = (OptionEditor) contentPane.getCenter();
			editor.focusNameField();
		}
	}

	private void refreshOptionList() {
		Section cur = selectedSection;
		if (cur == null) {
			optionList.setItems(FXCollections.emptyObservableList());
		} else {
			optionObsList = FXCollections.observableList(cur.getOptions());
			optionList.setItems(optionObsList);
		}
	}

	@FXML
	void deleteOptions() {
		Alert a = new Alert(AlertType.CONFIRMATION);
		a.getButtonTypes().setAll(ButtonType.YES, ButtonType.NO, ButtonType.CANCEL);
		a.setTitle("Are you sure?");
		a.setHeaderText("Are you sure?");
		a.setContentText("Are you sure you want to delete the selected options?");
		Optional<ButtonType> result = a.showAndWait();
		if (result.get() != ButtonType.YES)
			return;

		touch();
		if (selectedSection != null) {
			optionObsList.removeAll(optionList.getSelectionModel().getSelectedItems());
		}
	}

	@FXML
	void duplicateOptions() {
		MultipleSelectionModel<Option> selection = optionList.getSelectionModel();
		List<Integer> indices = selection.getSelectedIndices();
		int targetPosition = indices.get(indices.size() - 1);
		List<Option> copies = selection.getSelectedItems().parallelStream()
				.map(o -> ProjectSerializer.deepCopy(o)).collect(Collectors.toList());
		optionObsList.addAll(targetPosition + 1, copies);
		touch();
	}

	@FXML
	void sortOptions() {
		optionObsList.sort((a, b) -> a.getTitle().compareTo(b.getTitle()));
		touch();
	}

	private void updatePreview() {
		String website = project.getTemplate().render(project);
		preview.getEngine().loadContent(website);
	}

	private void updateStyleEditor() {
		editor.editStyle(project);
	}

	private void showError(String message) {
		logger.error(message);

		Alert a = new Alert(AlertType.ERROR);
		a.setTitle("Error");
		a.setContentText(message);
		a.show();
	}

	private void showError(String message, Exception ex) {
		logger.error(message, ex);

		ExceptionDialog exceptionDialog = new ExceptionDialog(ex);
		exceptionDialog.setTitle("Error");
		exceptionDialog.setHeaderText(message);
		exceptionDialog.show();
	}

	@FXML
	void templateFromFile() {
		FileChooser fileChooser = new FileChooser();
		try {
			if (Application.preferences.get("lastDir", null) != null
					&& Files.isDirectory(Paths.get(Application.preferences.get("lastDir", null))))
				fileChooser.setInitialDirectory(Paths.get(Application.preferences.get("lastDir", "")).toFile());
		} catch (Exception e) {
			logger.warn("Coulnd't access preferences", e);
		}
		fileChooser.setTitle("Import template");
		fileChooser.getExtensionFilters().addAll(
				new ExtensionFilter("CYOA Studio Template", "*.cyoatemplate"),
				new ExtensionFilter("All files", "*"));
		File selected = fileChooser.showOpenDialog(stage);
		if (selected != null) {
			try {
				Application.preferences.put("lastDir", selected.toPath().getParent().toAbsolutePath().toString());
				Path tempDirectory = Files.createTempDirectory(null);
				ZipUtil.unpack(selected, tempDirectory.toFile());

				loadTemplate(tempDirectory);

				FileUtils.deleteDirectory(tempDirectory.toFile());
			} catch (Exception e) {
				showError("Could not load template", e);
			}
		}
	}

	@FXML
	void templateFromFolder() {
		DirectoryChooser directoryChooser = new DirectoryChooser();
		try {
			if (Application.preferences.get("lastDir", null) != null
					&& Files.isDirectory(Paths.get(Application.preferences.get("lastDir", null))))
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
			} catch (Exception e) {
				showError("Could not load template", e);
			}
		}
	}

	private void loadTemplate(Path path) throws IOException {
		Path pagePath = path.resolve("page.html.mustache");
		String pageSource;
		if (Files.isRegularFile(pagePath)) {
			pageSource = FileUtils.readFileToString(pagePath.toFile(), Charset.forName("UTF-8"));
		} else {
			logger.info("No page template found, using default");
			pageSource = Template.defaultPageSource();
		}

		Path stylePath = path.resolve("style.css.mustache");
		String styleSource;
		if (Files.isRegularFile(stylePath)) {
			styleSource = FileUtils.readFileToString(stylePath.toFile(), Charset.forName("UTF-8"));
		} else {
			throw new RuntimeException("No style template found");
		}

		Path styleSettingsPath = path.resolve("style_options.json");
		Map<String, Object> styleSettings;
		if (Files.isRegularFile(styleSettingsPath)) {
			styleSettings = Style.parseStyleDefinition(styleSettingsPath);
		} else {
			styleSettings = new HashMap<>();
		}

		Template template = new Template(pageSource, styleSource);
		project.changeTemplate(template, styleSettings);
		touch();
		updatePreview();
		updateStyleEditor();
	}

	@FXML
	void templateFromCss() {
		FileChooser fileChooser = new FileChooser();
		try {
			if (Application.preferences.get("lastDir", null) != null
					&& Files.isDirectory(Paths.get(Application.preferences.get("lastDir", null))))
				fileChooser.setInitialDirectory(Paths.get(Application.preferences.get("lastDir", "")).toFile());
		} catch (Exception e) {
			logger.warn("Coulnd't access preferences", e);
		}
		fileChooser.setTitle("Import template");
		fileChooser.getExtensionFilters().addAll(
				new ExtensionFilter("CSS files", "*.css"),
				new ExtensionFilter("All files", "*"));
		File selected = fileChooser.showOpenDialog(stage);
		if (selected != null) {
			try {
				String pageSource = Template.defaultPageSource();
				String styleSource = FileUtils.readFileToString(selected, Charset.forName("UTF-8"));
				Map<String, Object> styleSettings = new HashMap<>();

				Template template = new Template(pageSource, styleSource);
				project.changeTemplate(template, styleSettings);
				touch();
				updatePreview();
				updateStyleEditor();
			} catch (Exception e) {
				showError("Could not load template", e);
			}
		}
	}

	@FXML
	void defaultTemplate() {
		project.changeTemplate(Template.defaultTemplate(), Style.defaultStyle());
		updatePreview();
		updateStyleEditor();
		touch();
	}

	@FXML
	void openHelp() {
		WebView webView = new WebView();
		Stage stage = new Stage();
		stage.setScene(new Scene(webView));
		webView.getEngine().load("https://quantencomputer.github.io/cyoastudio/manual.html");
		stage.show();
	}

	@FXML
	void about() {
		try {
			Parent about = FXMLLoader.load(getClass().getResource("About.fxml"));

			((Label) about.lookup("#versionLabel")).setText(Application.getVersion().toString());
			((TextArea) about.lookup("#licenseArea")).setText(
					IOUtils.toString(getClass().getResourceAsStream("/cyoastudio/LICENSE"), Charset.forName("UTF-8")));

			Stage stage = new Stage();
			stage.setScene(new Scene(about));
			stage.show();
		} catch (IOException e) {
			showError("Error", e);
		}
	}

}
