package org.soundnet.sudunarchiver.verify;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

import javax.sound.sampled.AudioFormat;

import org.pamguard.x3.sud.SudAudioInputStream;
import org.pamguard.x3.sud.SudParams;
import org.soundnet.sudunarchiver.verify.SudVerifyReport.CheckResult;
import org.soundnet.sudunarchiver.verify.SudVerifyReport.CheckStatus;

import javafx.application.Platform;
import javafx.concurrent.Task;

/**
 * Runs a set of checks over a list of sud files and builds a report on what it
 * found. This runs on its own thread so that the user interface stays
 * responsive while long checks, such as opening sud files to look at the sound
 * within them, are running.
 *
 * @author Jamie Macaulay
 */
public class SudVerifyTask extends Task<SudVerifyReport> {

	/**
	 * The suffix which decompressed IMU files have.
	 */
	public static final String XSENS_SUFFIX = ".xsensIMU.csv";

	/**
	 * The maximum number of file names listed against any one check in the report.
	 */
	private static final int MAX_DETAIL_LINES = 10;

	/**
	 * The longest error message put into the report against a file.
	 */
	private static final int MAX_ERROR_LENGTH = 90;

	/**
	 * The FFT length used for the spectrograms.
	 */
	private static final int FFT_LENGTH = 1024;

	/**
	 * The maximum number of time slices in a spectrogram. The hop between FFTs
	 * is increased for long snippets so that the images stay a sensible size.
	 */
	private static final int MAX_SPEC_SLICES = 2400;

	/**
	 * The sud files to check.
	 */
	private List<File> sudFiles;

	/**
	 * The folder decompressed files are saved to. Can be null, in which case
	 * decompressed files sit alongside the sud files.
	 */
	private File saveFolder;

	/**
	 * Which checks to run.
	 */
	private SudVerifyParams params;

	/**
	 * The report which is built up as the checks run.
	 */
	private SudVerifyReport report;

	public SudVerifyTask(List<File> sudFiles, File saveFolder, SudVerifyParams params) {
		this.sudFiles = new ArrayList<File>(sudFiles);
		this.saveFolder = saveFolder;
		this.params = params;
		this.report = new SudVerifyReport(this.sudFiles.size());
	}

	/**
	 * Called on the JavaFX thread once the checks have stopped.
	 */
	private Consumer<SudVerifyReport> onReportReady;

	/**
	 * Set what to do with the report once the checks have stopped, whether they
	 * ran to the end or were stopped part way through.
	 * <p>
	 * This is not the same as the task's own cancelled handler, which fires the
	 * moment the checks are told to stop while this thread is still working. By
	 * the time this is called the report has been finished with and is safe to
	 * read.
	 * @param onReportReady - called with the report, on the JavaFX thread.
	 */
	public void setOnReportReady(Consumer<SudVerifyReport> onReportReady) {
		this.onReportReady = onReportReady;
	}

	/**
	 * Hand the finished report over to the JavaFX thread.
	 * @return the report.
	 */
	private SudVerifyReport publishReport() {
		if (onReportReady != null) {
			Platform.runLater(() -> onReportReady.accept(report));
		}
		return report;
	}

	@Override
	protected void cancelled() {
		super.cancelled();
		//the worker only notices between files, so mark the report here instead.
		report.setCancelled(true);
	}

