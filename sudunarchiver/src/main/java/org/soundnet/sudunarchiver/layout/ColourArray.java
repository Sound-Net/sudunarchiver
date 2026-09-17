package org.soundnet.sudunarchiver.layout;

import javafx.scene.paint.Color;

/**
 * A simple colour map, used to colour spectrogram images and the colour range
 * slider which sits alongside them.
 * <p>
 * This is a cut down version of the PAMGuard colour array so that the look of
 * the spectrograms here matches the look of the spectrograms there.
 *
 * @author Jamie Macaulay
 */
public class ColourArray {

	/**
	 * The available colour maps.
	 */
	public enum ColourArrayType {
		GREY, REVERSEGREY, HOT, FIRE, INFERNO, BLUE, GREEN, RED;

		/**
		 * A name for the colour map that can be shown in a menu.
		 * @return a human readable name.
		 */
		public String getName() {
			switch (this) {
			case GREY: return "Grey";
			case REVERSEGREY: return "Reverse grey";
			case HOT: return "Hot";
			case FIRE: return "Fire";
			case INFERNO: return "Inferno";
			case BLUE: return "Blue";
			case GREEN: return "Green";
			case RED: return "Red";
			default: return this.toString();
			}
		}
	}

	/**
	 * The default number of colours in an array.
	 */
	public static final int DEFAULT_N_COLOURS = 256;

	/**
	 * The colours, from the lowest value to the highest.
	 */
	private Color[] colours;

	private ColourArray(Color[] colours) {
		this.colours = colours;
	}

	/**
	 * Create one of the standard colour arrays.
	 * @param nColours - the number of colours in the array.
	 * @param type - the type of colour array.
	 * @return the colour array.
	 */
	public static ColourArray createStandardColourArray(int nColours, ColourArrayType type) {
		switch (type) {
		case GREY:
			return createMultiColouredArray(nColours, Color.BLACK, Color.WHITE);
		case REVERSEGREY:
			return createMultiColouredArray(nColours, Color.WHITE, Color.BLACK);
		case HOT:
			return createMultiColouredArray(nColours, Color.BLUE, Color.GREEN, Color.YELLOW, Color.RED);
		case FIRE:
			return createMultiColouredArray(nColours, Color.BLACK, Color.RED, Color.YELLOW, Color.WHITE);
		case INFERNO:
			return createMultiColouredArray(nColours, Color.rgb(0, 0, 4), Color.rgb(87, 16, 110),
					Color.rgb(188, 55, 84), Color.rgb(249, 142, 9), Color.rgb(252, 255, 164));
		case BLUE:
			return createMultiColouredArray(nColours, Color.BLACK, Color.BLUE);
		case GREEN:
			return createMultiColouredArray(nColours, Color.BLACK, Color.GREEN);
		case RED:
			return createMultiColouredArray(nColours, Color.BLACK, Color.RED);
		default:
			return createMultiColouredArray(nColours, Color.BLACK, Color.WHITE);
		}
	}

	/**
	 * Create a colour array which interpolates between a set of control colours.
	 * @param nColours - the number of colours in the array.
	 * @param controlColours - the colours to interpolate between, lowest first.
	 * @return the colour array.
	 */
	public static ColourArray createMultiColouredArray(int nColours, Color... controlColours) {
		Color[] colours = new Color[nColours];
		int nSections = controlColours.length - 1;
		for (int i = 0; i < nColours; i++) {
			//position within the full array, scaled to the control colour index.
			double pos = nColours == 1 ? 0 : (double) i * nSections / (nColours - 1);
			int section = Math.min((int) pos, nSections - 1);
			double frac = pos - section;
			colours[i] = controlColours[section].interpolate(controlColours[section + 1], frac);
		}
		return new ColourArray(colours);
	}



	/**
	 * Get a colour by index.
	 * @param i - the index of the colour.
	 * @return the colour.
	 */
	public Color getColour(int i) {
		if (i < 0) i = 0;
		if (i >= colours.length) i = colours.length - 1;
		return colours[i];
	}

	/**
	 * Get a colour from a value between 0 and 1.
	 * @param value - a value between 0 (the bottom of the map) and 1 (the top).
	 * @return the colour.
	 */
	public Color getColour(double value) {
		return getColour((int) Math.round(value * (colours.length - 1)));
	}

	/**
	 * Get the colours as packed ARGB integers, which is what is needed to write
	 * pixels straight into an image.
	 * @return an array of ARGB values, one per colour.
	 */
	public int[] getARGBColours() {
		int[] argb = new int[colours.length];
		for (int i = 0; i < colours.length; i++) {
			Color c = colours[i];
			argb[i] = 0xFF000000
					| ((int) Math.round(c.getRed() * 255) << 16)
					| ((int) Math.round(c.getGreen() * 255) << 8)
					| ((int) Math.round(c.getBlue() * 255));
		}
		return argb;
	}

	/**
	 * Create a CSS linear gradient string for the colour array.
	 * @param toTop - true for a gradient that runs bottom to top, false for left to right.
	 * @return a CSS linear-gradient(...) string.
	 */
	public String getCSSGradient(boolean toTop) {
		StringBuilder sb = new StringBuilder("linear-gradient(to ");
		sb.append(toTop ? "top" : "right");
		//no need for all the colours - a handful of stops gives the same gradient.
		int nStops = Math.min(colours.length, 12);
		for (int i = 0; i < nStops; i++) {
			double frac = (double) i / (nStops - 1);
			sb.append(", ").append(colourToHex(getColour(frac)))
			.append(" ").append(String.format("%.1f", frac * 100.)).append("%");
		}
		sb.append(")");
		return sb.toString();
	}

	/**
	 * Convert a colour to a hex string that CSS understands.
	 * @param colour - the colour to convert.
	 * @return the hex string, e.g. #ff0000
	 */
	public static String colourToHex(Color colour) {
		return String.format("#%02x%02x%02x",
				(int) Math.round(colour.getRed() * 255),
				(int) Math.round(colour.getGreen() * 255),
				(int) Math.round(colour.getBlue() * 255));
	}

}
