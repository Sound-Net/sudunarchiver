package org.soundnet.sudunarchiver.verify;

/**
 * A minimal in place radix 2 FFT, used to calculate spectrograms for the sound
 * check.
 *
 * @author Jamie Macaulay
 */
public class FFT {

	/**
	 * In place complex FFT. The arrays must be a power of two in length.
	 * @param real - the real part of the data, overwritten with the real part of the transform.
	 * @param imag - the imaginary part of the data, overwritten with the imaginary part of the transform.
	 */
	public static void fft(double[] real, double[] imag) {
		int n = real.length;
		if (n <= 1) return;
		if (Integer.bitCount(n) != 1) {
			throw new IllegalArgumentException("The FFT length must be a power of two: " + n);
		}

		//bit reversal re-ordering.
		for (int i = 1, j = 0; i < n; i++) {
			int bit = n >> 1;
			for (; (j & bit) != 0; bit >>= 1) {
				j ^= bit;
			}
			j ^= bit;
			if (i < j) {
				double tmp = real[i]; real[i] = real[j]; real[j] = tmp;
				tmp = imag[i]; imag[i] = imag[j]; imag[j] = tmp;
			}
		}

		//butterflies.
		for (int len = 2; len <= n; len <<= 1) {
			double ang = -2 * Math.PI / len;
			double wReal = Math.cos(ang);
			double wImag = Math.sin(ang);
			for (int i = 0; i < n; i += len) {
				double curReal = 1, curImag = 0;
				for (int j = 0; j < len / 2; j++) {
					int a = i + j;
					int b = i + j + len / 2;
					double reB = real[b] * curReal - imag[b] * curImag;
					double imB = real[b] * curImag + imag[b] * curReal;
					real[b] = real[a] - reB;
					imag[b] = imag[a] - imB;
					real[a] += reB;
					imag[a] += imB;
					double nextReal = curReal * wReal - curImag * wImag;
					curImag = curReal * wImag + curImag * wReal;
					curReal = nextReal;
				}
			}
		}
	}

	/**
	 * Create a Hann window.
	 * @param n - the length of the window.
	 * @return the window coefficients.
	 */
	public static double[] hannWindow(int n) {
		double[] window = new double[n];
		for (int i = 0; i < n; i++) {
			window[i] = 0.5 * (1 - Math.cos(2 * Math.PI * i / (n - 1)));
		}
		return window;
	}

	/**
	 * The next power of two which is greater than or equal to n.
	 * @param n - the number.
	 * @return the next power of two.
	 */
	public static int nextPowerOfTwo(int n) {
		int p = 1;
		while (p < n) p <<= 1;
		return p;
	}

}
