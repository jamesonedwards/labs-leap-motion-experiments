package util;

import processing.core.PApplet;

import com.leapmotion.leap.Controller;
import com.leapmotion.leap.InteractionBox;
import com.leapmotion.leap.Vector;

public class LeapMotionUtil {
	public static final int LEFT_HAND = 1;
	public static final int RIGHT_HAND = 2;

	// Make ctor private so you can only use this class statically.
	private LeapMotionUtil() {

	}

	/**
	 * Convert the Leap Position position Vector to a Vector in the Processing window coordinate system.
	 * 
	 * @param pApplet
	 * @param lmController
	 * @param leapVector
	 * @return
	 */
	public static Vector leapToProcessingVector(PApplet pApplet, Controller lmController, Vector leapVector) {
		// Normalize the coordinates.
		InteractionBox iBox = lmController.frame().interactionBox();
		Vector iBoxVector = iBox.normalizePoint(leapVector);

		// Center points around the center of window, since the Leap origin is the center. Y is negative since coordinate system is different.
		return new Vector(PApplet.map(iBoxVector.getX(), 0, 1, 0, pApplet.width), PApplet.map(iBoxVector.getY(), 0, 1, pApplet.height, 0),
				iBoxVector.getZ());
	}

	public static Vector leapToProcessingVector(PApplet pApplet, Controller lmController, Vector leapVector, int hand) {
		float xMin = 0;
		float xMax = 0;

		switch (hand) {
		case LEFT_HAND:
			xMin = 0;
			xMax = pApplet.width / 2;
			break;
		case RIGHT_HAND:
			xMin = pApplet.width / 2;
			xMax = pApplet.width;
			break;
		}

		// Normalize the coordinates.
		InteractionBox iBox = lmController.frame().interactionBox();
		Vector iBoxVector = iBox.normalizePoint(leapVector);

		// Center points around the center of window, since the Leap origin is the center. Y is negative since coordinate system is different.
		return new Vector(PApplet.map(iBoxVector.getX(), 0, 1, xMin, xMax), PApplet.map(iBoxVector.getY(), 0, 1, pApplet.height, 0),
				iBoxVector.getZ());
	}
}