	@Override
	protected SudVerifyReport call() throws Exception {

		if (sudFiles.isEmpty()) {
			//nothing to check. Saying so once is a good deal clearer than every
			//check reporting that all none of the files were fine.
			report.addResult(new CheckResult("Sud files", CheckStatus.WARNING,
					"There are no sud files to check."));
			updateProgress(1, 1);
			return publishReport();
		}

		//sud file names carry the date, so sorting by name puts the deployment in order.
		Collections.sort(sudFiles, Comparator.comparing(File::getName));

		boolean anyFileCheck = params.checkZeroLength || params.checkXsensPresent || params.checkXsensSize;

		List<File> soundFiles = params.checkSound ? selectSoundFiles() : new ArrayList<File>();

		//the sound check is far slower than the file checks, so give it a good share of the progress bar.
		long soundUnit = anyFileCheck && !soundFiles.isEmpty()
				? Math.max(1, sudFiles.size() / soundFiles.size()) : 1;
		long totalWork = (anyFileCheck ? sudFiles.size() : 0) + soundFiles.size() * soundUnit;
		if (totalWork == 0) totalWork = 1;
		long work = 0;
		updateProgress(0, totalWork);

		//------- the checks which only need the files on disk -------
		List<File> zeroLengthFiles = new ArrayList<File>();
		List<File> missingXsens = new ArrayList<File>();
		List<File> smallXsens = new ArrayList<File>();
		int nXsensFound = 0;

		if (anyFileCheck) {
			for (File sudFile : sudFiles) {
				if (isCancelled()) {
					return publishReport();
				}

				updateMessage("Checking " + sudFile.getName());

				if (params.checkZeroLength && sudFile.length() == 0) {
					zeroLengthFiles.add(sudFile);
				}

				if (params.checkXsensPresent || params.checkXsensSize) {
					File xsensFile = findXsensFile(sudFile);
					if (xsensFile == null) {
						missingXsens.add(sudFile);
					}
					else {
						nXsensFound++;
						if (xsensFile.length() < params.minXsensSizeKB * 1024) {
							smallXsens.add(xsensFile);
						}
					}
				}

				updateProgress(++work, totalWork);
			}
		}

		if (params.checkZeroLength) {
			report.addResult(zeroLengthResult(zeroLengthFiles));
		}
		if (params.checkXsensPresent) {
			report.addResult(xsensPresentResult(missingXsens));
		}
		if (params.checkXsensSize) {
			report.addResult(xsensSizeResult(smallXsens, nXsensFound));
		}

		//------- the sound check -------
		if (params.checkSound) {
			CheckResult soundResult;
			List<String> failures = new ArrayList<String>();

			if (soundFiles.isEmpty()) {
				soundResult = new CheckResult("Sud sound check", CheckStatus.FAILED,
						"There are no sud files with any data in them to open.");
			}
			else {
				for (File soundFile : soundFiles) {
					if (isCancelled()) {
						return publishReport();
					}

					updateMessage("Opening " + soundFile.getName() + " for the sound check");

					try {
						SudSpectrogramData specData = loadSpectrogram(soundFile);
						if (specData == null) {
							failures.add(soundFile.getName() + " contains no audio");
						}
						else {
							report.getSpectrograms().add(specData);
						}
					}
					catch (Exception e) {
						failures.add(soundFile.getName()
								+ " could not be read - it may not be a valid sud file (" + errorText(e) + ")");
					}

					work += soundUnit;
					updateProgress(work, totalWork);
				}

				int nRead = soundFiles.size() - failures.size();

				if (failures.isEmpty()) {
					soundResult = new CheckResult("Sud sound check", CheckStatus.PASSED,
							"Sound was read from " + sudFileCount(nRead)
							+ " sampled through the deployment. See the spectrogram "
							+ (nRead == 1 ? "tab." : "tabs."));
				}
				else {
					soundResult = new CheckResult("Sud sound check", CheckStatus.FAILED,
							sudFilesOf(failures.size(), soundFiles.size())
							+ " sampled did not contain readable sound.");
					addDetail(soundResult, failures);
				}

				//with a short deployment, or one full of empty files, there may
				//not be as many files to sample as the check would normally use.
				if (soundFiles.size() < params.nSoundFiles) {
					soundResult.addDetail("The check normally samples " + params.nSoundFiles
							+ " sud files. Only " + sudFileCount(soundFiles.size())
							+ " with data in " + (soundFiles.size() == 1 ? "it was" : "them were")
							+ " available.");
				}
			}

			report.addResult(soundResult);
		}

		updateMessage("Checks complete");
		updateProgress(totalWork, totalWork);

		return publishReport();
	}

