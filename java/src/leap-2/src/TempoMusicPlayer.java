import java.io.File;
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
import ddf.minim.spi.*; // for AudioRecordingStream
import ddf.minim.ugens.*;

public class TempoMusicPlayer extends PApplet {
	// Logging:
	private final static Logger LOGGER = Logger.getLogger(TempoMusicPlayer.class.getName() + "Logger");
	private final static int WINDOW_WIDTH = 1000;
	private final static int WINDOW_HEIGHT = 667;
	private final static int FILE_BUFFER_SIZE = 512;
	
	private static Controller lmController;
	private static float centerX;
	private static float centerY;
	private static int waveformMultiplier = 100;
	private static float waveformXOffest;
	private static float waveformYOffest;
	private static float leftRightSpread;
	private static float projectionMultiplier = 1f;
	private static float xyMultiplier = 1000f;
	private static float radiusMultiplier = 1f;
	private static float radiusMin = 5f;
	private static float radiusMax = 50f;
	private static Minim minim;
	private static AudioOutput audioOutput;
	private static FilePlayer filePlayer;
	private static AudioMetaData meta;
	private static TickRate tickRate;
	private static float tickRateRate = 1f;
	private static boolean enableEffects = true;
	private static String assetPath = System.getProperty("user.dir") + File.separator + "assets" + File.separator;
	private final static String audioFilePath = assetPath + "8-DaftPunkGetLuckyfeatPharrellWilliams.mp3";

	/**
	 * HACK: Get this PApplet to run from command line.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		PApplet.main(TempoMusicPlayer.class.getName());
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
			AudioRecordingStream myFile = minim.loadFileStream(audioFilePath, // the file to load
					FILE_BUFFER_SIZE, // Specify 512 for the length of the sample buffers (the default buffer size is 1024).
					true // whether to load it totally into memory or not we say true because the file is short.
					);
			// This opens the file and puts it in the "play" state.
			filePlayer = new FilePlayer(myFile);
			filePlayer.loop();

			// This creates a TickRate UGen with the default playback speed of 1. ie, it will sound as if the file is patched directly to the output.
			tickRate = new TickRate(1.f);
			  
			// Get a line out from Minim. It's important that the file is the same audio format as our output (i.e. same sample rate, number of
			// channels, etc).
			audioOutput = minim.getLineOut();

			// Patch the file player through the effects to the output.
			filePlayer.patch(tickRate).patch(audioOutput);

			// Get the metadata.
			meta = filePlayer.getMetaData();

			// Set positionion elements.
			waveformXOffest = centerX - audioOutput.bufferSize() / 2;
			waveformYOffest = centerY - waveformMultiplier / 2;
			leftRightSpread = waveformMultiplier * 2;
			
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

	private void drawWaveform() {
		/*
		 * We draw the waveform by connecting neighbor values with a line/ We multiply each of the values by 50 because the values in the buffers are
		 * normalized. This means that they have values between -1 and 1. If we don't scale them up our waveform will look more or less like a
		 * straight line.
		 * 
		 * Source: http://code.compartmental.net/tools/minim/quickstart/
		 */
		stroke(255);
		for (int i = 0; i < audioOutput.bufferSize() - 1; i++) {
			line(waveformXOffest + i, waveformMultiplier + audioOutput.left.get(i) * waveformMultiplier + waveformYOffest, i + 1 + waveformXOffest,
					waveformMultiplier + audioOutput.left.get(i + 1) * waveformMultiplier + waveformYOffest);
			line(waveformXOffest + i, leftRightSpread + audioOutput.right.get(i) * waveformMultiplier + waveformYOffest, i + 1 + waveformXOffest,
					leftRightSpread + audioOutput.right.get(i + 1) * waveformMultiplier + waveformYOffest);
		}
	}
	
	public void draw() {
		background(0);
		drawWaveform();
		String titleText = "Title: " + meta.title();
		float titleXOffset = 0;
		float titleYOffset = -waveformMultiplier / 2;
		text(titleText, centerX - textWidth(titleText) / 2 + titleXOffset, centerY + titleYOffset);

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

				// Change the rate control value based on mouse position.
				tickRateRate = map(x, 0, width, 0.0f, 3.0f);
				tickRate.value.setLastValue(tickRateRate);			
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
		
		// Display tempo.
		String tempoText = "Tempo: " + (audioOutput.getTempo() * tickRateRate);
		float tempoXOffset = titleXOffset;
		float tempoYOffset = titleYOffset + 15;
		text(tempoText, centerX - textWidth(tempoText) / 2 + tempoXOffset, centerY + tempoYOffset);
	}

	// TODO: Get this to work.
	private void normalizeGain() {
		LOGGER.info("GAIN: " + audioOutput.getGain());
		if (audioOutput.getGain() > 1) {
			LOGGER.info("LOUD! " + audioOutput.getGain());
			audioOutput.setGain(1);
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
			if (enableEffects) {
				// Disable all effects.
				filePlayer.unpatch(tickRate);
				filePlayer.unpatch(audioOutput);
				filePlayer.patch(audioOutput);
			} else {
				// Enable all effects.
				filePlayer.unpatch(audioOutput);
				filePlayer.patch(tickRate);//.patch(audioOutput);
			}
			enableEffects = !enableEffects;
			break;
		case 'i':
			// Toggle interpolation. With interpolation on, it will sound as a record would when slowed down or sped up
			tickRate.setInterpolation(!tickRate.isInterpolating());
		}
	}

	private void raiseVolume() {
		LOGGER.info("RaiseVolumn() called with current GAIN = " + audioOutput.getGain());
		audioOutput.setGain(audioOutput.getGain() + 0.3f);
	}
	
	private void lowerVolume() {
		LOGGER.info("LowerVolumn() called with current GAIN = " + audioOutput.getGain());
		audioOutput.setGain(audioOutput.getGain() - 0.3f);
	}

	private void pan(Vector pt) {
		float xMapped = map(lmController.frame().interactionBox().normalizePoint(pt).getX(), 0, 1, -1, 1);
		LOGGER.info("Pan() called with current PAN = " + audioOutput.getPan() + " and new PAN = " + xMapped);
		audioOutput.setPan(xMapped);
	}
	
	private void togglePlay() {
		if (filePlayer.isPlaying()) {
			filePlayer.pause();
		} else {
			filePlayer.play();
		}
	}

	private void cleanup() {
		// TODO: Put all cleanup stuff here!
		// Close and stop stuff.
		filePlayer.close();
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
