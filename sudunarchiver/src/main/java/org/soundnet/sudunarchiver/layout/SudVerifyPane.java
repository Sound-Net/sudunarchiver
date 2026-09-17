package org.soundnet.sudunarchiver.layout;

import java.io.File;
import java.util.List;

import org.controlsfx.control.ToggleSwitch;
import org.soundnet.sudunarchiver.verify.SudSpectrogramData;
import org.soundnet.sudunarchiver.verify.SudVerifyParams;
import org.soundnet.sudunarchiver.verify.SudVerifyReport;
import org.soundnet.sudunarchiver.verify.SudVerifyTask;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TabPane.TabClosingPolicy;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 * The data checking controls. This is the back of the flip pane which sits
 * below the file selection controls - the decompression controls are on the
 * front.
 * <p>
 * Each toggle switches on one check. The checks run over every sud file which
 * has been selected and, when they have finished, a report appears in the tab
 * below. The sound check also adds a tab of spectrograms per sud file it opened.
 *
 * @author Jamie Macaulay
 */
public class SudVerifyPane extends BorderPane {

	/**
	 * The colour used for checks which passed.
	 */
	private static final Color PASS_COLOUR = Color.web("#2e7d32");

	/**
	 * The colour used for checks which failed.
	 */
	private static final Color FAIL_COLOUR = Color.web("#c62828");

	/**
	 * The colour used for checks which raised a warning.
	 */
	private static final Color WARN_COLOUR = Color.web("#ef6c00");

	/**
	 * The pane which holds the file selection controls and the decompression
	 * controls.
	 */
	private SudUnpackerPane sudUnpackerPane;

	/**
	 * Checks that no sud file in the folder is 0 kB.
	 */
	private ToggleSwitch zeroLengthToggle;

	/**
	 * Checks that every sud file has a matching xsensIMU.csv file.
	 */
	private ToggleSwitch xsensPresentToggle;

	/**
	 * Checks that the xsensIMU.csv files are a sensible size.
	 */
	private ToggleSwitch xsensSizeToggle;

	/**
	 * Opens a few sud files and shows a spectrogram of the sound in them.
	 */
	private ToggleSwitch soundToggle;

	/**
	 * Starts and stops the checks.
	 */
	private Button runButton;

	/**
	 * Shows how far through the files the checks are.
	 */
	private ProgressBar progressBar;

	/**
	 * Shows which file is being checked.
	 */
	private Label progressLabel;

	/**
	 * Holds the report and, after a sound check, the spectrograms.
	 */
	private TabPane resultTabPane;

	/**
	 * The tab which holds the text of the report.
	 */
	private Tab reportTab;

	/**
	 * The text of the report.
	 */
	private TextArea reportArea;

	/**
	 * A one line summary of the report which sits above it.
	 */
	private Label reportSummaryLabel;

	/**
	 * The currently running checks. Null if nothing is running.
	 */
	private SudVerifyTask verifyTask;

	/**
	 * The settings for the checks.
	 */
	private SudVerifyParams verifyParams = new SudVerifyParams();

	public SudVerifyPane(SudUnpackerPane sudUnpackerPane) {
		this.sudUnpackerPane = sudUnpackerPane;
		setCenter(createVerifyPane());
		enableControls();
	}

