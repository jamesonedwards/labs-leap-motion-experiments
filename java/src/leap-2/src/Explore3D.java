import java.util.logging.ConsoleHandler;
import java.util.logging.Logger;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.commons.lang3.exception.ExceptionUtils;

import processing.core.*;
import util.LeapMotionUtil;
import util.ShapeUtil;

import com.leapmotion.leap.CircleGesture;
import com.leapmotion.leap.Controller;
import com.leapmotion.leap.Frame;
import com.leapmotion.leap.Gesture;
import com.leapmotion.leap.Pointable;
import com.leapmotion.leap.Gesture.State;
import com.leapmotion.leap.ScreenTapGesture;
import com.leapmotion.leap.SwipeGesture;
import com.leapmotion.leap.Vector;

/**
 * @author jameson.edwards
 */
public class Explore3D extends PApplet {
	// Logging:
	private final static Logger LOGGER = Logger.getLogger(Explore3D.class.getName() + "Logger");
	private final static int WINDOW_WIDTH = 1280;
	private final static int WINDOW_HEIGHT = 720;

	private static Controller lmController;
	private static float projectionMultiplier = 1f;
	private static float xyMultiplier = 1000f;

	private static float xmag, ymag = 0;
	private static float newXmag, newYmag = 0;

	private static float pointerX;
	private static float pointerY;
	
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
			// lmController.enableGesture(Gesture.Type.TYPE_SCREEN_TAP);
			// lmController.enableGesture(Gesture.Type.TYPE_CIRCLE);
			lmController.enableGesture(Gesture.Type.TYPE_SWIPE);

			// Set screen tap config.
			if (lmController.config().setFloat("Gesture.ScreenTap.MinForwardVelocity", 50.0f)
					&& lmController.config().setFloat("Gesture.ScreenTap.HistorySeconds", .1f)
					&& lmController.config().setFloat("Gesture.ScreenTap.MinDistance", 3.0f))
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
	private void drawCube(float x, float y) {
		LOGGER.info("(" + x + ", " + y + ")");
		
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

		scale(90);
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
			}

			for (Gesture gesture : frame.gestures()) {
				switch (gesture.type()) {
				case TYPE_CIRCLE:
					LOGGER.info("Circle gesture detected.");
					break;
				case TYPE_SWIPE:
					LOGGER.info("Swipe gesture detected.");
					SwipeGesture swipe = new SwipeGesture(gesture);
					break;
				case TYPE_SCREEN_TAP:
					LOGGER.info("Screen tap gesture detected.");
					break;
				case TYPE_KEY_TAP:
					LOGGER.info("Key tap gesture detected.");
					break;
				default:
					LOGGER.info("Unknown gesture detected.");
					break;
				}
			}
		}
		
		//drawCube(mouseX, mouseY);
		drawCube(pointerX, pointerY);
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
