package org.soundnet.sudunarchiver.verify;

/**
 * The parameters for verifying a folder of sud files.
 *
 * @author Jamie Macaulay
 */
public class SudVerifyParams {

	/**
	 * True to check that no sud file is zero length.
	 */
	public boolean checkZeroLength = true;

	/**
	 * True to check that every sud file has a matching *.xsensIMU.csv file.
	 */
	public boolean checkXsensPresent = true;

	/**
	 * True to check that every *.xsensIMU.csv file is above the minimum size.
	 */
	public boolean checkXsensSize = true;

	/**
	 * True to open a few sud files and check the sound within them.
	 */
	public boolean checkSound = true;

	/**
	 * The minimum size, in kB, an xsensIMU.csv file must be to pass the size check.
	 */
	public double minXsensSizeKB = 200;

	/**
	 * The number of sud files, spread through the deployment, which are opened
	 * for the sound check.
	 */
	public int nSoundFiles = 4;

	/**
	 * The length, in seconds, of the snippet of sound which is used for the
	 * spectrograms in the sound check.
	 */
	public double soundSnippetSeconds = 20;

	/**
	 * The peak to peak voltage of the SoundTrap analogue to digital converter.
	 * A full scale sample is half of this.
	 */
	public double adcPeakToPeakVolts = 2.0;

	/**
	 * The end to end system sensitivity in dB re 1V/uPa, i.e. the hydrophone
	 * sensitivity plus whatever gain the recorder applies. This turns the
	 * spectrograms from arbitrary numbers into spectral levels in
	 * dB re 1 uPa^2/Hz.
	 */
	public double systemSensitivityDB = -180;

}
