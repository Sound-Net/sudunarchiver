package org.soundnet.sudunarchiver.layout;

import org.soundnet.sudunarchiver.layout.ColourArray.ColourArrayType;
import org.soundnet.sudunarchiver.verify.SudSpectrogramData;

import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.Tooltip;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

/**
 * Shows the spectrograms of a snippet of sound taken from a single sud file.
 * There is one spectrogram per channel and a colour range slider, the same as
 * the one in PAMGuard, which sets the amplitude limits of the colour map. There
 * are no other controls - this is a quick look at the data, not a display to
 * navigate around.
 *
 * @author Jamie Macaulay
 */
public class SpectrogramPane extends BorderPane {

	/**
	 * The gap, in pixels, between controls.
	 */
	private static final double SPACING = 5;

	/**
	 * The width, in pixels, of the colour map chooser.
	 */
	private static final double COLOUR_BOX_WIDTH = 130;

	/**
	 * The width, in pixels, of the colour range slider.
	 */
	private static final double SLIDER_WIDTH = 26;

	/**
	 * The units the spectrogram colours are in.
	 */
	public static final String SPECTRAL_LEVEL_UNITS = "dB re 1 \u00b5Pa\u00b2/Hz";

	/**
	 * The width, in pixels, given over to the frequency axis.
	 */
	private static final double FREQ_AXIS_WIDTH = 52;

	/**
	 * The height, in pixels, given over to the time axis.
	 */
	private static final double TIME_AXIS_HEIGHT = 30;

	/**
	 * The gap, in pixels, left at the top of each spectrogram.
	 */
	private static final double TOP_MARGIN = 4;

	/**
	 * The gap, in pixels, left at the right of each spectrogram so that the last
	 * time label is not cut in half.
	 */
	private static final double RIGHT_MARGIN = 14;

	/**
	 * The gap, in pixels, left at the bottom of the spectrograms which have no
	 * time axis, so that the bottom frequency label has room.
	 */
	private static final double BOTTOM_MARGIN = 8;

	/**
	 * The data which is being shown.
	 */
	private SudSpectrogramData specData;

	/**
	 * The colour map used for the spectrograms.
	 */
	private ColourArrayType colourArrayType = ColourArrayType.HOT;

	/**
	 * The colours of the current colour map.
	 */
	private ColourArray colourArray = ColourArray.createStandardColourArray(
			ColourArray.DEFAULT_N_COLOURS, colourArrayType);

	/**
	 * The slider which sets the amplitude limits of the colour map.
	 */
	private ColourRangeSlider colourSlider;

	/**
	 * The chooser for the colour map.
	 */
	private ComboBox<ColourArrayType> colourBox;

	/**
	 * One display per channel.
	 */
	private SpectrogramChannelPane[] channelPanes;

	/**
	 * The images which hold the coloured spectrogram data, one per channel.
	 */
	private WritableImage[] images;

	public SpectrogramPane(SudSpectrogramData specData) {
		this.specData = specData;

		setCenter(createChannelPanes());
		setRight(createColourSlider());
		//the header holds the colour map chooser, so the slider it changes has
		//to be there first.
		setTop(createHeader());

		colourImages();
	}

	/**
	 * Create the row which sits above the spectrograms, with a description of
	 * the data on the left and the colour map chooser on the right.
	 * @return the header row.
	 */
	private Pane createHeader() {
		BorderPane header = new BorderPane();
		//the description goes in the centre rather than on the left because a
		//border pane always gives its left and right regions their preferred
		//width, and only the centre is allowed to shrink in a narrow window.
		header.setCenter(createHeaderLabel());
		header.setRight(createColourMapBox());
		BorderPane.setAlignment(header.getCenter(), Pos.CENTER_LEFT);
		return header;
	}

