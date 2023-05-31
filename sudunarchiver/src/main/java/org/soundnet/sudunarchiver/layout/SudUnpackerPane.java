package org.soundnet.sudunarchiver.layout;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser.ExtensionFilter;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.stage.FileChooser;
import javafx.stage.DirectoryChooser;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.application.Platform;
import javafx.beans.property.SimpleListProperty;

import java.io.File;
import java.util.List;
import java.util.ArrayList;

import org.controlsfx.control.*;
import org.soundnet.sudunarchiver.SudUnpackerControl.SudFileProcessTask;
import org.soundnet.sudunarchiver.SudUnpackerParams;

import net.synedra.validatorfx.Severity;
import net.synedra.validatorfx.ValidationMessage;
import net.synedra.validatorfx.ValidationResult;
import net.synedra.validatorfx.Validator;

/**
 * 
 * The main pane with controls for users. 
 * 
 * @author Jamie Macaulay
 *
 */
public class SudUnpackerPane extends BorderPane {

	public static final double DEFAULT_SPACING = 5.;//pixel 

	public static final int DEFAULT_IKON_SIZE = 22;//pixel 

	private Validator validator = new Validator();

	private TextField filesTextFiles;

	/**
	 * Toggle switch for search for sub folders. 
	 */
	private ToggleSwitch subFolderToggle;

	/**
	 * Field which shows where files will be saved to to. 
	 */
	private TextField saveTextFiles;


	/**
	 * Toggle swtich to save wav files 
	 */
	private ToggleSwitch wavSaveToggle;

	/**
	 * Toggle switch to save click files. 
	 */
	private ToggleSwitch clkSaveToggle;

	/**
	 * Toggle switch to save csv files. 
	 */
	private ToggleSwitch csvSaveToggle;

	/**
	 * Toggle for selecting whether xml files hsould be saved. 
	 */
	private ToggleSwitch xmlSaveToggle;

	/**
	 * Showsthe sud decompression. 
	 */
	private TaskProgressView<SudFileProcessTask> progressView;

	/**
	 * Starts the sud decompression running
	 */
	private Button runButton;

	/**
	 * The spinner for setting how many threads to use. 
	 */
	private Spinner<Integer> threadSpinner;

	/**
	 * A file chooser
	 */
	private FileChooser fileChooser = new FileChooser();

	/**
	 * Folder chooser
	 */
	private DirectoryChooser folderChooser = new DirectoryChooser();

	/**
	 * Folder save chooser
	 */
	private DirectoryChooser folderSaveChooser = new DirectoryChooser();

	/**
	 * Progress indicator. 
	 */
	private ProgressIndicator progressIndicator;

	/**
	 * The current list of sud files.
	 */
	private SimpleListProperty<File> sudFiles = new SimpleListProperty<>(FXCollections.observableArrayList());


	/**
	 * The current folder of sud files. Can be null.
	 */
	private File currentFolder = null;

	/**
	 * The save folder. 
	 */
	private File saveFolder;

	/**
	 * Reference to the view
	 */
	private SudUnpackerView sudUnpackerView;

	private boolean isRunning = false; 




	public SudUnpackerPane(SudUnpackerView sudUnpackerView) {
		this.sudUnpackerView=sudUnpackerView; 
		this.setCenter(createSudPane());
	}