	/**
	 * Create all the controls for the checking pane.
	 * @return the pane holding the controls.
	 */
	private VBox createVerifyPane() {

		/**************Checks Section ***************/

		Label checkLabel = new Label("Checks");
		sudUnpackerPane.setTitleLabel(checkLabel);

		zeroLengthToggle = new ToggleSwitch();
		HBox zeroLengthRow = createCheckRow(zeroLengthToggle, "fltral-document-error-24", "0 kB sud files",
				"Check that none of the sud files in the folder are 0 kB");

		xsensPresentToggle = new ToggleSwitch();
		HBox xsensPresentRow = createCheckRow(xsensPresentToggle, "fltral-document-search-24", "Xsens CSV files",
				"Check that every sud file has a matching *" + SudVerifyTask.XSENS_SUFFIX + " file");

		xsensSizeToggle = new ToggleSwitch();
		HBox xsensSizeRow = createCheckRow(xsensSizeToggle, "fltrmz-ruler-24", "Xsens size check",
				"Check that every *" + SudVerifyTask.XSENS_SUFFIX + " file is above "
						+ (int) verifyParams.minXsensSizeKB + " kB");

		soundToggle = new ToggleSwitch();
		HBox soundRow = createCheckRow(soundToggle, "fltral-data-histogram-24", "Sud sound check",
				"Open " + verifyParams.nSoundFiles + " sud files spread through the deployment and plot a spectrogram of a "
						+ (int) verifyParams.soundSnippetSeconds + " second snippet from each of them");

		/**************Progress  Section ***************/

		Label runLabel = new Label("Run checks");
		sudUnpackerPane.setTitleLabel(runLabel);

		runButton = new Button();
		runButton.setTooltip(new Tooltip("Start or stop the checks"));
		runButton.setGraphic(SudIkonDude.createPamIcon("fltfmz-play-20", SudUnpackerPane.DEFAULT_IKON_SIZE));
		runButton.setOnAction((action) -> {
			if (verifyTask == null) {
				runChecks();
			}
			else {
				verifyTask.cancel();
			}
		});

		progressBar = new ProgressBar();
		progressBar.setProgress(0);
		progressBar.setPadding(new Insets(5, 5, 5, 5));

		progressLabel = new Label();

		BorderPane runBorderPane = new BorderPane();
		runBorderPane.setLeft(runButton);
		runBorderPane.setCenter(progressBar);
		BorderPane.setAlignment(progressBar, Pos.CENTER);
		runBorderPane.setBottom(progressLabel);

		/**************Results Section ***************/

		reportArea = new TextArea();
		reportArea.setEditable(false);
		reportArea.setWrapText(true);
		reportArea.setFont(Font.font("Monospaced", 12));
		reportArea.setText("Select some checks and press play to check the selected sud files.");

		reportSummaryLabel = new Label();
		reportSummaryLabel.setPadding(new Insets(5, 5, 0, 5));
		reportSummaryLabel.setVisible(false);
		reportSummaryLabel.setManaged(false);

		BorderPane reportPane = new BorderPane();
		reportPane.setTop(reportSummaryLabel);
		reportPane.setCenter(reportArea);

		reportTab = new Tab("Report", reportPane);
		reportTab.setGraphic(SudIkonDude.createPamIcon("fltral-clipboard-text-24", 16));
		reportTab.setClosable(false);

		resultTabPane = new TabPane(reportTab);
		resultTabPane.setTabClosingPolicy(TabClosingPolicy.UNAVAILABLE);
		resultTabPane.setMinHeight(0);
		resultTabPane.setPrefHeight(150);
		resultTabPane.setMaxHeight(Double.MAX_VALUE);

		/**************Overall Layout**************/

		VBox vBox = new VBox();
		vBox.setSpacing(5);
		vBox.getChildren().addAll(checkLabel, zeroLengthRow, xsensPresentRow, xsensSizeRow,
				soundRow, runLabel, runBorderPane, resultTabPane);

		//the results soak up any spare vertical space as the window is resized.
		VBox.setVgrow(resultTabPane, Priority.ALWAYS);

		return vBox;
	}

	/**
	 * Create a row of controls for one of the checks.
	 * @param toggleSwitch - the toggle switch which turns the check on and off.
	 * @param iconString - the icon to show next to the check.
	 * @param name - the name of the check.
	 * @param tooltip - a description of what the check does.
	 * @return the row of controls.
	 */
	private HBox createCheckRow(ToggleSwitch toggleSwitch, String iconString, String name, String tooltip) {
		toggleSwitch.setSelected(true);
		toggleSwitch.selectedProperty().addListener((obsVal, oldVal, newVal) -> enableControls());
		return SudUnpackerPane.createToggleRow(toggleSwitch, iconString, name, tooltip);
	}

	/**
	 * Start the checks running on their own thread.
	 */
	private void runChecks() {

		List<File> sudFiles = sudUnpackerPane.getSudFiles();

		if (sudFiles == null || sudFiles.isEmpty()) {
			Alert alert = new Alert(AlertType.ERROR);
			alert.setHeaderText("Cannot check SUD files");
			alert.setContentText("There are no sud files selected. Select a folder of sud files first.");
			alert.show();
			return;
		}

		getParams(verifyParams);

		//clear out anything left over from the last run.
		resultTabPane.getTabs().retainAll(reportTab);
		resultTabPane.getSelectionModel().select(reportTab);
		reportArea.setText("");
		reportSummaryLabel.setVisible(false);
		reportSummaryLabel.setManaged(false);

		verifyTask = new SudVerifyTask(sudFiles, sudUnpackerPane.getSaveFolder(), verifyParams);

		progressBar.progressProperty().bind(verifyTask.progressProperty());
		progressLabel.textProperty().bind(verifyTask.messageProperty());

		//the report arrives once the checks have really stopped, which for a
		//cancelled run is a moment after the task says it has been cancelled.
		verifyTask.setOnReportReady(report -> showReport(report));

		verifyTask.setOnSucceeded(event -> checksFinished());

		verifyTask.setOnCancelled(event -> {
			reportArea.setText("The checks were stopped. Finishing the current file...");
			checksFinished();
		});

		verifyTask.setOnFailed(event -> {
			Throwable error = verifyTask.getException();
			reportArea.setText("The checks could not be completed:\n\n"
					+ (error == null ? "unknown error" : error.toString()));
			if (error != null) {
				error.printStackTrace();
			}
			checksFinished();
		});

		setRunButtonIcon(true);
		enableControls();

		Thread thread = new Thread(verifyTask);
		thread.setDaemon(true);
		thread.start();
	}

