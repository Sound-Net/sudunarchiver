package org.soundnet.sudunarchiver.verify;

import java.io.File;
import java.util.Arrays;

/**
 * The spectrogram data for a snippet of sound taken from one sud file. There is
 * one spectrogram per channel.
 *
 * @author Jamie Macaulay
 */
public class SudSpectrogramData {

	/**
	 * The most values used to work out the percentiles of the data.
	 */
	private static final int MAX_SAMPLE_VALUES = 200000;

	/**
	 * The sud file the data came from.
	 */
	private File file;

	/**
	 * The sample rate in samples per second.
	 */
	private float sampleRate;

	/**
	 * The spectrogram surfaces. [channel][time slice][frequency bin], in dB.
	 */
	private double[][][] spectrogram;

	/**
	 * The length of the snippet in seconds.
	 */
	private double durationSeconds;

	/**
	 * The minimum dB value within the data.
	 */
	private double minDB = Double.MAX_VALUE;

	/**
	 * The maximum dB value within the data.
	 */
	private double maxDB = -Double.MAX_VALUE;

	/**
	 * The bottom of the colour slider range.
	 */
	private double sliderMinDB;

	/**
	 * The top of the colour slider range.
	 */
	private double sliderMaxDB;

	/**
	 * The amplitude limit the colour map starts at the bottom of.
	 */
	private double defaultLowDB;

	/**
	 * The amplitude limit the colour map starts at the top of.
	 */
	private double defaultHighDB;

	public SudSpectrogramData(File file, float sampleRate, double durationSeconds, double[][][] spectrogram) {
		this.file = file;
		this.sampleRate = sampleRate;
		this.durationSeconds = durationSeconds;
		this.spectrogram = spectrogram;

		for (double[][] channel : spectrogram) {
			for (double[] slice : channel) {
				for (double value : slice) {
					if (value < minDB) minDB = value;
					if (value > maxDB) maxDB = value;
				}
			}
		}
		if (minDB > maxDB) {
			//no data at all.
			minDB = 0;
			maxDB = 1;
		}

		calcColourLimits();
	}

	/**
	 * Work out sensible amplitude limits for the colour map.
	 * <p>
	 * The full range of the data is no use for this. A handful of near silent
	 * bins and one loud click between them will stretch the range so far that
	 * everything else is a single flat colour. Percentiles of the data are used
	 * instead, so the display starts off showing the sound rather than the
	 * extremes.
	 */
	private void calcColourLimits() {

		double[] sample = sampleValues();

		if (sample.length == 0) {
			sliderMinDB = minDB;
			sliderMaxDB = maxDB;
			defaultLowDB = minDB;
			defaultHighDB = maxDB;
			return;
		}

		Arrays.sort(sample);

		//the colour map starts off spanning the bulk of the data. The loudest
		//one percent, which in a sud file is usually a handful of clicks or a
		//knock on the tag, is left saturated at the top of the map - trying to
		//fit those in as well leaves everything else a single flat colour.
		defaultLowDB = percentile(sample, 0.3);
		defaultHighDB = percentile(sample, 0.99);
		if (defaultHighDB <= defaultLowDB) {
			defaultHighDB = defaultLowDB + 1;
		}

		//leave room to move the limits in both directions without squashing the
		//thumbs into the corner of a range set by one very loud or quiet bin.
		double span = defaultHighDB - defaultLowDB;
		sliderMinDB = Math.max(minDB, defaultLowDB - span);
		sliderMaxDB = Math.min(maxDB, defaultHighDB + 2 * span);
		if (sliderMaxDB <= sliderMinDB) {
			sliderMaxDB = sliderMinDB + 1;
		}
	}

	/**
	 * Take an evenly spread sample of the spectrogram values. There is no need
	 * to sort millions of numbers to find a percentile.
	 * @return the sampled values.
	 */
	private double[] sampleValues() {
		long nValues = 0;
		for (double[][] channel : spectrogram) {
			for (double[] slice : channel) {
				nValues += slice.length;
			}
		}
		if (nValues == 0) {
			return new double[0];
		}

		int stride = (int) Math.max(1, nValues / MAX_SAMPLE_VALUES);

		double[] sample = new double[(int) (nValues / stride) + 1];
		int nSampled = 0;
		long index = 0;
		for (double[][] channel : spectrogram) {
			for (double[] slice : channel) {
				for (double value : slice) {
					if (index++ % stride == 0 && nSampled < sample.length) {
						sample[nSampled++] = value;
					}
				}
			}
		}

		return Arrays.copyOf(sample, nSampled);
	}

	/**
	 * Find a percentile of a sorted array.
	 * @param sorted - the sorted values.
	 * @param fraction - the percentile, between 0 and 1.
	 * @return the value at that percentile.
	 */
	private static double percentile(double[] sorted, double fraction) {
		int index = (int) Math.round(fraction * (sorted.length - 1));
		index = Math.max(0, Math.min(index, sorted.length - 1));
		return sorted[index];
	}

	/**
	 * The bottom of the colour slider range.
	 * @return the lowest amplitude the slider can be set to, in dB.
	 */
	public double getSliderMinDB() {
		return sliderMinDB;
	}

	/**
	 * The top of the colour slider range.
	 * @return the highest amplitude the slider can be set to, in dB.
	 */
	public double getSliderMaxDB() {
		return sliderMaxDB;
	}

	/**
	 * The amplitude the colour map starts at the bottom of.
	 * @return the lower amplitude limit, in dB.
	 */
	public double getDefaultLowDB() {
		return defaultLowDB;
	}

	/**
	 * The amplitude the colour map starts at the top of.
	 * @return the upper amplitude limit, in dB.
	 */
	public double getDefaultHighDB() {
		return defaultHighDB;
	}

	public File getFile() {
		return file;
	}

	public float getSampleRate() {
		return sampleRate;
	}

	public double[][][] getSpectrogram() {
		return spectrogram;
	}

	public int getNChannels() {
		return spectrogram.length;
	}

	public double getDurationSeconds() {
		return durationSeconds;
	}

}
