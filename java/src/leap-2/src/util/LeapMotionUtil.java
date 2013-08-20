package util;

import processing.core.PApplet;

import com.leapmotion.leap.Controller;
import com.leapmotion.leap.Frame;
import com.leapmotion.leap.Hand;
import com.leapmotion.leap.InteractionBox;
import com.leapmotion.leap.Pointable;
import com.leapmotion.leap.Vector;

public class LeapMotionUtil {
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
}
