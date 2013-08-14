import java.util.logging.ConsoleHandler;
import java.util.logging.Logger;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.commons.lang3.exception.ExceptionUtils;

import processing.core.PApplet;
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

import ddf.minim.*;
import ddf.minim.effects.*;

public class MusicPlayer extends PApplet {
	// Logging:
	private final static Logger LOGGER = Logger.getLogger(MusicPlayer.class.getName() + "Logger");
	private final static int WINDOW_WIDTH = 1000;
	private final static int WINDOW_HEIGHT = 667;
	private final static String ASSET_PATH = "C:\\Dev\\labs-leap-motion-experiments\\java\\src\\leap-2\\assets\\";
	private final static String AUDIO_FILE_PATH = ASSET_PATH + "dnb-sample-1.mp3";

	private static Controller lmController;
	private static float centerX;
	private static float centerY;
	private static float projectionMultiplier = 1f;
	private static float xyMultiplier = 1000f;
	private static float radiusMultiplier = 1f;
	private static float radiusMin = 5f;
	private static float radiusMax = 50f;
	private static Minim minim;
	private static AudioPlayer song;
	private static AudioMetaData meta;
	private static BandPass bpf;

	/**
	 * HACK: Get this PApplet to run from command line.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		PApplet.main(MusicPlayer.class.getName());
	}

	public void setup() {
		// This must be the first line of code in setup():
		size(WINDOW_WIDTH, WINDOW_HEIGHT/* , P2D */);
		// Set up PApplet.
		centerX = width / 2;
		centerY = height / 2;

		// Setup logging.
		LOGGER.setLevel(java.util.logging.Level.INFO);
		LOGGER.addHandler(new ConsoleHandler());

		try {
			// Disable window resizing.
			findFrame().setResizable(false);

			minim = new Minim(this);
			// TODO: Load an audio file (make this interactive?).
			// Specify 512 for the length of the sample buffers (the default buffer size is 1024).
			song = minim.loadFile(AUDIO_FILE_PATH, 512);
			song.loop();
			song.printControls();
			/*
			 * Available controls are:
				  Master Gain, which has a range of 6.0206 to -80.0 and doesn't support shifting.
				  Mute
				  Balance, which has a range of 1.0 to -1.0 and doesn't support shifting.
				  Pan, which has a range of 1.0 to -1.0 and doesn't support shifting.
			 */

			// Make a band pass filter with a center frequency of 440 Hz and a bandwidth of 20 Hz. The third argument is the sample rate of the audio
			// that will be filtered. It is required to correctly compute values used by the filter.
			bpf = new BandPass(440, 20, song.sampleRate());
			song.addEffect(bpf);
			
			// Get the metadata.
			meta = song.getMetaData();
			
			// Instantiate the Leap Motion controller.
			lmController = new Controller();

			// Set gestures to track.
			//lmController.enableGesture(Gesture.Type.TYPE_SCREEN_TAP);
			lmController.enableGesture(Gesture.Type.TYPE_CIRCLE);
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

	public void draw() {
		background(0);
		String titleText = "Title: " + meta.title();
		int waveformMultiplier = 100;
		float waveformXOffest = centerX - song.bufferSize() / 2;
		float waveformYOffest = centerY - waveformMultiplier / 2;
		float leftRightSpread = waveformMultiplier * 2;
		float titleXOffset = 0;
		float titleYOffset = -waveformMultiplier / 2;
		text(titleText, centerX - textWidth(titleText) / 2 + titleXOffset, centerY + titleYOffset);

		/*
		 * We draw the waveform by connecting neighbor values with a line/ We multiply each of the values by 50 because the values in the buffers are
		 * normalized. This means that they have values between -1 and 1. If we don't scale them up our waveform will look more or less like a
		 * straight line.
		 * 
		 * Source: http://code.compartmental.net/tools/minim/quickstart/
		 */
		stroke(255);
		for (int i = 0; i < song.bufferSize() - 1; i++) {
			line(waveformXOffest + i, waveformMultiplier + song.left.get(i) * waveformMultiplier + waveformYOffest, i + 1 + waveformXOffest,
					waveformMultiplier + song.left.get(i + 1) * waveformMultiplier + waveformYOffest);
			line(waveformXOffest + i, leftRightSpread + song.right.get(i) * waveformMultiplier + waveformYOffest, i + 1 + waveformXOffest,
					leftRightSpread + song.right.get(i + 1) * waveformMultiplier + waveformYOffest);
		}

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
				float y = centerY - projectedDirection.getY() * xyMultiplier;
				// Radius is based on how close the pointer is, with closer values being more negative. Note the lower cap on radius.
				float z = foremost.tipPosition().getZ();
				//float radius = z < radiusMin ? radiusMin : -z * radiusMultiplier;
				float radius;
				if (z < radiusMin)
					radius = radiusMin;
				else if (z > radiusMax)
					radius = radiusMax;
				else
					radius = z * radiusMultiplier;
				
				// Draw circle.
				ShapeUtil.drawCircle(this, x, y, radius, new int[] { 255, 0, 0 });
				
				// Map the pointer position to the range [100, 10000], an arbitrary range of passBand frequencies
				// Make sure x isn't zero.
				float passBand = map(x > 0 ? x : 1, 1, width, 100, 2000);
				//float passBand = map(lmController.frame().interactionBox().normalizePoint(projectedDirection).getX(), 0, 1, 100, 10000);
				bpf.setFreq(passBand);
				//float bandWidth = map(y, 0, height, 50, 500);
				float bandWidth = map(radius, radiusMin, radiusMax, 50, 500);
				//LOGGER.info("radius: " + radius + ", bandWidth: " + bandWidth);
				//float bandWidth = 100;
				bpf.setBandWidth(bandWidth);
				// Prints the new values of the coefficients in the console
				bpf.printCoeff();
			}
			
			//normalizeGain();
			
			for (Gesture gesture : frame.gestures()) {
				switch (gesture.type()) {
				case TYPE_CIRCLE:
					LOGGER.info("Circle gesture detected.");
					CircleGesture circle = new CircleGesture(gesture);
					// Calculate clock direction using the angle between circle normal and pointable.
					String clockwiseness;
					if (circle.pointable().direction().angleTo(circle.normal()) <= Math.PI / 4) {
						// Clockwise if angle is less than 90 degrees.
						clockwiseness = "clockwise";
						raiseVolume();
					} else {
						clockwiseness = "counterclockwise";
						lowerVolume();
					}

					// Calculate angle swept since last frame
					double sweptAngle = 0;
					if (circle.state() != State.STATE_START) {
						CircleGesture previousUpdate = new CircleGesture(lmController.frame(1).gesture(circle.id()));
						sweptAngle = (circle.progress() - previousUpdate.progress()) * 2 * Math.PI;
					}

					//LOGGER.info("Circle id: " + circle.id() + ", " + circle.state() + ", progress: " + circle.progress() + ", radius: "
					//		+ circle.radius() + ", angle: " + Math.toDegrees(sweptAngle) + ", " + clockwiseness);
					break;
				case TYPE_SWIPE:
					LOGGER.info("Swipe gesture detected.");
					SwipeGesture swipe = new SwipeGesture(gesture);
					pan(swipe.position());
					//System.out.println("Swipe id: " + swipe.id() + ", " + swipe.state() + ", position: " + swipe.position() + ", direction: "
					//		+ swipe.direction() + ", speed: " + swipe.speed());
					break;
				case TYPE_SCREEN_TAP:
					LOGGER.info("Screen tap gesture detected.");
					ScreenTapGesture screenTap = new ScreenTapGesture(gesture);
					//System.out.println("Screen Tap id: " + screenTap.id() + ", " + screenTap.state() + ", position: " + screenTap.position()
					//		+ ", direction: " + screenTap.direction());
					togglePlay();
					break;
				case TYPE_KEY_TAP:
					LOGGER.info("Key tap gesture detected.");
					/*
					 * KeyTapGesture keyTap = new KeyTapGesture(gesture); System.out.println("Key Tap id: " + keyTap.id() + ", " + keyTap.state() +
					 * ", position: " + keyTap.position() + ", direction: " + keyTap.direction());
					 */
					break;
				default:
					LOGGER.info("Unknown gesture detected.");
					break;
				}
			}
		}
	}

	// TODO: Get this to work.
	private void normalizeGain() {
		LOGGER.info("GAIN: " + song.getGain());
		if (song.getGain() > 1) {
			LOGGER.info("LOUD! " + song.getGain());
			song.setGain(1);
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
		case 'e':
			if (song.isEffected())
				song.noEffects();
			else
				song.effects();
			break;
		}
	}

	private void raiseVolume() {
		LOGGER.info("RaiseVolumn() called with current GAIN = " + song.getGain());
		song.setGain(song.getGain() + 1);
	}
	
	private void lowerVolume() {
		LOGGER.info("LowerVolumn() called with current GAIN = " + song.getGain());
		song.setGain(song.getGain() - 1);
	}

	private void pan(Vector pt) {
		float xMapped = map(lmController.frame().interactionBox().normalizePoint(pt).getX(), 0, 1, -1, 1);
		LOGGER.info("Pan() called with current PAN = " + song.getPan() + " and new PAN = " + xMapped);
		song.setPan(xMapped);
	}
	
	private void panRight() {
		LOGGER.info("PanRight() called with current PAN = " + song.getPan());
		song.setPan(song.getPan() + 0.02f);
	}
	
	private void panLeft() {
		LOGGER.info("PanLeft() called with current PAN = " + song.getPan());
		song.setPan(song.getPan() - 0.02f);
	}
	
	private void togglePlay() {
		if (song.isPlaying()) {
			song.pause();
		} else {
			song.play();
		}
	}

	private void cleanup() {
		// TODO: Put all cleanup stuff here!
		// Close and stop stuff.
		song.close();
		minim.stop();
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