	/**
	 * Build the result for the zero length sud file check.
	 * @param zeroLengthFiles - the files which were zero length.
	 * @return the check result.
	 */
	private CheckResult zeroLengthResult(List<File> zeroLengthFiles) {
		CheckResult result;
		if (zeroLengthFiles.isEmpty()) {
			result = new CheckResult("0 kB sud files", CheckStatus.PASSED,
					sudFiles.size() == 1 ? "The sud file is not 0 kB."
							: "None of the " + sudFileCount(sudFiles.size()) + " are 0 kB.");
		}
		else {
			result = new CheckResult("0 kB sud files", CheckStatus.FAILED,
					sudFilesOf(zeroLengthFiles.size(), sudFiles.size()) + " "
					+ isAre(zeroLengthFiles.size()) + " 0 kB.");
			addDetail(result, fileNames(zeroLengthFiles));
		}
		return result;
	}

	/**
	 * Build the result for the xsens file presence check.
	 * @param missingXsens - the sud files with no matching xsens file.
	 * @return the check result.
	 */
	private CheckResult xsensPresentResult(List<File> missingXsens) {
		CheckResult result;
		if (missingXsens.isEmpty()) {
			result = new CheckResult("Xsens CSV files", CheckStatus.PASSED,
					sudFilesOf(sudFiles.size(), sudFiles.size()) + " " + hasHave(sudFiles.size())
					+ " a matching *" + XSENS_SUFFIX + " file.");
		}
		else {
			String summary;
			if (sudFiles.size() == 1) {
				summary = "The sud file has no matching *" + XSENS_SUFFIX + " file.";
			}
			else if (missingXsens.size() == sudFiles.size()) {
				//"all of them have no match" is a clumsy way of saying none of them do.
				summary = "None of the " + sudFileCount(sudFiles.size()) + " have a matching *"
						+ XSENS_SUFFIX + " file.";
			}
			else {
				summary = missingXsens.size() + " of the " + sudFileCount(sudFiles.size()) + " "
						+ hasHave(missingXsens.size()) + " no matching *" + XSENS_SUFFIX + " file.";
			}
			result = new CheckResult("Xsens CSV files", CheckStatus.FAILED, summary);
			addDetail(result, fileNames(missingXsens));
		}
		return result;
	}

	/**
	 * Build the result for the xsens file size check.
	 * @param smallXsens - the xsens files which are below the size limit.
	 * @param nXsensFound - the number of xsens files which were found.
	 * @return the check result.
	 */
	private CheckResult xsensSizeResult(List<File> smallXsens, int nXsensFound) {
		CheckResult result;
		int minKB = (int) params.minXsensSizeKB;
		if (nXsensFound == 0) {
			result = new CheckResult("Xsens size check", CheckStatus.WARNING,
					"There are no *" + XSENS_SUFFIX + " files to check.");
		}
		else if (smallXsens.isEmpty()) {
			result = new CheckResult("Xsens size check", CheckStatus.PASSED,
					xsensFilesOf(nXsensFound, nXsensFound) + " " + isAre(nXsensFound)
					+ " above " + minKB + " kB.");
		}
		else {
			result = new CheckResult("Xsens size check", CheckStatus.FAILED,
					xsensFilesOf(smallXsens.size(), nXsensFound) + " "
					+ isAre(smallXsens.size()) + " below " + minKB + " kB.");
			List<String> lines = new ArrayList<String>();
			for (File file : smallXsens) {
				lines.add(file.getName() + "  (" + (file.length() / 1024) + " kB)");
			}
			addDetail(result, lines);
		}
		return result;
	}

	/**
	 * Find the xsens IMU csv file which goes with a sud file. Decompressed files
	 * either sit next to the sud file or in the save folder, so look in both.
	 * @param sudFile - the sud file.
	 * @return the matching xsens file, or null if there is not one.
	 */
	private File findXsensFile(File sudFile) {
		String name = sudFile.getName();
		if (name.toLowerCase().endsWith(".sud")) {
			name = name.substring(0, name.length() - 4);
		}
		String xsensName = name + XSENS_SUFFIX;

		File local = new File(sudFile.getParentFile(), xsensName);
		if (local.exists()) {
			return local;
		}

		if (saveFolder != null) {
			File saved = new File(saveFolder, xsensName);
			if (saved.exists()) {
				return saved;
			}
		}

		return null;
	}

