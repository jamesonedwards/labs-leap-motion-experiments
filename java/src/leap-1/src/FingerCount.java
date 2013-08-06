import com.leapmotion.*;
import com.leapmotion.leap.*;
import processing.core.PApplet;

import java.util.logging.ConsoleHandler;
import java.util.logging.Logger;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.commons.lang3.exception.ExceptionUtils;

import util.*;

public class FingerCount extends PApplet {
	// Logging:
	private final static Logger LOGGER = Logger
			.getLogger("FingerCountLogger");

	private static Controller controller;
	private static float centerX;
	private static float centerY;
	//private static float radius;
	
	/**
	 * HACK: Get this PApplet to run from command line.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		PApplet.main(FingerCount.class.getName());
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
			String msg = "Exception details: \nType: "
					+ ex.getClass().toString()
					+ "\nMessage: "
					+ ex.getMessage()
					+ "\nStack trace: "
					+ ExceptionUtils.getStackTrace(ex)
					+ "\n\n"
					+ "Object state: "
					+ ReflectionToStringBuilder.toString(this,
							ToStringStyle.MULTI_LINE_STYLE, true, true)
					+ "\n\n";

			LOGGER.severe(msg);
			System.exit(1); // Exit program.
		}
	}

	public void draw() {
		if (controller.isConnected()) {
			Frame frame = controller.frame();
			LOGGER.info("FPS: " + frame.currentFramesPerSecond());
			int numFingers = frame.fingers().count();
			LOGGER.info("Num fingers: " + numFingers);
			float x = centerX;
			float y = centerY;
			float radius = 20 * numFingers;
			background(0, 0, 0);
			//ShapeUtil.drawCircle(this, x, y, radius, fill, stroke, strokeWeight, alpha)
			ShapeUtil.drawCircle(this, x, y, radius, new int[] { 255, 0, 0 } );
			
			LOGGER.info("FPS: " + frame.currentFramesPerSecond());
			LOGGER.info("Num fingers: " + frame.fingers().count());
			//GestureList gestures = frame.gestures();
			//if (gestures.count() > 0)
			//	LOGGER.info("Num gestures: " + gestures.count());
		}
	}
}
