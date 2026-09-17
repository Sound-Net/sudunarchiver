package org.soundnet.sudunarchiver.layout;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.value.ChangeListener;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;
import javafx.scene.transform.Rotate;
import javafx.util.Duration;

/**
 * A pane which holds two nodes and flips between them, like turning a card
 * over. Used here to flip between the decompression controls and the data
 * checking controls.
 *
 * @author Jamie Macaulay
 */
public class FlipPane extends StackPane {

	/**
	 * How long the flip takes.
	 */
	private static final Duration FLIP_TIME = Duration.millis(500);

	/**
	 * The node shown on the front of the card.
	 */
	private Node front;

	/**
	 * The node shown on the back of the card.
	 */
	private Node back;

	/**
	 * True when the front is showing.
	 */
	private ReadOnlyBooleanWrapper frontShowing = new ReadOnlyBooleanWrapper(true);

	/**
	 * The animation which turns the card over. Kept so that a second click
	 * partway through a flip does not leave two animations fighting.
	 */
	private Timeline flipAnimation;

	public FlipPane(Node front, Node back) {
		this.front = front;
		this.back = back;

		setRotationAxis(Rotate.Y_AXIS);

		//the back is mounted the other way round so that it is the right way
		//round once the card has been turned over.
		back.setRotationAxis(Rotate.Y_AXIS);
		back.setRotate(180);
		back.setVisible(false);

		getChildren().addAll(front, back);
	}

	/**
	 * Flip the pane over to show whichever node is not showing.
	 */
	public void flip() {
		showFront(!frontShowing.get());
	}

	/**
	 * Flip to a particular side.
	 * @param showFront - true to show the front, false to show the back.
	 */
	public void showFront(boolean showFront) {
		if (showFront == frontShowing.get() && flipAnimation == null) {
			return;
		}

		if (flipAnimation != null) {
			flipAnimation.stop();
		}

		frontShowing.set(showFront);

		double endAngle = showFront ? 0 : 180;

		flipAnimation = new Timeline(
				new KeyFrame(Duration.ZERO, new KeyValue(rotateProperty(), getRotate())),
				new KeyFrame(FLIP_TIME, new KeyValue(rotateProperty(), endAngle, Interpolator.EASE_BOTH)));

		//swap the two faces over as the card passes edge on.
		rotateProperty().addListener(faceSwapListener);

		flipAnimation.setOnFinished(e -> {
			rotateProperty().removeListener(faceSwapListener);
			setRotate(endAngle);
			updateFaces();
			flipAnimation = null;
		});

		flipAnimation.play();
	}

	/**
	 * Listens to the rotation and swaps which face is visible halfway through
	 * the flip.
	 */
	private final ChangeListener<Number> faceSwapListener =
			(obsVal, oldVal, newVal) -> updateFaces();

	/**
	 * Show whichever face is pointing at the user.
	 */
	private void updateFaces() {
		boolean showFront = getRotate() < 90;
		front.setVisible(showFront);
		back.setVisible(!showFront);
	}

	/**
	 * True when the front of the pane is showing.
	 * @return true if the front is showing.
	 */
	public boolean isFrontShowing() {
		return frontShowing.get();
	}

}
