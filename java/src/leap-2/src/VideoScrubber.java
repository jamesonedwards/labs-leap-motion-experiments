import java.io.File;
import java.util.logging.ConsoleHandler;
import java.util.logging.Logger;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.commons.lang3.exception.ExceptionUtils;

import processing.core.*;
import processing.video.*;

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
 * Source: http://www.learningprocessing.com/examples/chapter-16/example-16-5/
 * 
 * @author jameson.edwards
 */
public class VideoScrubber extends PApplet {
	// Logging:
	private final static Logger LOGGER = Logger.getLogger(VideoScrubber.class.getName() + "Logger");
	private final static int WINDOW_WIDTH = 1280;
	private final static int WINDOW_HEIGHT = 720;

	private static Controller lmController;
	private static float centerX;
	private static float centerY;
	private static float projectionMultiplier = 1f;
	private static float xyMultiplier = 1000f;
	private static Movie movie;
	private static boolean moviePlaying = true;
	private static float movieVolumeIncrement = 0.05f;
	private static float movieVolume = 0.5f; // Set initial movie volume to 50%.
	private static int movieFrameRate = 29; // Set the frame rate for this app to that of the movie.
	private static String assetPath = System.getProperty("user.dir") + File.separator + "assets" + File.separator;
	private static String videoFilePath = assetPath + "video-sample-1.mp4";

	/**
	 * HACK: Get this PApplet to run from command line.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		PApplet.main(VideoScrubber.class.getName());
	}

	public void setup() {
		// This must be the first line of code in setup():
		size(WINDOW_WIDTH, WINDOW_HEIGHT/* , P2D */);
		frameRate(movieFrameRate);
		// Set up PApplet.
		centerX = width / 2;
		centerY = height / 2;

		// Setup logging.
		LOGGER.setLevel(java.util.logging.Level.INFO);
		LOGGER.addHandler(new ConsoleHandler());

		try {
			// Disable window resizing.
			findFrame().setResizable(false);

			// TODO: Load a video file (make this interactive?).
			if (!new File(videoFilePath).exists())
				throw new Exception("File doesn't exist: " + videoFilePath);
			movie = new Movie(this, videoFilePath);
			movie.loop();

			// Set initial movie volume to 50%, and keep track of volume in separate param.
			movie.volume(movieVolume);

			// Instantiate the Leap Motion controller.
			lmController = new Controller();

			// Set gestures to track.
			// lmController.enableGesture(Gesture.Type.TYPE_SCREEN_TAP);
			lmController.enableGesture(Gesture.Type.TYPE_CIRCLE);
			// lmController.enableGesture(Gesture.Type.TYPE_SWIPE);

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

	public void draw() {
		background(0);
		if (lmController.isConnected()) {
			Frame frame = lmController.frame();
			LOGGER.fine("FPS: " + frame.currentFramesPerSecond());

			Pointable foremost = frame.pointables().frontmost();
			if (foremost.isValid()) {
				Vector direction = foremost.direction();
				Vector projectedDirection = direction.times(projectionMultiplier);

				// Center x axis around center of window.
				float x = centerX + projectedDirection.getX() * xyMultiplier;
				// Center y axis around center of window. Negative since
				// coordinate system is different.
				//float y = centerY - projectedDirection.getY() * xyMultiplier;
				
				if (moviePlaying) {
					// Draw the video frames.
					// Ratio of mouse X over width
					float ratio = x / (float) width;

					// The jump() function allows you to jump immediately to a point of time within the video.
					// Duration() returns the total length of the movie in seconds.
					movie.jump(ratio * movie.duration());
				}

				// "Play" movie...
				// Read frame
				movie.read();
				// Display frame
				image(movie, 0, 0);

				// Draw vertical bar.
				stroke(0, 255, 0);
				strokeWeight(10);
				line(x, 0, x, height);
				stroke(0);
				strokeWeight(1);
			} else {
				// "Play" movie...
				// Read frame
				movie.read();
				// Display frame
				image(movie, 0, 0);
			}

			for (Gesture gesture : frame.gestures()) {
				switch (gesture.type()) {
				case TYPE_CIRCLE:
					LOGGER.info("Circle gesture detected.");
					CircleGesture circle = new CircleGesture(gesture);
					// Calculate clock direction using the angle between circle normal and pointable.
					if (circle.pointable().direction().angleTo(circle.normal()) <= Math.PI / 4) {
						// Clockwise if angle is less than 90 degrees.
						raiseVolume();
					} else {
						lowerVolume();
					}
					break;
				case TYPE_SWIPE:
					LOGGER.info("Swipe gesture detected.");
					SwipeGesture swipe = new SwipeGesture(gesture);
					break;
				case TYPE_SCREEN_TAP:
					LOGGER.info("Screen tap gesture detected.");
					ScreenTapGesture screenTap = new ScreenTapGesture(gesture);
					togglePlay();
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
	}

	/*
	 * Basic play controls.
	 */
	public void keyPressed() {
		switch (key) {
		case 'p':
			togglePlay();
			break;
		case 'u':
			raiseVolume();
			break;
		case 'd':
			lowerVolume();
			break;
		}
	}

	private void raiseVolume() {
		movieVolume = movieVolume < 1 - movieVolumeIncrement ? movieVolume + movieVolumeIncrement : 1;
		LOGGER.info("RaiseVolume() called setting volume to " + movieVolume);
		movie.volume(movieVolume);
	}

	private void lowerVolume() {
		movieVolume = movieVolume > movieVolumeIncrement ? movieVolume - movieVolumeIncrement : 0;
		LOGGER.info("LowerVolume() called setting volume to " + movieVolume);
		movie.volume(movieVolume);
	}

	private void togglePlay() {
		if (moviePlaying) {
			movie.stop();
		} else {
			movie.play();
		}
		moviePlaying = !moviePlaying;
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
