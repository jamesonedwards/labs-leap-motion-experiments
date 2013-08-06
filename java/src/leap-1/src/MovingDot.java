import com.leapmotion.*;
import com.leapmotion.leap.*;
import processing.core.PApplet;

import java.util.logging.ConsoleHandler;
import java.util.logging.Logger;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.commons.lang3.exception.ExceptionUtils;

import util.*;

public class MovingDot extends PApplet {
	// Logging:
	private final static Logger LOGGER = Logger.getLogger("MovingDotLogger");

	private static Controller controller;
	private static float centerX;
	private static float centerY;

	// private static float radius;

	/**
	 * HACK: Get this PApplet to run from command line.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		PApplet.main(MovingDot.class.getName());
	}

	public void setup() {
		// This must be the first line of code in setup():
		size(1000, 600);
		// Set up PApplet.
		centerX = width / 2;
		centerY = height / 2;
		background(0, 0, 0);

		// Setup logging.
		LOGGER.setLevel(java.util.logging.Level.INFO);
		LOGGER.addHandler(new ConsoleHandler());

		try {
			controller = new Controller();
		} catch (Exception ex) {
			String msg = "Exception details: \nType: " + ex.getClass().toString() + "\nMessage: " + ex.getMessage() + "\nStack trace: "
					+ ExceptionUtils.getStackTrace(ex) + "\n\n" + "Object state: "
					+ ReflectionToStringBuilder.toString(this, ToStringStyle.MULTI_LINE_STYLE, true, true) + "\n\n";

			LOGGER.severe(msg);
			System.exit(1); // Exit program.
		}
	}

	public void draw() {
		if (controller.isConnected()) {
			Frame frame = controller.frame();
			LOGGER.fine("FPS: " + frame.currentFramesPerSecond());
			background(0, 0, 0);

			Pointable foremost = frame.pointables().frontmost();
			if (foremost.isValid()) {
				float projectionMultiplier = 1f;
				float xyMultiplier = 1000f;
				float radiusMultiplier = 1f;
				Vector direction = foremost.direction();
				Vector projectedDirection = direction.times(projectionMultiplier);

				// Center x axis around center of window.
				float x = centerX + projectedDirection.getX() * xyMultiplier;
				// Center y axis around center of window. Negative since
				// coordinate system is different.
				float y = centerY - projectedDirection.getY() * xyMultiplier;
				// Radius is based on how close the pointer is, with closer values being more negative. Note the lower cap on radius.
				float radiusMin = 50f;
				float z = foremost.tipPosition().getZ();
				float radius = z < radiusMin ? radiusMin : -z * radiusMultiplier;

				// Draw circle.
				ShapeUtil.drawCircle(this, x, y, radius, new int[] { 255, 0, 0 });
			}
		}
	}
}
