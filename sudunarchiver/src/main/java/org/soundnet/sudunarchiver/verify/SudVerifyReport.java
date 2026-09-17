package org.soundnet.sudunarchiver.verify;

import java.util.ArrayList;
import java.util.List;

/**
 * The results of running a set of checks on a folder of sud files.
 *
 * @author Jamie Macaulay
 */
public class SudVerifyReport {

	/**
	 * How a single check ended up.
	 */
	public enum CheckStatus {
		PASSED, FAILED, WARNING, SKIPPED
	}

	/**
	 * The result of a single check.
	 */
	public static class CheckResult {

		private String name;

		private CheckStatus status;

		private String summary;

		/**
		 * Extra lines of detail, usually the names of the offending files.
		 */
		private List<String> detail = new ArrayList<String>();

		public CheckResult(String name, CheckStatus status, String summary) {
			this.name = name;
			this.status = status;
			this.summary = summary;
		}

		public String getName() {
			return name;
		}

		public CheckStatus getStatus() {
			return status;
		}

		public String getSummary() {
			return summary;
		}

		public List<String> getDetail() {
			return detail;
		}

		public void addDetail(String line) {
			detail.add(line);
		}

	}

	/**
	 * The results of each check which was run.
	 */
	private List<CheckResult> results = new ArrayList<CheckResult>();

	/**
	 * The spectrogram data from the sound check. Empty if the sound check was
	 * not run.
	 */
	private List<SudSpectrogramData> spectrograms = new ArrayList<SudSpectrogramData>();

	/**
	 * The number of sud files which were checked.
	 */
	private int nFiles;

	/**
	 * True if the checks were stopped before they finished.
	 */
	private boolean cancelled = false;

	public SudVerifyReport(int nFiles) {
		this.nFiles = nFiles;
	}

	public void addResult(CheckResult result) {
		results.add(result);
	}

	public List<CheckResult> getResults() {
		return results;
	}

	public List<SudSpectrogramData> getSpectrograms() {
		return spectrograms;
	}

	public int getNFiles() {
		return nFiles;
	}

	public boolean isCancelled() {
		return cancelled;
	}

	public void setCancelled(boolean cancelled) {
		this.cancelled = cancelled;
	}

	/**
	 * The overall outcome of a run of checks.
	 */
	public enum Verdict {
		PASSED, FAILED, CANCELLED, NOTHING_RUN, NO_CHECKS
	}

	/**
	 * The overall outcome of the run. Both the text report and the summary line
	 * above it come from this, so they can never disagree.
	 * @return the verdict.
	 */
	public Verdict getVerdict() {
		if (cancelled) {
			return Verdict.CANCELLED;
		}
		if (results.isEmpty()) {
			return Verdict.NO_CHECKS;
		}
		if (getNFailed() > 0) {
			return Verdict.FAILED;
		}
		if (getNPassed() == 0) {
			//everything that ran was a warning, so there is nothing to pass.
			return Verdict.NOTHING_RUN;
		}
		return Verdict.PASSED;
	}

	/**
	 * A one line summary of the outcome of the run.
	 * @return the summary.
	 */
	public String getVerdictText() {
		switch (getVerdict()) {
		case CANCELLED:
			return "The checks were stopped before they finished";
		case NO_CHECKS:
			return "No checks were selected";
		case NOTHING_RUN:
			return "No checks could be run";
		case FAILED:
			int nFailed = getNFailed();
			return nFailed + (nFailed == 1 ? " check failed" : " checks failed");
		default:
			return "All checks passed";
		}
	}

	/**
	 * The number of checks which passed.
	 * @return the number of passed checks.
	 */
	public int getNPassed() {
		int n = 0;
		for (CheckResult result : results) {
			if (result.getStatus() == CheckStatus.PASSED) n++;
		}
		return n;
	}

	/**
	 * The number of checks which failed.
	 * @return the number of failed checks.
	 */
	public int getNFailed() {
		int n = 0;
		for (CheckResult result : results) {
			if (result.getStatus() == CheckStatus.FAILED) n++;
		}
		return n;
	}

	/**
	 * Create a human readable version of the report.
	 * @return the report as text.
	 */
	public String toReportString() {
		StringBuilder sb = new StringBuilder();

		if (cancelled) {
			sb.append("The checks were stopped before they finished.\n\n");
		}

		sb.append(nFiles).append(nFiles == 1 ? " sud file was selected.\n\n" : " sud files were selected.\n\n");

		for (CheckResult result : results) {
			sb.append(statusSymbol(result.getStatus())).append("  ").append(result.getName()).append("\n");
			sb.append("     ").append(result.getSummary()).append("\n");
			for (String line : result.getDetail()) {
				sb.append("        ").append(line).append("\n");
			}
			sb.append("\n");
		}

		if (!cancelled) {
			//a cancelled run has already said so at the top of the report.
			sb.append(getVerdictText()).append(".\n");
		}

		return sb.toString();
	}

	/**
	 * A symbol to put in front of a check in the text report.
	 * @param status - the status of the check.
	 * @return a short symbol.
	 */
	private static String statusSymbol(CheckStatus status) {
		switch (status) {
		case PASSED: return "[PASS]";
		case FAILED: return "[FAIL]";
		case WARNING: return "[WARN]";
		default: return "[ -- ]";
		}
	}

}
