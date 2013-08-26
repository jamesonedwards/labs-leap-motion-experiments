import java.io.File;
import java.util.logging.ConsoleHandler;
import java.util.logging.Logger;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.commons.lang3.exception.ExceptionUtils;

import processing.core.PApplet;
import processing.core.PImage;
import util.ShapeUtil;

import com.leapmotion.leap.Controller;
import com.leapmotion.leap.Frame;
import com.leapmotion.leap.Gesture;
import com.leapmotion.leap.SwipeGesture;
import com.leapmotion.leap.Gesture.State;
import com.leapmotion.leap.Gesture.Type;

/**
 * @author jameson.edwards
 */
public class SimpleSwipeGallery extends PApplet {
	// Logging:
	private final static Logger LOGGER = Logger.getLogger(SimpleSwipeGallery.class.getName() + "Logger");
	private final static int WINDOW_WIDTH = 1280;
	private final static int WINDOW_HEIGHT = 720;
	private final static int FRAME_RATE = 30;
	private final static int SWIPE_WAIT_FRAMES = floor(FRAME_RATE / 4);
	private final static float SWIPE_X_THESHOLD = 0.5f;
	private final static float SWIPE_Y_THESHOLD = 0.3f;
	private final static float ZOOM_INCREMENT = 0.3f;
	private final static float ZOOM_MAX = 5.0f;
	private final static float ZOOM_MIN = 0.1f;

	private static Controller lmController;
	private static float centerX;
	private static float centerY;
	private static String assetPath = System.getProperty("user.dir") + File.separator + "assets" + File.separator + "gallery" + File.separator;
	private static float scale = 1.0f;

	// Carousel of images, as an array.
	private static PImage[] images;
	private static int curImageIndex = 0;
	private static int swipeWaitCnt = SWIPE_WAIT_FRAMES;

	/**
	 * HACK: Get this PApplet to run from command line.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		PApplet.main(SimpleSwipeGallery.class.getName());
	}

	public void setup() {
		// This must be the first line of code in setup():
		size(WINDOW_WIDTH, WINDOW_HEIGHT/* , P2D */);
		frameRate(FRAME_RATE);

		// Set up PApplet.
		centerX = width / 2;
		centerY = height / 2;

		// Setup logging.
		LOGGER.setLevel(java.util.logging.Level.INFO);
		LOGGER.addHandler(new ConsoleHandler());

		try {
			// Disable window resizing.
			ShapeUtil.findFrame(this).setResizable(false);

			// Get images from a directory.
			images = ShapeUtil.loadImages(this, assetPath);

			// Set the default background image.
			background(0);
			if (images.length > 0) {
				curImageIndex = 0;
				LOGGER.info("Showing image " + String.valueOf(curImageIndex + 1) + " of " + String.valueOf(images.length));
			}

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

	public void draw() {
		// Show current image.
		background(0);

		if (images.length > 0) {
			// If a swipe has just happened, wait before the next swipe can be detected.
			if (swipeWaitCnt > 0) {
				swipeWaitCnt--;
			} else {
				if (lmController.isConnected()) {
					Frame frame = lmController.frame();

					// TODO: The swipe gestures don't see to work very well...
					
					for (Gesture gesture : frame.gestures()) {
						if (gesture.type() == Type.TYPE_SWIPE && gesture.state() == State.STATE_STOP) {
							LOGGER.info("Swipe gesture detected.");
							SwipeGesture swipe = new SwipeGesture(gesture);
							if (swipe.direction().getX() > SWIPE_X_THESHOLD) {
								nextImage();
								LOGGER.info("Showing image " + String.valueOf(curImageIndex + 1) + " of " + String.valueOf(images.length));
							} else if (swipe.direction().getX() < -SWIPE_X_THESHOLD) {
								prevImage();
								LOGGER.info("Showing image " + String.valueOf(curImageIndex + 1) + " of " + String.valueOf(images.length));
							} else if (swipe.direction().getY() > SWIPE_Y_THESHOLD) {
								scaleUp();
							} else if (swipe.direction().getY() < -SWIPE_Y_THESHOLD) {
								scaleDown();
							}

							// Reset swipe wait counter.
							swipeWaitCnt = SWIPE_WAIT_FRAMES;
						}
					}
				}
			}

			ShapeUtil.centerImage(this, images[curImageIndex], scale);
		}
	}

	private void scaleUp() {
		if (scale >= ZOOM_MAX - ZOOM_INCREMENT)
			scale = ZOOM_MAX;
		else
			scale += ZOOM_INCREMENT;
		LOGGER.info("Image scale changed to " + String.valueOf(scale));
	}

	private void scaleDown() {
		if (scale <= ZOOM_MIN + ZOOM_INCREMENT)
			scale = ZOOM_MIN;
		else
			scale -= ZOOM_MIN;
		LOGGER.info("Image scale changed to " + String.valueOf(scale));
	}

	private void nextImage() {
		if (images.length > 0) {
			if (curImageIndex >= images.length - 1)
				curImageIndex = 0;
			else
				curImageIndex++;
		}

		scale(0.1f);
	}

	private void prevImage() {
		if (images.length > 0) {
			if (curImageIndex <= 0)
				curImageIndex = images.length - 1;
			else
				curImageIndex--;
		}
	}
}