	/**
	 * Create a chooser for the colour map.
	 * @return the colour map chooser.
	 */
	private Node createColourMapBox() {

		colourBox = new ComboBox<ColourArrayType>();
		colourBox.getItems().addAll(ColourArrayType.values());
		colourBox.setValue(colourArrayType);
		colourBox.setTooltip(new Tooltip("Change the colours the spectrogram is plotted with"));
		colourBox.setMinWidth(COLOUR_BOX_WIDTH);
		colourBox.setPrefWidth(COLOUR_BOX_WIDTH);

		//show the name of each colour map next to a swatch of its colours.
		colourBox.setButtonCell(new ColourArrayCell());
		colourBox.setCellFactory(listView -> new ColourArrayCell());

		colourBox.valueProperty().addListener((obsVal, oldVal, newVal) -> {
			if (newVal != null) {
				setColourArrayType(newVal);
			}
		});

		HBox hBox = new HBox();
		hBox.setSpacing(SPACING);
		hBox.setAlignment(Pos.CENTER_RIGHT);
		hBox.setPadding(new Insets(2, 5, 2, 5));
		hBox.getChildren().add(colourBox);

		return hBox;
	}

	/**
	 * Set the colour map the spectrograms are plotted with.
	 * @param colourArrayType - the colour map to use.
	 */
	public void setColourArrayType(ColourArrayType colourArrayType) {
		this.colourArrayType = colourArrayType;
		if (colourBox != null) {
			//keeps the chooser right when the colour map is set in code.
			colourBox.setValue(colourArrayType);
		}
		this.colourArray = ColourArray.createStandardColourArray(ColourArray.DEFAULT_N_COLOURS, colourArrayType);
		colourSlider.setColourArrayType(colourArrayType);
		colourImages();
	}


	/**
	 * A list cell which shows the name of a colour map next to a swatch of its
	 * colours.
	 */
	private static class ColourArrayCell extends ListCell<ColourArrayType> {

		@Override
		protected void updateItem(ColourArrayType item, boolean empty) {
			super.updateItem(item, empty);
			if (empty || item == null) {
				setText(null);
				setGraphic(null);
				return;
			}
			setText(item.getName());

			Region swatch = new Region();
			swatch.setPrefSize(28, 12);
			swatch.setMinSize(28, 12);
			swatch.setStyle("-fx-background-color: "
					+ ColourArray.createStandardColourArray(ColourArray.DEFAULT_N_COLOURS, item).getCSSGradient(false)
					+ "; -fx-border-color: grey;");
			setGraphic(swatch);
		}

	}

	/**
	 * Create the label which sits above the spectrograms and says where the data
	 * came from.
	 * @return the header label.
	 */
	private Label createHeaderLabel() {
		String text = specData.getFile().getName() + "   -   "
				+ String.format("%.1f", specData.getSampleRate() / 1000.) + " kHz, "
				+ specData.getNChannels() + (specData.getNChannels() == 1 ? " channel, " : " channels, ")
				+ String.format("%.0f", specData.getDurationSeconds()) + " s snippet   -   "
				+ SPECTRAL_LEVEL_UNITS;

		Label label = new Label(text);
		label.setPadding(new Insets(5, 5, 5, 5));
		label.setTooltip(new Tooltip(specData.getFile().getAbsolutePath()));
		//without this the description holds the whole pane open at its full
		//width in a narrow window and the colour slider gets pushed off the edge.
		label.setMinWidth(0);
		return label;
	}

	/**
	 * Create one spectrogram display per channel.
	 * @return a pane holding all the channel displays.
	 */
	private Pane createChannelPanes() {
		int nChannels = specData.getNChannels();

		channelPanes = new SpectrogramChannelPane[nChannels];
		images = new WritableImage[nChannels];

		VBox vBox = new VBox();
		vBox.setSpacing(2);

		for (int i = 0; i < nChannels; i++) {
			double[][] channelSpec = specData.getSpectrogram()[i];
			int nSlices = channelSpec.length;
			int nBins = nSlices > 0 ? channelSpec[0].length : 1;
			images[i] = new WritableImage(Math.max(1, nSlices), Math.max(1, nBins));

			//only the bottom channel needs a time axis - they all share the same time scale.
			channelPanes[i] = new SpectrogramChannelPane(i, i == nChannels - 1);
			VBox.setVgrow(channelPanes[i], Priority.ALWAYS);
			vBox.getChildren().add(channelPanes[i]);
		}

		return vBox;
	}