	/**
	 * Pick a few sud files which are spread evenly through the deployment. Zero
	 * length files are left out because there is nothing in them to open.
	 * @return the files to open for the sound check.
	 */
	private List<File> selectSoundFiles() {
		List<File> candidates = new ArrayList<File>();
		for (File file : sudFiles) {
			if (file.length() > 0) {
				candidates.add(file);
			}
		}

		//a short deployment, or one full of empty files, simply gets checked
		//with however many files it does have.
		int nWanted = Math.min(params.nSoundFiles, candidates.size());
		if (nWanted <= 0) {
			return new ArrayList<File>();
		}

		List<File> selected = new ArrayList<File>();
		for (int i = 0; i < nWanted; i++) {
			//spread the files through the deployment rather than bunching them at the ends.
			int index = (int) Math.floor((i + 0.5) * candidates.size() / nWanted);
			index = Math.min(index, candidates.size() - 1);
			File file = candidates.get(index);
			if (!selected.contains(file)) {
				selected.add(file);
			}
		}

		return selected;
	}

	/**
	 * Open a sud file, read a snippet of sound from the start of it and turn
	 * that into a spectrogram for each channel.
	 * @param sudFile - the sud file to open.
	 * @return the spectrogram data, or null if the file held no audio.
	 * @throws Exception if the file could not be opened.
	 */
	private SudSpectrogramData loadSpectrogram(File sudFile) throws Exception {

		SudParams sudParams = new SudParams();
		sudParams.setVerbose(false);
		//nothing is written to disk - this is only reading the sound.
		sudParams.setFileSave(false, false, false, false, false);
		sudParams.setSudEnable(true, false, false, false);
		sudParams.setSudFilePath(sudFile.getAbsolutePath());

		SudAudioInputStream audioStream = null;
		try {
			audioStream = SudAudioInputStream.openInputStream(sudFile, sudParams, false);

			AudioFormat format = audioStream.getFormat();
			float sampleRate = format.getSampleRate();
			int nChannels = format.getChannels();
			int bytesPerSample = Math.max(1, format.getSampleSizeInBits() / 8);

			if (sampleRate <= 0 || nChannels <= 0) {
				return null;
			}

			int nFrames = (int) Math.round(sampleRate * params.soundSnippetSeconds);
			byte[] buffer = new byte[nFrames * nChannels * bytesPerSample];

			int bytesRead = 0;
			while (bytesRead < buffer.length) {
				int read = audioStream.read(buffer, bytesRead, buffer.length - bytesRead);
				if (read <= 0) break;
				bytesRead += read;
				if (isCancelled()) break;
			}

			int framesRead = bytesRead / (nChannels * bytesPerSample);
			if (framesRead < FFT_LENGTH) {
				return null;
			}

			double[][] channelData = toChannelData(buffer, framesRead, nChannels, bytesPerSample,
					format.isBigEndian());

			double[][][] spectrogram = new double[nChannels][][];
			for (int i = 0; i < nChannels; i++) {
				spectrogram[i] = createSpectrogram(channelData[i], sampleRate);
			}

			return new SudSpectrogramData(sudFile, sampleRate, framesRead / sampleRate, spectrogram);
		}
		finally {
			if (audioStream != null) {
				try {
					audioStream.close();
				}
				catch (Exception e) {
					//nothing useful to do if the stream will not close.
				}
			}
		}
	}

