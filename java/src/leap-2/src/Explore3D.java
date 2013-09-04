import java.util.logging.ConsoleHandler;
import java.util.logging.Logger;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.commons.lang3.exception.ExceptionUtils;

import processing.core.*;
import util.LeapMotionUtil;
import util.ShapeUtil;

import com.leapmotion.leap.Controller;
import com.leapmotion.leap.Frame;
import com.leapmotion.leap.Gesture;
import com.leapmotion.leap.Pointable;
import com.leapmotion.leap.SwipeGesture;
import com.leapmotion.leap.Vector;

/**
 * Sources:
 * http://processing.org/tutorials/transform2d/
 * http://processing.org/tutorials/p3d/
 * 
 * @author jameson.edwards
 */
public class Explore3D extends PApplet {
	// Logging:
	private final static Logger LOGGER = Logger.getLogger(Explore3D.class.getName() + "Logger");
	
	// Constants:
	private final static int WINDOW_WIDTH = 1280;
	private final static int WINDOW_HEIGHT = 720;
	private final static float Z_AXIS_MULTIPLIER = 3f;
	private final static int SHAPE_COUNT = 3;
	
	// Global variables:
	private static Controller lmController;
	private static float xmag, ymag = 0;
	private static float newXmag, newYmag = 0;
	private static float pointerX = 0;
	private static float pointerY = 0;
	private static float pointerZ = 1 / Z_AXIS_MULTIPLIER;
	private static int currentShape = 0;
	
	/**
	 * HACK: Get this PApplet to run from command line.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		PApplet.main(Explore3D.class.getName());
	}

	public void setup() {
		// This must be the first line of code in setup():
		size(WINDOW_WIDTH, WINDOW_HEIGHT, P3D);
		noStroke();
		colorMode(RGB, 1);

		// Setup logging.
		LOGGER.setLevel(java.util.logging.Level.INFO);
		LOGGER.addHandler(new ConsoleHandler());

		try {
			// Disable window resizing.
			ShapeUtil.findFrame(this).setResizable(false);

			// Instantiate the Leap Motion controller.
			lmController = new Controller();

			// Set gestures to track.
			lmController.enableGesture(Gesture.Type.TYPE_SWIPE);
			if (lmController.config().setFloat("Gesture.Swipe.MinLength", 150.0f)
					&& lmController.config().setFloat("Gesture.Swipe.MinVelocity", 100f))
				lmController.config().save();
		} catch (Exception ex) {
			String msg = "Exception details: \nType: " + ex.getClass().toString() + "\nMessage: " + ex.getMessage() + "\nStack trace: "
					+ ExceptionUtils.getStackTrace(ex) + "\n\n" + "Object state: "
					+ ReflectionToStringBuilder.toString(this, ToStringStyle.MULTI_LINE_STYLE, true, true) + "\n\n";
			LOGGER.severe(msg);
			exit(); // Exit program.
		}
	}

	/**
	 * Source: http://processing.org/examples/rgbcube.html
	 * 
	 * @param x
	 * @param y
	 */
	private void drawCube(float x, float y, float z) {
		LOGGER.fine("(" + x + ", " + y + ", " + z + ")");
		
		pushMatrix();
		translate(width / 2, height / 2, -30);

		newXmag = x / (float) width * TWO_PI;
		newYmag = y / (float) height * TWO_PI;

		float diff = xmag - newXmag;
		if (abs(diff) > 0.01) {
			xmag -= diff / 4.0;
		}

		diff = ymag - newYmag;
		if (abs(diff) > 0.01) {
			ymag -= diff / 4.0;
		}

		rotateX(-ymag);
		rotateY(-xmag);

		//scale(90);
		// Control scaling with the z coordinate.
		scale(90f * z);
		beginShape(QUADS);

		fill(0, 1, 1);
		vertex(-1, 1, 1);
		fill(1, 1, 1);
		vertex(1, 1, 1);
		fill(1, 0, 1);
		vertex(1, -1, 1);
		fill(0, 0, 1);
		vertex(-1, -1, 1);

		fill(1, 1, 1);
		vertex(1, 1, 1);
		fill(1, 1, 0);
		vertex(1, 1, -1);
		fill(1, 0, 0);
		vertex(1, -1, -1);
		fill(1, 0, 1);
		vertex(1, -1, 1);

		fill(1, 1, 0);
		vertex(1, 1, -1);
		fill(0, 1, 0);
		vertex(-1, 1, -1);
		fill(0, 0, 0);
		vertex(-1, -1, -1);
		fill(1, 0, 0);
		vertex(1, -1, -1);

		fill(0, 1, 0);
		vertex(-1, 1, -1);
		fill(0, 1, 1);
		vertex(-1, 1, 1);
		fill(0, 0, 1);
		vertex(-1, -1, 1);
		fill(0, 0, 0);
		vertex(-1, -1, -1);

		fill(0, 1, 0);
		vertex(-1, 1, -1);
		fill(1, 1, 0);
		vertex(1, 1, -1);
		fill(1, 1, 1);
		vertex(1, 1, 1);
		fill(0, 1, 1);
		vertex(-1, 1, 1);

		fill(0, 0, 0);
		vertex(-1, -1, -1);
		fill(1, 0, 0);
		vertex(1, -1, -1);
		fill(1, 0, 1);
		vertex(1, -1, 1);
		fill(0, 0, 1);
		vertex(-1, -1, 1);

		endShape();
		popMatrix();
	}