	/**
	 * Tidy up once the checks have stopped, whether they finished or not.
	 */
	private void checksFinished() {
		progressBar.progressProperty().unbind();
		progressLabel.textProperty().unbind();
		verifyTask = null;
		setRunButtonIcon(false);
		enableControls();
	}

	/**
	 * Show a report in the report tab, and add a tab of spectrograms for every
	 * sud file which was opened by the sound check.
	 * @param report - the report to show. Can be null.
	 */
	private void showReport(SudVerifyReport report) {
		if (report == null) {
			reportArea.setText("No results.");
			return;
		}

		reportArea.setText(report.toReportString());

		setSummaryLabel(report);

		for (SudSpectrogramData specData : report.getSpectrograms()) {
			Tab tab = new Tab(specData.getFile().getName(), new SpectrogramPane(specData));
			tab.setGraphic(SudIkonDude.createPamIcon("fltral-data-histogram-24", 16));
			tab.setTooltip(new Tooltip(specData.getFile().getAbsolutePath()));
			tab.setClosable(false);
			resultTabPane.getTabs().add(tab);
		}
	}

	/**
	 * Set the one line summary which sits above the report.
	 * @param report - the report to summarise.
	 */
	private void setSummaryLabel(SudVerifyReport report) {

		Color colour;
		String iconString;
		String text = report.getVerdictText();

		switch (report.getVerdict()) {
		case PASSED:
			colour = PASS_COLOUR;
			iconString = "fltral-checkmark-circle-24";
			break;
		case FAILED:
			text += " - see below";
			colour = FAIL_COLOUR;
			iconString = "fltral-error-circle-24";
			break;
		default:
			colour = WARN_COLOUR;
			iconString = "fltrmz-warning-24";
			break;
		}

		reportSummaryLabel.setText(text);
		reportSummaryLabel.setTextFill(colour);
		reportSummaryLabel.setFont(Font.font(null, FontWeight.BOLD, 13));
		reportSummaryLabel.setGraphic(SudIkonDude.createPamIcon(iconString, colour, 18));
		reportSummaryLabel.setVisible(true);
		reportSummaryLabel.setManaged(true);
	}

	/**
	 * Set whether the run button shows a play or a stop symbol.
	 * @param isRunning - true if the checks are running.
	 */
	private void setRunButtonIcon(boolean isRunning) {
		runButton.setGraphic(SudIkonDude.createPamIcon(isRunning ? "fltfmz-stop-20" : "fltfmz-play-20",
				SudUnpackerPane.DEFAULT_IKON_SIZE));
	}

	/**
	 * Enable or disable the controls depending on what has been selected and
	 * whether the checks are running.
	 */
	private void enableControls() {
		boolean anyCheck = zeroLengthToggle.isSelected() || xsensPresentToggle.isSelected()
				|| xsensSizeToggle.isSelected() || soundToggle.isSelected();

		runButton.setDisable(!anyCheck && verifyTask == null);

		boolean running = verifyTask != null;
		zeroLengthToggle.setDisable(running);
		xsensPresentToggle.setDisable(running);
		xsensSizeToggle.setDisable(running);
		soundToggle.setDisable(running);
	}


	/**
	 * Stop the checks if they are running.
	 */
	public void stop() {
		if (verifyTask != null) {
			verifyTask.cancel();
		}
	}

	/**
	 * Update the check parameters from the controls.
	 * @param params - the parameters to update. Note this is not cloned within the function.
	 * @return the updated parameters.
	 */
	public SudVerifyParams getParams(SudVerifyParams params) {
		params.checkZeroLength = zeroLengthToggle.isSelected();
		params.checkXsensPresent = xsensPresentToggle.isSelected();
		params.checkXsensSize = xsensSizeToggle.isSelected();
		params.checkSound = soundToggle.isSelected();
		return params;
	}

	/**
	 * Update the controls to reflect the current parameters.
	 * @param params - the params to set.
	 */
	public void setParams(SudVerifyParams params) {
		this.verifyParams = params;
		zeroLengthToggle.setSelected(params.checkZeroLength);
		xsensPresentToggle.setSelected(params.checkXsensPresent);
		xsensSizeToggle.setSelected(params.checkXsensSize);
		soundToggle.setSelected(params.checkSound);
	}

}