	/**
	 * Split interleaved audio bytes into one array of samples per channel,
	 * scaled to between -1 and 1.
	 * @param buffer - the raw audio bytes.
	 * @param nFrames - the number of complete frames in the buffer.
	 * @param nChannels - the number of channels.
	 * @param bytesPerSample - the number of bytes in each sample.
	 * @param bigEndian - true if the samples are big endian.
	 * @return the samples, [channel][sample].
	 */
	private double[][] toChannelData(byte[] buffer, int nFrames, int nChannels, int bytesPerSample,
			boolean bigEndian) {

		double[][] channelData = new double[nChannels][nFrames];
		double scale = Math.pow(2, 8 * bytesPerSample - 1);

		for (int frame = 0; frame < nFrames; frame++) {
			for (int chan = 0; chan < nChannels; chan++) {
				int start = (frame * nChannels + chan) * bytesPerSample;
				long value = 0;
				if (bigEndian) {
					for (int i = 0; i < bytesPerSample; i++) {
						value = (value << 8) | (buffer[start + i] & 0xFF);
					}
				}
				else {
					for (int i = bytesPerSample - 1; i >= 0; i--) {
						value = (value << 8) | (buffer[start + i] & 0xFF);
					}
				}
				//sign extend from the sample size up to a long.
				int shift = 64 - 8 * bytesPerSample;
				value = (value << shift) >> shift;

				channelData[chan][frame] = value / scale;
			}
		}

		return channelData;
	}

	/**
	 * Create a spectrogram from a single channel of sound.
	 * <p>
	 * The result is a calibrated power spectral density in dB re 1 uPa^2/Hz. A
	 * full scale sample is half the peak to peak voltage of the recorder's
	 * analogue to digital converter, and the system sensitivity turns that
	 * voltage into a pressure.
	 * @param data - the samples, between -1 and 1.
	 * @param sampleRate - the sample rate in samples per second.
	 * @return the spectrogram in dB re 1 uPa^2/Hz, [time slice][frequency bin].
	 */
	private double[][] createSpectrogram(double[] data, float sampleRate) {

		int hop = FFT_LENGTH / 2;
		int nFFTs = Math.max(1, (data.length - FFT_LENGTH) / hop + 1);

		//a 20 second snippet holds far more FFTs than there are pixels to show
		//them in. Averaging them down, rather than throwing most of them away,
		//uses all of the sound and takes most of the speckle out of the noise
		//floor, which is what makes a spectrogram easy to read.
		int nSlices = Math.min(nFFTs, MAX_SPEC_SLICES);
		int fftsPerSlice = (int) Math.ceil((double) nFFTs / nSlices);
		nSlices = (int) Math.ceil((double) nFFTs / fftsPerSlice);

		double[] window = FFT.hannWindow(FFT_LENGTH);
		int nBins = FFT_LENGTH / 2;

		//a full scale sample sits at the peak of the converter range.
		double fullScaleVolts = params.adcPeakToPeakVolts / 2.;

		//the window and the FFT length both scale the transform, and the result
		//is one sided, so everything but DC and Nyquist carries twice the power.
		double windowPower = 0;
		for (double w : window) {
			windowPower += w * w;
		}
		double psdScale = 2 * fullScaleVolts * fullScaleVolts / (sampleRate * windowPower);

		//volts to micro Pascals, from the end to end system sensitivity.
		double calibrationDB = -params.systemSensitivityDB;

		double[][] spectrogram = new double[nSlices][nBins];
		int[] sliceCounts = new int[nSlices];
		double[] real = new double[FFT_LENGTH];
		double[] imag = new double[FFT_LENGTH];

		for (int fft = 0; fft < nFFTs; fft++) {
			int start = fft * hop;
			for (int i = 0; i < FFT_LENGTH; i++) {
				int index = start + i;
				real[i] = index < data.length ? data[index] * window[i] : 0;
				imag[i] = 0;
			}

			FFT.fft(real, imag);

			//sum the power of each FFT into the slice it belongs to.
			int slice = Math.min(fft / fftsPerSlice, nSlices - 1);
			double[] sliceData = spectrogram[slice];
			for (int bin = 0; bin < nBins; bin++) {
				sliceData[bin] += (real[bin] * real[bin] + imag[bin] * imag[bin]) * psdScale;
			}
			sliceCounts[slice]++;
		}

		for (int slice = 0; slice < nSlices; slice++) {
			int count = Math.max(1, sliceCounts[slice]);
			for (int bin = 0; bin < nBins; bin++) {
				//a floor, well below anything a recorder can resolve, keeps
				//digital silence off the bottom of the dB scale.
				spectrogram[slice][bin] = 10 * Math.log10(Math.max(spectrogram[slice][bin] / count, 1e-20))
						+ calibrationDB;
			}
		}

		return spectrogram;
	}