	public void draw() {
		/*
		 * TODO: - swipe between shapes (cube, sphere, etc) - rotate in 3D space WITH WHAT? - zoom in/out in 3D space WITH WHAT?
		 * 
		 * Textured cube: http://processing.org/examples/texturecube.html
		 * Globe: http://processing.org/examples/texturesphere.html
		 */
		background(0.5f);

		if (lmController.isConnected()) {
			Frame frame = lmController.frame();
			LOGGER.fine("FPS: " + frame.currentFramesPerSecond());

			Pointable foremost = frame.pointables().frontmost();
			if (foremost.isValid() && frame.hands().count() == 1) {
				Vector translatedPosition = LeapMotionUtil.leapToProcessingVector(this, lmController, foremost.tipPosition());
				pointerX = translatedPosition.getX();
				pointerY = translatedPosition.getY();
				pointerZ = translatedPosition.getZ();
			}

			for (Gesture gesture : frame.gestures()) {
				if (gesture.type() == Gesture.Type.TYPE_SWIPE) {
					LOGGER.info("Swipe gesture detected.");
					SwipeGesture swipe = new SwipeGesture(gesture);
					if (swipe.direction().getX() > 0) {
						// Swiped right.
						incrementShape();
					} else if (swipe.direction().getX() < 0) {
						// Swiped left.
						decrementShape();
					} else {
						LOGGER.info("NOT SURE HOW WE GOT HERE.");
					}
				}
			}
		}

		switch (currentShape) {
		case 0:
			drawCube(pointerX, pointerY, pointerZ * Z_AXIS_MULTIPLIER);
			break;
		case 1:
			// TODO: MORE HERE
			break;
		case 2:
			// TODO: MORE HERE
			break;
		}
	}
	
	private void incrementShape() {
		if (currentShape >= SHAPE_COUNT - 1)
			currentShape = 0;
		else
			currentShape++;
		LOGGER.info("Swiping to forward to shape: " + currentShape);
	}

	private void decrementShape() {
		if (currentShape <= 0)
			currentShape = SHAPE_COUNT - 1;
		else
			currentShape--;
		LOGGER.info("Swiping to back to shape: " + currentShape);
	}

	private void cleanup() {
		// TODO: Put all cleanup stuff here!
	}

	public void stop() {
		cleanup();
		super.stop();
	}

	public void exit() {
		cleanup();
		super.exit();
	}
}
