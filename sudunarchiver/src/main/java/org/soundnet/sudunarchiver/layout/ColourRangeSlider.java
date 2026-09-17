package org.soundnet.sudunarchiver.layout;

import org.controlsfx.control.RangeSlider;
import org.soundnet.sudunarchiver.layout.ColourArray.ColourArrayType;

import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.layout.Pane;

/**
 * A vertical range slider which is coloured with a colour map, exactly as the
 * colour range slider in PAMGuard is. The section between the two thumbs shows
 * the colour map, everything above the top thumb is the colour of the top of
 * the map and everything below the bottom thumb is the colour of the bottom of
 * the map.
 * <p>
 * Dragging the thumbs changes the amplitude limits of whatever the slider is
 * attached to - here a spectrogram.
 *
 * @author Jamie Macaulay
 */
public class ColourRangeSlider extends RangeSlider {

	/**
	 * The bar which sits above the high thumb and is filled with the colour at
	 * the top of the colour map.
	 */
	private Pane topBar = new Pane();

	/**
	 * The current colour map.
	 */
	private ColourArrayType colourArrayType = ColourArrayType.GREY;

	/**
	 * The colour array for the current colour map.
	 */
	private ColourArray colourArray;

	public ColourRangeSlider() {
		this(Orientation.VERTICAL);
	}

	public ColourRangeSlider(Orientation orientation) {
		super();
		setOrientation(orientation);
		setShowTickMarks(false);
		setShowTickLabels(false);
		topBar.setMouseTransparent(true);
		setColourArrayType(colourArrayType);
	}

	/**
	 * Set the colour map used by the slider.
	 * @param colourArrayType - the colour map.
	 */
	public void setColourArrayType(ColourArrayType colourArrayType) {
		this.colourArrayType = colourArrayType;
		this.colourArray = ColourArray.createStandardColourArray(ColourArray.DEFAULT_N_COLOURS, colourArrayType);
		colourTracks();
		requestLayout();
	}

	/**
	 * Get the current colour map.
	 * @return the colour map.
	 */
	public ColourArrayType getColourArrayType() {
		return colourArrayType;
	}

	/**
	 * Colour the track, the range bar and the top bar to show the colour map.
	 * The nodes only exist once the skin has been created, so this is called
	 * again on every layout pass.
	 */
	private void colourTracks() {
		boolean vertical = getOrientation() == Orientation.VERTICAL;

		Node track = lookup(".track");
		if (track != null) {
			track.setStyle("-fx-background-color: " + ColourArray.colourToHex(colourArray.getColour(0.)) + ";");
		}

		Node rangeBar = lookup(".range-bar");
		if (rangeBar != null) {
			rangeBar.setStyle("-fx-background-color: " + colourArray.getCSSGradient(vertical) + ";");
		}

		topBar.setStyle("-fx-background-color: " + ColourArray.colourToHex(colourArray.getColour(1.)) + ";");
	}

	@Override
	protected void layoutChildren() {
		super.layoutChildren();

		colourTracks();

		Node track = lookup(".track");
		Node highThumb = lookup(".high-thumb");
		if (track == null || highThumb == null) {
			return;
		}

		//sit the top bar just above the track so it is under the thumbs.
		if (!getChildren().contains(topBar)) {
			int trackIndex = getChildren().indexOf(track);
			getChildren().add(trackIndex < 0 ? 0 : trackIndex + 1, topBar);
		}

		if (getOrientation() == Orientation.VERTICAL) {
			double top = track.getLayoutY();
			double bottom = highThumb.getLayoutY() + highThumb.getBoundsInLocal().getHeight() / 2.;
			topBar.resizeRelocate(track.getLayoutX(), top,
					track.getBoundsInLocal().getWidth(), Math.max(0, bottom - top));
		}
		else {
			double left = highThumb.getLayoutX() + highThumb.getBoundsInLocal().getWidth() / 2.;
			double right = track.getLayoutX() + track.getBoundsInLocal().getWidth();
			topBar.resizeRelocate(left, track.getLayoutY(),
					Math.max(0, right - left), track.getBoundsInLocal().getHeight());
		}
	}

}