	/**
	 * Add detail lines to a check result, trimming very long lists.
	 * @param result - the result to add the lines to.
	 * @param lines - the lines to add.
	 */
	private void addDetail(CheckResult result, List<String> lines) {
		int nShown = Math.min(lines.size(), MAX_DETAIL_LINES);
		for (int i = 0; i < nShown; i++) {
			result.addDetail(lines.get(i));
		}
		if (lines.size() > nShown) {
			result.addDetail("...and " + (lines.size() - nShown) + " more");
		}
	}

	/**
	 * Turn an exception into something short enough to sit in a report. Errors
	 * thrown from inside the sud library mean nothing to anyone reading the
	 * report, but they are worth keeping for anyone trying to work out why a
	 * particular file will not open.
	 * @param e - the exception.
	 * @return a short description of what went wrong.
	 */
	private static String errorText(Throwable e) {
		String message = e.getMessage();
		if (message == null || message.trim().isEmpty()) {
			return e.getClass().getSimpleName();
		}
		return message.length() > MAX_ERROR_LENGTH
				? message.substring(0, MAX_ERROR_LENGTH - 3) + "..." : message;
	}

	/**
	 * Phrase a number of sud files, e.g. "1 sud file" or "6 sud files".
	 * @param n - the number of files.
	 * @return the number and the noun, in the right form.
	 */
	private static String sudFileCount(int n) {
		return n + (n == 1 ? " sud file" : " sud files");
	}

	/**
	 * Phrase a number of xsens files, e.g. "1 *.xsensIMU.csv file".
	 * @param n - the number of files.
	 * @return the number and the noun, in the right form.
	 */
	private static String xsensFileCount(int n) {
		return n + " *" + XSENS_SUFFIX + (n == 1 ? " file" : " files");
	}

	/**
	 * Phrase a count of sud files against the total, in whatever form reads
	 * properly: "The sud file", "All 3 sud files" or "2 of the 6 sud files".
	 * @param n - the number of files being talked about.
	 * @param total - the total number of files.
	 * @return the phrase, which starts a sentence.
	 */
	private static String sudFilesOf(int n, int total) {
		if (total == 1) {
			return "The sud file";
		}
		if (n == total) {
			return "All " + sudFileCount(total);
		}
		return n + " of the " + sudFileCount(total);
	}

	/**
	 * Phrase a count of xsens files against the total, in whatever form reads
	 * properly.
	 * @param n - the number of files being talked about.
	 * @param total - the total number of files.
	 * @return the phrase, which starts a sentence.
	 */
	private static String xsensFilesOf(int n, int total) {
		if (total == 1) {
			return "The one *" + XSENS_SUFFIX + " file";
		}
		if (n == total) {
			return "All " + xsensFileCount(total);
		}
		return n + " of the " + xsensFileCount(total);
	}

	/**
	 * The form of "to be" which goes with a count.
	 * @param n - the number of things.
	 * @return "is" or "are".
	 */
	private static String isAre(int n) {
		return n == 1 ? "is" : "are";
	}

	/**
	 * The form of "to have" which goes with a count.
	 * @param n - the number of things.
	 * @return "has" or "have".
	 */
	private static String hasHave(int n) {
		return n == 1 ? "has" : "have";
	}

	/**
	 * Get the names of a list of files.
	 * @param files - the files.
	 * @return the file names.
	 */
	private List<String> fileNames(List<File> files) {
		List<String> names = new ArrayList<String>();
		for (File file : files) {
			names.add(file.getName());
		}
		return names;
	}

}