	/**
	 * Create the colour range slider and the labels which sit around it.
	 * @return a pane holding the slider.
	 */
	private Pane createColourSlider() {

		colourSlider = new ColourRangeSlider(Orientation.VERTICAL);
		colourSlider.setColourArrayType(colourArrayType);
		colourSlider.setMin(specData.getSliderMinDB());
		colourSlider.setMax(specData.getSliderMaxDB());
		//the high value has to be set first. A range slider will not let the low
		//value past the high one, and until the high value has been set it is
		//still sitting wherever changing the minimum left it.
		colourSlider.setHighValue(specData.getDefaultHighDB());
		colourSlider.setLowValue(specData.getDefaultLowDB());

		colourSlider.setTooltip(new Tooltip("Drag to change the spectral level limits, in "
				+ SPECTRAL_LEVEL_UNITS + ", of the spectrogram colours"));
		//the thumbs are wider than the track, so the slider needs room for them
		//or they end up cut in half against the edge of a narrow window.
		colourSlider.setMinWidth(SLIDER_WIDTH);
		colourSlider.setPrefWidth(SLIDER_WIDTH);

		colourSlider.lowValueProperty().addListener((obsVal, oldVal, newVal) -> colourImages());
		colourSlider.highValueProperty().addListener((obsVal, oldVal, newVal) -> colourImages());

		Label dbLabel = new Label("dB");
		dbLabel.setFont(Font.font(11));
		dbLabel.setTooltip(new Tooltip(SPECTRAL_LEVEL_UNITS));

		VBox vBox = new VBox();
		vBox.setSpacing(5);
		vBox.setPadding(new Insets(5, 5, 5, 5));
		vBox.setAlignment(Pos.CENTER);
		vBox.getChildren().addAll(dbLabel, colourSlider);
		VBox.setVgrow(colourSlider, Priority.ALWAYS);

		return vBox;
	}

	/**
	 * Colour every spectrogram image using the current amplitude limits and then
	 * repaint the displays.
	 */
	private void colourImages() {

		double lowDB = colourSlider == null ? specData.getDefaultLowDB() : colourSlider.getLowValue();
		double highDB = colourSlider == null ? specData.getDefaultHighDB() : colourSlider.getHighValue();
		double range = highDB - lowDB;
		if (range <= 0) range = 1;

		int[] argbColours = colourArray.getARGBColours();
		int nColours = argbColours.length;

		for (int chan = 0; chan < specData.getNChannels(); chan++) {
			double[][] channelSpec = specData.getSpectrogram()[chan];
			int nSlices = channelSpec.length;
			if (nSlices == 0) continue;
			int nBins = channelSpec[0].length;

			int[] pixels = new int[nSlices * nBins];
			for (int slice = 0; slice < nSlices; slice++) {
				for (int bin = 0; bin < nBins; bin++) {
					int index = (int) ((channelSpec[slice][bin] - lowDB) / range * (nColours - 1));
					if (index < 0) index = 0;
					if (index >= nColours) index = nColours - 1;
					//images are drawn from the top down but low frequencies go at the bottom.
					pixels[(nBins - 1 - bin) * nSlices + slice] = argbColours[index];
				}
			}

			images[chan].getPixelWriter().setPixels(0, 0, nSlices, nBins,
					PixelFormat.getIntArgbInstance(), pixels, 0, nSlices);
		}

		for (SpectrogramChannelPane channelPane : channelPanes) {
			channelPane.repaint();
		}
	}

	/**
	 * A single channel of the spectrogram, with a frequency axis and, for the
	 * bottom channel, a time axis.
	 */
	private class SpectrogramChannelPane extends Pane {

		private Canvas canvas = new Canvas();

		private int channel;

		/**
		 * True if this channel shows the time axis.
		 */
		private boolean showTimeAxis;

