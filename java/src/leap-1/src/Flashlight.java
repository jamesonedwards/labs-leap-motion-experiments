import com.leapmotion.leap.*;
import processing.core.PApplet;
import processing.core.PGraphics;
import processing.core.PImage;

import java.util.ArrayList;
import java.util.logging.ConsoleHandler;
import java.util.logging.Logger;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.commons.lang3.exception.ExceptionUtils;

public class Flashlight extends PApplet {
	// Logging:
	private final static Logger LOGGER = Logger.getLogger("FlashlightLogger");
	private final static int WINDOW_WIDTH = 1000;
	private final static int WINDOW_HEIGHT = 667;
	private final static String ASSET_PATH = "C:\\Dev\\labs-leap-motion-experiments\\java\\src\\leap-1\\assets\\";
	private final static String BG_IMAGE_PATH = ASSET_PATH + "bg_image.jpg";
	private final static String MASK_IMAGE_PATH = ASSET_PATH + "mask_image.jpg";

	private static Controller controller;
	private static float centerX;
	private static float centerY;
	private static float projectionMultiplier = 1f;
	private static float xyMultiplier = 1000f;
	private static float radiusMultiplier = 1f;
	private static float radiusMin = 5f;
	private static PImage bgImage;
	private static PImage maskImage;
	private static PGraphics pg;
	private static boolean persistDots = false;
	private static ArrayList<float[]> dots = new ArrayList<float[]>();

	/**
	 * HACK: Get this PApplet to run from command line.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		PApplet.main(Flashlight.class.getName());
	}

	public void setup() {
		// This must be the first line of code in setup():
		size(WINDOW_WIDTH, WINDOW_HEIGHT/* , P2D */);
		// Set up PApplet.
		centerX = width / 2;
		centerY = height / 2;
		pg = createGraphics(width, height/* , P2D */);

		// Setup logging.
		LOGGER.setLevel(java.util.logging.Level.INFO);
		LOGGER.addHandler(new ConsoleHandler());

		try {
			// Disable window resizing.
			findFrame().setResizable(false);

			// Load background and mask images, and resize them to the size of the window.
			int imgBorderHack = 0;
			bgImage = loadImage(BG_IMAGE_PATH);
			if (bgImage == null)
				throw new Exception("In setup(), the PImage for bgImage was null for some reason");
			bgImage.resize(width + imgBorderHack, height + imgBorderHack);
			maskImage = loadImage(MASK_IMAGE_PATH);
			if (maskImage == null)
				throw new Exception("In setup(), the PImage for maskImage was null for some reason");
			maskImage.resize(width + imgBorderHack, height + imgBorderHack);

			// Instantiate the Leap Motion controller.
			controller = new Controller();
		} catch (Exception ex) {
			String msg = "Exception details: \nType: " + ex.getClass().toString() + "\nMessage: " + ex.getMessage() + "\nStack trace: "
					+ ExceptionUtils.getStackTrace(ex) + "\n\n" + "Object state: "
					+ ReflectionToStringBuilder.toString(this, ToStringStyle.MULTI_LINE_STYLE, true, true) + "\n\n";

			LOGGER.severe(msg);
			System.exit(1); // Exit program.
		}
	}

	public void keyPressed() {
		LOGGER.info("Key was pressed: " + key);

		// Determine what to do when certain keys are pressed
		switch (key) {
		case ('p'):
			if (persistDots) {
				// Turn off persistence and reset dot array.
				dots.clear();
				persistDots = false;
			} else {
				persistDots = true;
			}
			break;
		}
	}

	public void draw() {
		if (controller.isConnected()) {
			Frame frame = controller.frame();
			LOGGER.fine("FPS: " + frame.currentFramesPerSecond());

			// Draw the mask image.
			image(maskImage, 0, 0);

			Pointable foremost = frame.pointables().frontmost();
			if (foremost.isValid()) {
				Vector direction = foremost.direction();
				Vector projectedDirection = direction.times(projectionMultiplier);

				// Center x axis around center of window.
				float x = centerX + projectedDirection.getX() * xyMultiplier;
				// Center y axis around center of window. Negative since
				// coordinate system is different.
				float y = centerY - projectedDirection.getY() * xyMultiplier;
				// Radius is based on how close the pointer is, with closer values being more negative. Note the lower cap on radius.
				float z = foremost.tipPosition().getZ();
				float radius = z < radiusMin ? radiusMin : -z * radiusMultiplier;

				/*
				 * Source: http://forum.processing.org/topic/image-behind-image
				 */
				pg.beginDraw();
				pg.background(0, 0);
				pg.noStroke();
				pg.fill(255);
				if (persistDots) {
					dots.add(new float[] { x, y, radius * 2 });
					for (final float[] dot : dots)
						pg.ellipse(dot[0], dot[1], dot[2], dot[2]);
				} else {
					pg.ellipse(x, y, radius * 2, radius * 2);
				}
				pg.endDraw();
				bgImage.mask(pg);
				image(bgImage, 0, 0);
			} else {
				// Reset the saved dots.
				dots.clear();
			}
		}
	}

	/**
	 * Get the current frame. (A hack for Eclipse from https://forum.processing.org/topic/trying-to-use-processing-in-eclipse).
	 * 
	 * @return
	 */
	private java.awt.Frame findFrame() {
		java.awt.Container f = this.getParent();
		while (!(f instanceof java.awt.Frame) && f != null)
			f = f.getParent();
		return (java.awt.Frame) f;
	}
}