	public Pane createSudPane() {

		/**************Import Files Section ***************/

		Label fileLabel = new Label("Select SUD files"); 
		setTitleLabel(fileLabel);  

		progressIndicator = new ProgressIndicator();
		progressIndicator.setPrefSize(20, 20);
		progressIndicator.setVisible(false);

		HBox fileLabelBox = new HBox(); 
		fileLabelBox.setSpacing(5); 
		fileLabelBox.setAlignment(Pos.CENTER_LEFT);
		fileLabelBox.getChildren().addAll(fileLabel, progressIndicator); 

		filesTextFiles = new TextField(); 
		filesTextFiles.setEditable(false);
		validator.createCheck()
		.dependsOn("files_selected", sudFiles.sizeProperty())
		.withMethod(c -> {
			Integer size = c.get("files_selected");
			if (size.intValue()<=0) {
				if (currentFolder!=null) {
					c.error("The current folder contains no sud files");
				}
				else {
					c.error("You must select some sud files");
				}
			}
		})
		.decorates(filesTextFiles)
		.immediate();

		Button filesButton = new Button(); 
		filesButton.setGraphic(SudIkonDude.createPamIcon("fltral-document-copy-48", DEFAULT_IKON_SIZE));


		fileChooser.setTitle("Open SUD File");
		fileChooser.getExtensionFilters().addAll(
				new ExtensionFilter("Sud Files", "*.sud"));
		filesButton.setOnAction((action)->{
			List<File> sudFiles = fileChooser.showOpenMultipleDialog(sudUnpackerView.getStage());
			if (sudFiles != null) {
				setSudTextField(null, sudFiles);
			}
		});


		Button folderButton = new Button(); 
		folderButton.setGraphic(SudIkonDude.createPamIcon("fltral-folder-open-20", DEFAULT_IKON_SIZE));
		folderButton.setOnAction((action)->{
			folderChooser.setTitle("Open SUD Folder");
			File sudFolder = folderChooser.showDialog(sudUnpackerView.getStage());
			if (sudFolder != null) {
				currentFolder=sudFolder; 
				updateSudFolder(); 
			}
		});

		HBox filesHBox = new HBox(); 
		filesHBox.setSpacing(DEFAULT_SPACING);
		filesHBox.setAlignment(Pos.CENTER_LEFT);
		HBox.setHgrow(filesTextFiles, Priority.ALWAYS);

		filesTextFiles.prefHeightProperty().bind(filesButton.heightProperty());

		filesHBox.getChildren().addAll(filesTextFiles, folderButton, filesButton); 

		HBox subFolderHBox = new HBox(); 
		subFolderHBox.setSpacing(DEFAULT_SPACING);
		subFolderHBox.setAlignment(Pos.CENTER_LEFT);
		subFolderToggle = new ToggleSwitch();
		subFolderToggle.selectedProperty().addListener((obsVal, oldval, newVal)->{
			updateSudFolder(); 
		});

		subFolderHBox.getChildren().addAll(subFolderToggle, new Label("Sub folders")); 

		/**************Save Files Section ***************/

		Label saveLabel = new Label("Save to"); 
		setTitleLabel(saveLabel);

		saveTextFiles = new TextField(); 
		saveTextFiles.setText("Files will be saved to same location");
		saveTextFiles.setEditable(false);

		Button folderSaveButton = new Button(); 
		folderSaveButton.setGraphic(SudIkonDude.createPamIcon("fltral-folder-open-20", DEFAULT_IKON_SIZE));
		folderSaveButton.setOnAction((action)->{
			folderSaveChooser.setTitle("Open SUD Folder");
			saveFolder = folderSaveChooser.showDialog(sudUnpackerView.getStage());
			updateSaveFolder(); 
		

		});

		HBox saveHBox = new HBox(); 
		saveHBox.setSpacing(DEFAULT_SPACING);
		saveHBox.setAlignment(Pos.CENTER_LEFT);
		HBox.setHgrow(saveTextFiles, Priority.ALWAYS);

		Region blankSpace = new Region(); 
		blankSpace.prefWidthProperty().bind(filesButton.widthProperty());

		saveHBox.setPadding(getInsets());

		saveTextFiles.prefHeightProperty().bind(folderSaveButton.heightProperty());

		saveHBox.getChildren().addAll(saveTextFiles, folderSaveButton, blankSpace); 

		/**************Decompress Section ***************/
		
		Label decompressLabel = new Label("Decompress"); 
		setTitleLabel(decompressLabel);

		wavSaveToggle = new ToggleSwitch("Wav files"); 
		wavSaveToggle.selectedProperty().addListener((obsVal, oldVal, newVal)->{
			enableControls();
		}); 

		clkSaveToggle = new ToggleSwitch("Click files"); 
		clkSaveToggle.selectedProperty().addListener((obsVal, oldVal, newVal)->{
			enableControls();
		}); 

		csvSaveToggle = new ToggleSwitch("CSV files"); 
		csvSaveToggle.selectedProperty().addListener((obsVal, oldVal, newVal)->{
			enableControls();
		}); 

		xmlSaveToggle = new ToggleSwitch("XML files"); 
		xmlSaveToggle.selectedProperty().addListener((obsVal, oldVal, newVal)->{
			enableControls();
		}); 

		/**************Progress  Section ***************/

		Label runLabel = new Label("Run"); 
		setTitleLabel(runLabel);

		HBox runHBox = new HBox(); 
		runHBox.setSpacing(5);
		runHBox.setAlignment(Pos.CENTER_LEFT);

		runButton = new Button(); 
		runButton.setGraphic(SudIkonDude.createPamIcon("fltfmz-play-20", DEFAULT_IKON_SIZE));
		runButton.setOnAction((action)->{
			if (!isRunning) {
				if (!checkRunErrors()) {
					runButton.setGraphic(SudIkonDude.createPamIcon("fltfmz-stop-20", DEFAULT_IKON_SIZE));
					isRunning=true;
					this.sudUnpackerView.run(); 
				}
			}
			else {
				runButton.setGraphic(SudIkonDude.createPamIcon("fltfmz-play-20", DEFAULT_IKON_SIZE));
				isRunning=false;
				this.sudUnpackerView.stop(); 
			}
		});

		threadSpinner = new Spinner<Integer>(1, 8, 1, 1); 
		threadSpinner.getStyleClass().add( Spinner.STYLE_CLASS_SPLIT_ARROWS_HORIZONTAL);
		threadSpinner.setPrefWidth(70);
		threadSpinner.prefHeightProperty().bind(runButton.heightProperty());
		//threadSpinner.getValueFactory().setValue(1);

		validator.createCheck()
		.dependsOn("thread_count", threadSpinner.valueProperty())
		.withMethod(c -> {
			Integer threadCount = c.get("thread_count");
			if (threadCount.intValue()>3) {
				c.warn("This might melt your computer...");
			}
		})
		.decorates(threadSpinner)
		.immediate();


		runHBox.getChildren().addAll(new Label("No. threads"), threadSpinner); 

		BorderPane runBorderPane = new BorderPane();
		runBorderPane.setLeft(runButton);
		runBorderPane.setRight(runHBox);

		progressView = new TaskProgressView<SudFileProcessTask>(); 
		progressView.setRetainTasks(true);

		//enable the controls. 
		enableControls();

		/**************Overall Layout**************/

		VBox vBox = new VBox(); 
		vBox.setSpacing(5);
		vBox.setPadding(new Insets(DEFAULT_SPACING,DEFAULT_SPACING,DEFAULT_SPACING,DEFAULT_SPACING));
		vBox.getChildren().addAll(fileLabelBox, filesHBox, subFolderHBox, decompressLabel, wavSaveToggle, clkSaveToggle,
				csvSaveToggle, xmlSaveToggle, saveLabel, saveHBox, runLabel, runBorderPane, progressView); 


		return vBox;
	}


