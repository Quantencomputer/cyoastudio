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

import cyoastudio.*;
import cyoastudio.data.*;
import cyoastudio.io.*;
import cyoastudio.io.HtmlImageExporter.OutputFormat;
import cyoastudio.io.ProjectSerializer.ImageType;
import cyoastudio.templating.*;
import cyoastudio.templating.ProjectConverter.Bounds;
import javafx.beans.value.*;
import javafx.collections.*;
import javafx.concurrent.*;
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
	@FXML
	private ProgressBar previewProgressBar;

	private Stage stage;
	private Project project;
	private Path saveLocation;
	private static boolean dirty = false;
	private Section selectedSection;
	private Option selectedOption;
	private StyleEditor editor;
	private String currentElementId = "";

	private Thread backgroundRenderer;

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
		project = new Project();

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
					partialPreview();
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

		preview.getEngine().getLoadWorker().stateProperty().addListener((ov, oldState, newState) -> {
			if (newState == Worker.State.SUCCEEDED && !getCurrentElementId().isEmpty()) {
				try {
					preview.getEngine()
							.executeScript(
									"document.getElementById('" + getCurrentElementId() + "').scrollIntoView();");
				} catch (Exception e) {
					logger.error("Error while scrolling to selected preview element", e);
				}
			}
		});

		editor = new StyleEditor();
		styleTab.setContent(editor);

		cleanUp();
	}

	private void editSection(Section section) {
		selectedSection = section;
		currentElementId = "section-" + sectionObsList.indexOf(section);

		refreshOptionList();
		SectionEditor editor = new SectionEditor(selectedSection);
		editor.setOnNameChange(() -> {
			sectionList.refresh();
		});
		contentPane.setCenter(editor);
	}

	private void editOption(Option option) {
		selectedOption = option;
		currentElementId = "option-" + sectionObsList.indexOf(selectedSection) + "-" + optionObsList.indexOf(option);

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
		try {
			Application.getDatastorage().close();
		} catch (IOException e) {
			showError("Could not clean up working directory", e);
		}
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
		directoryChooser.setInitialDirectory(Preferences.getPath("lastDir").toFile());
		directoryChooser.setTitle("Image export folder");

		File selected = directoryChooser.showDialog(stage);
		if (selected != null) {
			try {
				Preferences.setPath("lastDir", selected.toPath());

				setDisable(true);
				String prefix = project.getTitle();
				HtmlImageExporter.convert(project, selected.toPath(), prefix, OutputFormat.IMAGE, error -> {
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
		fileChooser.setInitialDirectory(Preferences.getPath("lastDir").toFile());
		fileChooser.setTitle("Export HTML");
		fileChooser.getExtensionFilters().addAll(
				new ExtensionFilter("HTML files", "*.html", ".htm"),
				new ExtensionFilter("All files", "*"));
		File selected = fileChooser.showSaveDialog(stage);
		if (selected != null) {
			try {
				Preferences.setPath("lastDir", selected.toPath().getParent());
				String text = project.getTemplate().render(project, ImageType.BASE64);

				FileUtils.writeStringToFile(selected, text, Charset.forName("UTF-8"));
			} catch (Exception e) {
				showError("Error while exporting", e);
			}
		}
	}

	@FXML
	void exportSplitHTML() {
		DirectoryChooser directoryChooser = new DirectoryChooser();
		directoryChooser.setInitialDirectory(Preferences.getPath("lastDir").toFile());
		directoryChooser.setTitle("Image export folder");

		File selected = directoryChooser.showDialog(stage);
		if (selected != null) {
			try {
				Preferences.setPath("lastDir", selected.toPath());

				setDisable(true);
				String prefix = project.getTitle();
				HtmlImageExporter.convert(project, selected.toPath(), prefix, OutputFormat.HTML, error -> {
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
	void importJson() {
		if (!isUserSure("Import project")) {
			return;
		}

		FileChooser fileChooser = new FileChooser();
		fileChooser.setInitialDirectory(Preferences.getPath("lastDir").toFile());
		fileChooser.setTitle("Import project");
		fileChooser.getExtensionFilters().addAll(
				new ExtensionFilter("JSON file", "*.json"),
				new ExtensionFilter("All files", "*"));
		File selected = fileChooser.showOpenDialog(stage);
		if (selected != null) {
			try {
				Preferences.setPath("lastDir", selected.toPath().getParent());

				FileReader reader = new FileReader(selected);
				flushDataStorage();
				replaceProject(ProjectSerializer.fromReader(reader, ImageType.BASE64));
				reader.close();
			} catch (Exception e) {
				showError("Couldn't import file", e);
			}
		}
	}

	private void flushDataStorage() {
		try {
			Application.getDatastorage().flush();
		} catch (IOException e) {
			showError("Error while setting up working directory", e);
			exitProgram();
		}
	}

	private void replaceProject(Project newProject) {
		project = newProject;

		cleanUp();
	}

	@FXML
	void exportJson() {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setInitialDirectory(Preferences.getPath("lastDir").toFile());
		fileChooser.setTitle("Export plain text");
		fileChooser.getExtensionFilters().addAll(
				new ExtensionFilter("JSON files", "*.json"),
				new ExtensionFilter("All files", "*"));
		File selected = fileChooser.showSaveDialog(stage);
		if (selected != null) {
			try {
				Preferences.setPath("lastDir", selected.toPath().getParent());

				byte[] jsonData = ProjectSerializer.toBytes(project, ImageType.BASE64);
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

		flushDataStorage();
		replaceProject(new Project());
		saveLocation = null;
	}

	private void cleanUp() {
		selectedOption = null;
		selectedSection = null;
		currentElementId = "";

		refreshSectionList();
		refreshOptionList();

		contentPane.setCenter(null);
		updateStyleEditor();
		fullPreview();

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
		fileChooser.setInitialDirectory(Preferences.getPath("lastDir").toFile());
		fileChooser.setTitle("Open project");
		fileChooser.getExtensionFilters().addAll(
				new ExtensionFilter("CYOA Studio Project", "*.cyoa"),
				new ExtensionFilter("All files", "*"));
		File selected = fileChooser.showOpenDialog(stage);
		if (selected != null) {
			try {
				Preferences.setPath("lastDir", selected.toPath().getParent());

				flushDataStorage();
				replaceProject(ProjectSerializer.readFromZip(selected.toPath()));
				saveLocation = selected.toPath();
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
		Path backupLocation = null;
		try {
			if (Files.exists(saveLocation)) {
				String filename = saveLocation.getFileName().toString();
				int i = 1;
				do {
					String counter = (i == 1) ? "" : Integer.toString(i);
					backupLocation = saveLocation.getParent().resolve(filename + ".backup" + counter + ".cyoa");
					i++;
				} while (Files.exists(backupLocation));
				Files.move(saveLocation, backupLocation);
			}
			ProjectSerializer.writeToZip(project, saveLocation);
			dirty = false;
			if (Files.exists(backupLocation)) {
				Files.delete(backupLocation);
			}
		} catch (Exception e) {
			saveLocation = null;
			if (backupLocation != null)
				showError("Error while saving. A backup copy of the old project was created at "
						+ backupLocation.toString(), e);
			else
				showError("Error while saving.", e);
		}
	}

	private boolean selectSaveLocation() {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setInitialDirectory(Preferences.getPath("lastDir").toFile());
		fileChooser.setTitle("Save");
		fileChooser.getExtensionFilters().addAll(
				new ExtensionFilter("CYOA Studio Project", "*.cyoa"),
				new ExtensionFilter("All files", "*"));
		File selected = fileChooser.showSaveDialog(stage);
		if (selected == null) {
			return false;
		} else {
			Preferences.setPath("lastDir", selected.toPath().getParent());

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
		int targetPosition = selection.getSelectedIndex();
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
			targetIndex = optionObsList.size();
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
		optionList.getSelectionModel().clearSelection();
		if (selectedSection == null) {
			optionList.setItems(FXCollections.emptyObservableList());
		} else {
			optionObsList = FXCollections.observableList(selectedSection.getOptions());
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
		int targetPosition = selection.getSelectedIndex();
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

	@FXML
	private void partialPreview() {
		updatePreview(true);
	}

	private void updatePreview(boolean partial) {
		Task<String> renderTask = new Task<String>() {
			@Override
			protected String call() throws Exception {
				if (partial && selectedSection != null) {
					return project.getTemplate().render(project, true,
							new Bounds(sectionObsList.indexOf(selectedSection)), ImageType.REFERENCE);
				} else {
					return project.getTemplate().render(project, ImageType.REFERENCE);
				}
			}

			@Override
			protected void succeeded() {
				super.succeeded();
				try {
					preview.getEngine().loadContent(get());
					previewProgressBar.setProgress(100);
				} catch (Exception e) {
					logger.error("Error while updating preview", e);
				}
			}
		};
		previewProgressBar.setProgress(ProgressBar.INDETERMINATE_PROGRESS);
		preview.getEngine().loadContent("");

		if (backgroundRenderer != null) {
			backgroundRenderer.stop();
		}
		backgroundRenderer = new Thread(renderTask);
		backgroundRenderer.start();
	}

	@FXML
	private void fullPreview() {
		updatePreview(false);
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
		fileChooser.setInitialDirectory(Preferences.getPath("lastTemplateDir").toFile());
		fileChooser.setTitle("Import template");
		fileChooser.getExtensionFilters().addAll(
				new ExtensionFilter("CYOA Studio Template", "*.cyoatemplate"),
				new ExtensionFilter("All files", "*"));
		File selected = fileChooser.showOpenDialog(stage);
		if (selected != null) {
			try {
				Preferences.setPath("lastTemplateDir", selected.toPath().getParent());

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
		directoryChooser.setInitialDirectory(Preferences.getPath("lastTemplateImportDir").toFile());
		directoryChooser.setTitle("Import template");
		File selected = directoryChooser.showDialog(stage);
		if (selected != null) {
			try {
				Preferences.setPath("lastTemplateImportDir", selected.toPath());

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
		partialPreview();
		updateStyleEditor();
	}

	@FXML
	void templateFromCss() {
		FileChooser fileChooser = new FileChooser();
		fileChooser.setInitialDirectory(Preferences.getPath("lastTemplateImportDir").toFile());
		fileChooser.setTitle("Import template");
		fileChooser.getExtensionFilters().addAll(
				new ExtensionFilter("CSS files", "*.css"),
				new ExtensionFilter("All files", "*"));
		File selected = fileChooser.showOpenDialog(stage);
		if (selected != null) {
			try {
				Preferences.setPath("lastTemplateImportDir", selected.toPath().getParent());

				String pageSource = Template.defaultPageSource();
				String styleSource = FileUtils.readFileToString(selected, Charset.forName("UTF-8"));
				Map<String, Object> styleSettings = new HashMap<>();

				Template template = new Template(pageSource, styleSource);
				project.changeTemplate(template, styleSettings);
				touch();
				partialPreview();
				updateStyleEditor();
			} catch (Exception e) {
				showError("Could not load template", e);
			}
		}
	}

	@FXML
	void defaultTemplate() {
		project.changeTemplate(Template.defaultTemplate(), Style.defaultStyle());
		partialPreview();
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

			((TextArea) about.lookup("#infoArea")).setText(Application.getRuntimeDetails());
			((TextArea) about.lookup("#licenseArea")).setText(
					IOUtils.toString(getClass().getResourceAsStream("/cyoastudio/LICENSE"), Charset.forName("UTF-8")));

			Stage stage = new Stage();
			stage.setScene(new Scene(about));
			stage.show();
		} catch (IOException e) {
			showError("Error", e);
		}
	}

	public String getCurrentElementId() {
		return currentElementId;
	}

	@FXML
	void showOptions() {
		new OptionsWindow();
	}
}