		private SpectrogramChannelPane(int channel, boolean showTimeAxis) {
			this.channel = channel;
			this.showTimeAxis = showTimeAxis;
			getChildren().add(canvas);
			setMinHeight(80);
			setPrefHeight(200);
			//a pane takes its minimum size from its preferred size, and the
			//preferred size of this one comes from the canvas, which is as wide
			//as the pane last was. Without this the display can grow but never
			//shrink, and the colour slider ends up off the edge of the window.
			setMinWidth(0);
		}

		@Override
		protected void layoutChildren() {
			super.layoutChildren();
			canvas.setWidth(getWidth());
			canvas.setHeight(getHeight());
			repaint();
		}

		/**
		 * Redraw the spectrogram and its axes.
		 */
		private void repaint() {
			double width = canvas.getWidth();
			double height = canvas.getHeight();
			if (width <= 0 || height <= 0) return;

			GraphicsContext gc = canvas.getGraphicsContext2D();
			gc.clearRect(0, 0, width, height);

			double plotX = FREQ_AXIS_WIDTH;
			double plotY = TOP_MARGIN;
			double plotWidth = width - FREQ_AXIS_WIDTH - RIGHT_MARGIN;
			double plotHeight = height - TOP_MARGIN - (showTimeAxis ? TIME_AXIS_HEIGHT : BOTTOM_MARGIN);
			if (plotWidth <= 0 || plotHeight <= 0) return;

			gc.setImageSmoothing(false);
			gc.drawImage(images[channel], plotX, plotY, plotWidth, plotHeight);

			gc.setStroke(Color.GREY);
			gc.setLineWidth(1);
			gc.strokeRect(plotX + 0.5, plotY + 0.5, plotWidth - 1, plotHeight - 1);

			drawFrequencyAxis(gc, plotX, plotY, plotHeight);
			if (showTimeAxis) {
				drawTimeAxis(gc, plotX, plotY + plotHeight, plotWidth);
			}
		}

		/**
		 * Draw the frequency axis down the left hand side of the spectrogram.
		 */
		private void drawFrequencyAxis(GraphicsContext gc, double plotX, double plotY, double plotHeight) {
			double maxFreqKHz = specData.getSampleRate() / 2000.;

			gc.setStroke(Color.GREY);
			gc.setFill(Color.GREY);
			gc.setFont(Font.font(10));
			gc.setTextAlign(TextAlignment.RIGHT);

			//a handful of labels, however tall the display is.
			int nTicks = Math.max(2, Math.min(6, (int) (plotHeight / 40)));
			for (int i = 0; i <= nTicks; i++) {
				double frac = (double) i / nTicks;
				double y = plotY + plotHeight * (1 - frac);
				gc.strokeLine(plotX - 4, y, plotX, y);
				gc.fillText(String.format(maxFreqKHz < 10 ? "%.1f" : "%.0f", frac * maxFreqKHz),
						plotX - 7, y + 4);
			}

			//the axis title, written down the side.
			gc.save();
			gc.translate(10, plotY + plotHeight / 2);
			gc.rotate(-90);
			gc.setTextAlign(TextAlignment.CENTER);
			gc.fillText("kHz", 0, 0);
			gc.restore();
		}

		/**
		 * Draw the time axis along the bottom of the spectrogram.
		 */
		private void drawTimeAxis(GraphicsContext gc, double plotX, double plotBottom, double plotWidth) {
			double duration = specData.getDurationSeconds();

			gc.setStroke(Color.GREY);
			gc.setFill(Color.GREY);
			gc.setFont(Font.font(10));
			gc.setTextAlign(TextAlignment.CENTER);

			int nTicks = Math.max(2, Math.min(10, (int) (plotWidth / 60)));
			for (int i = 0; i <= nTicks; i++) {
				double frac = (double) i / nTicks;
				double x = plotX + plotWidth * frac;
				gc.strokeLine(x, plotBottom, x, plotBottom + 4);
				gc.fillText(String.format("%.0f", frac * duration), x, plotBottom + 15);
			}

			gc.fillText("Time (s)", plotX + plotWidth / 2, plotBottom + 27);
		}

	}

}