	/**
	 * Called whenever the save folder is upodated. 
	 */
	private void updateSaveFolder() {
		if (saveFolder != null) {
			saveTextFiles.setText(saveFolder.getAbsolutePath());
		}
		else {
			saveTextFiles.setText("Files will be saved to same location");
		}	
	}

	/**
	 * Set the text field so it shows the number of files. 
	 * @param sudFolder - the sud folder.
	 * @param sudFiles - the sud files. 
	 */
	private void setSudTextField(File sudFolder, List<File> sudFiles) {
		String message; 

		if (sudFolder!=null) {
			message = sudFiles.size() + " sud files  -  " + sudFolder; 

		}
		else {
			message = sudFiles.size() + " sud files \n";
			for (int i=0; i<sudFiles.size(); i++) {
				message+= sudFiles.get(i).getName() + "\n"; 
			}
		}

		this.filesTextFiles.setText(message);
		filesTextFiles.setTooltip(new Tooltip(message));
	}


	private List<File> getSudFileList(boolean selected) {
		return listSud(this.currentFolder, selected);
	}

	/**
	 * Enable the controls. 
	 */
	private void enableControls() {
		runButton.setDisable(true);
		if (wavSaveToggle.isSelected() || clkSaveToggle.isSelected() || 
				csvSaveToggle.isSelected() || xmlSaveToggle.isSelected()) {
			runButton.setDisable(false);
		}
	}

	/**
	 * Set the title label. 
	 * @param label - the label. 
	 */
	private void setTitleLabel(Label label) {
		label.setFont(Font.font(null, FontWeight.BOLD, 18));
	}; 


	/**
	 * Task for loading a list of sud files. 
	 * @author Jamie Macaulay
	 *
	 */
	class SudFolderTasK extends Task<Integer>{

		/**
		 * Use sub folders. 
		 */
		private boolean isSubFolder;

		/**
		 * The current sub folder. 
		 */
		private File subFolder;


		public SudFolderTasK(File folder, boolean isSubFolder) {
			this.isSubFolder = isSubFolder; 
			this.subFolder =folder; 
		}

		@Override protected Integer call() throws Exception {
			int iterations = 0;

			try {
				List<File> folderSudFiles = getSudFileList(subFolderToggle.isSelected());

				if (folderSudFiles!=null) {
					Platform.runLater(()->{
						sudFiles.clear();
						sudFiles.addAll(folderSudFiles); 
					});
				}

			}
			catch(Exception e) {
				e.printStackTrace();
			}

			return iterations;
		}

		@Override 
		protected void succeeded() {
			super.succeeded();
			sudFilesDone();
			updateMessage("Sud File List succeeded!");
		}

		@Override 
		protected void cancelled() {
			super.cancelled();
			sudFilesDone();
			updateMessage("Sud File List cancelled!");
		}

		@Override 
		protected void failed() {
			super.failed();
			sudFilesDone();
			updateMessage("Sud File List failed!");
		}

		private void sudFilesDone() {
			showProgressIndicator(false); 
			setSudTextField(subFolder, sudFiles);
		}

	};

	/**
	 * Show the progress indicator. 
	 * @param show - true to show progress indicator. False to hide progress indicator. 
	 */
	public void showProgressIndicator(boolean show) {
		progressIndicator.setProgress(-1);
		progressIndicator.setVisible(show);
	}

	/**
	 * List all sud files in a folder. 
	 * @param directoryName - the name of the folder. 
	 * @param subFolder - true to get files from sud folders
	 * @return a list of all sud files. 
	 */
	public List<File> listSud(File directory, boolean subFolder) {

		List<File> sudFiles = new ArrayList<File>(); 

		// Get all files from a directory.
		File[] fList = directory.listFiles();
		if(fList != null)
			for (File file : fList) {      
				if (file.isFile() && isSudFile(file)) {
					sudFiles.add(file);
				} else if (file.isDirectory() && subFolder) {
					sudFiles.addAll(listSud(file, subFolder));
				}
			}

		return sudFiles;
	}

	/**
	 * Check whether a file is a sud file. 
	 * @param file - the file to check.
	 * @return true if the file is a sud file. 
	 */
	private boolean isSudFile(File file) {
		return file.getName().endsWith(".sud"); 
	}
	
	private void updateSudFolder() {
		if (currentFolder != null) {

			showProgressIndicator(true);

			Thread th = new Thread(new SudFolderTasK(currentFolder, subFolderToggle.isSelected()));
			th.setDaemon(true);
			th.start();

		};
	}

	/**
	 * Update the parameters from the set user controls. 
	 * @param params - the parameters to update. Note this is not cloned within the function. 
	 * @return the updated parameters. 
	 */
	public SudUnpackerParams getParams(SudUnpackerParams params) {

		params.saveFolder = this.saveFolder; 
		
		params.nThreads = this.threadSpinner.getValue(); 
		
		params.subFolder = this.subFolderToggle.isSelected();
		
		params.unPackWav = this.wavSaveToggle.isSelected();
		params.unPackCSV = this.csvSaveToggle.isSelected();
		params.unPackXML = this.xmlSaveToggle.isSelected();
		params.unPackClicks = this.clkSaveToggle.isSelected();
		
		params.sudFiles = this.sudFiles.get(); 

		return params;
	}

	/**
	 * Update the controls to reflect the current paramters. 
	 * @param params - the params to set
	 */
	public void setParams(SudUnpackerParams params) {
		
		this.wavSaveToggle.setSelected(	params.unPackWav);
		this.csvSaveToggle.setSelected(	params.unPackCSV);
		this.xmlSaveToggle.setSelected(	params.unPackXML);
		this.clkSaveToggle.setSelected(	params.unPackClicks);
		
		this.threadSpinner.getValueFactory().setValue(	params.nThreads);; 

		this.subFolderToggle.setSelected(params.subFolder);
		
		this.sudFiles.set(FXCollections.observableList(params.sudFiles));
		
		//if the folder is null will do nothing. Note this will override file list if it has already been set. 
		this.currentFolder =  params.saveFolder; 
		updateSudFolder();
		
		this.saveFolder =  params.saveFolder; 
		this.updateSaveFolder(); 
	}

	/**
	 * Get the validator. 
	 * @return - the validator. 
	 */
	public Validator getValidator() {
		return this.validator;
	}
	
	/**
	 * Check if there are run errors and show a dialog if so. 
	 * @return true of there were errors, false if not. 
	 */
	private boolean checkRunErrors() {
		ValidationResult result = getValidator().getValidationResult(); 

		List<ValidationMessage> messages = result.getMessages(); 

		int errcount = 0; 
		String errorMessages = "";
		for (int i=0; i<messages.size(); i++) {
			if (messages.get(i).getSeverity().equals(Severity.ERROR)) {
				errorMessages +=	messages.get(i).getText(); 
				errcount++; 
			}
		}

		if (errcount>0) {
	        Alert a = new Alert(AlertType.ERROR);
	        a.setHeaderText("Cannot decompress SUD");
	        a.setContentText(errorMessages);
	        a.show();
	        return true;
		}
		
		return false; 
	}

	/**
	 * The task progress view. 
	 * @return the task progress view. 
	 */
	public TaskProgressView<SudFileProcessTask> getTaskView() {
		return this.progressView; 		
	}



}
