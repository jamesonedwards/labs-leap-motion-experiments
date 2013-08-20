import java.util.logging.ConsoleHandler;
import java.util.logging.Logger;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.commons.lang3.exception.ExceptionUtils;

import processing.core.PApplet;
import util.ShapeUtil;
import util.MinimUtil;
import util.LeapMotionUtil;

import com.leapmotion.leap.Controller;
import com.leapmotion.leap.Frame;
import com.leapmotion.leap.Hand;
import com.leapmotion.leap.Pointable;
import com.leapmotion.leap.Vector;

import ddf.minim.*;
import ddf.minim.signals.SineWave;
import ddf.minim.ugens.*;

public class SimpleTheremin extends PApplet {
	// Logging:
	private final static Logger LOGGER = Logger.getLogger(SimpleTheremin.class.getName() + "Logger");
	private final static int WINDOW_WIDTH = 1600;
	private final static int WINDOW_HEIGHT = 800;
	private final static int FILE_BUFFER_SIZE = 1024;

	private static Controller lmController;
	private static float centerX;
	private static float centerY;
	private static int waveformMultiplier = 100;
	private static float waveformXOffest;
	private static float waveformYOffest;
	private static float leftRightSpread;
	//private static float projectionMultiplier = 1f;
	//private static float xyMultiplier = 1f;
	private static float radiusMultiplier = 1f;
	private static float radiusMin = 5f;
	private static float radiusMax = 5f;
	private static Minim minim;
	private static AudioOutput audioOutput;
	//private static ToneInstrument tone;
	private static SineWave sine;

	/**
	 * HACK: Get this PApplet to run from command line.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		PApplet.main(SimpleTheremin.class.getName());
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
			ShapeUtil.findFrame(this).setResizable(false);

			minim = new Minim(this);

			// Get a line out from Minim.
			audioOutput = minim.getLineOut(Minim.MONO, FILE_BUFFER_SIZE);
			sine = new SineWave(0f, 0.5f, audioOutput.sampleRate()); // here the sine is generated it has A (frequency) = 440Hz , 0.5 amplitude and sample rate
			sine.portamento(100); // set the portamento speed on the oscillator to 200 milliseconds
			audioOutput.addSignal(sine);
			audioOutput.mute();
			
			// Set waveform position elements.
			waveformXOffest = centerX - audioOutput.bufferSize() / 2;
			waveformYOffest = centerY - waveformMultiplier / 2;
			leftRightSpread = waveformMultiplier * 2;

			// Instantiate the Leap Motion controller.
			lmController = new Controller();

			// Set screen tap config.
			/*
			if (lmController.config().setFloat("Gesture.ScreenTap.MinForwardVelocity", 50.0f)
					&& lmController.config().setFloat("Gesture.ScreenTap.HistorySeconds", .1f)
					&& lmController.config().setFloat("Gesture.ScreenTap.MinDistance", 3.0f))
				lmController.config().save();
			*/
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
		MinimUtil.drawWaveform(this, minim, audioOutput, waveformMultiplier, waveformXOffest, waveformYOffest, leftRightSpread, new int[] { 255, 255,
				255 });

		if (lmController.isConnected()) {
			Frame frame = lmController.frame();

			if (frame.hands().count() == 2) {
				Hand lHand = frame.hands().leftmost();
				Pointable lPointer = lHand.pointables().rightmost();
				Hand rHand = frame.hands().rightmost();
				Pointable rPointer = rHand.pointables().leftmost();

				if (lPointer.isValid() && rPointer.isValid()) {
					
					// FIXME: FIX HAND PARTITIONING MATH!
					
					// Transform coordinates from Leap to Processing.
					Vector lPosition = LeapMotionUtil.leapToProcessingVector(this, lmController, lPointer.tipPosition(), LeapMotionUtil.LEFT_HAND);
					Vector rPosition = LeapMotionUtil.leapToProcessingVector(this, lmController, rPointer.tipPosition(), LeapMotionUtil.RIGHT_HAND);

					/*
					// Radius is based on how close the pointer is, with closer values being more negative. Note the lower cap on radius.
					float z = foremost.tipPosition().getZ();
					// float radius = z < radiusMin ? radiusMin : -z * radiusMultiplier;
					float radius;
					if (z < radiusMin)
						radius = radiusMin;
					else if (z > radiusMax)
						radius = radiusMax;
					else
						radius = z * radiusMultiplier;
					*/
					
					// FIXME: Get this to play a continuous tone!
					// TRY THIS: https://forum.processing.org/topic/i-want-to-hear-continues-sound-as-long-as-i-paint#25080000001421145
					
					// Play a note based on the pointer positions.
					float frequencyMin = 10f;
					float frequencyMax = 4000f;
					float amplitudeMin = -40f;
					float amplitudeMax = 4f;
					
					//tone = new ToneInstrument(
					//		map(rPosition.getX(), 0, width, frequencyMin, frequencyMax),
					//		map(lPosition.getY(), height, 0, amplitudeMin, amplitudeMax),
					//		audioOutput);
					// Get the note duration, based on the frame rate.
					//float noteDuration = 1 / frame.currentFramesPerSecond();
					//audioOutput.playNote(0f, noteDuration, tone);
					
					sine.setFreq(map(rPosition.getX(), 0, width, frequencyMin, frequencyMax));
					audioOutput.setGain(map(lPosition.getY(), height, 0, amplitudeMin, amplitudeMax));
				    audioOutput.unmute();
					
					// Draw guide markers.
					ShapeUtil.drawCircle(this, lPosition.getX(), lPosition.getY(), radiusMin, new int[] { 255, 0, 0 });
					ShapeUtil.drawCircle(this, rPosition.getX(), rPosition.getY(), radiusMin, new int[] { 0, 255, 0 });
				}
			} else {
				audioOutput.mute();
			}

			LOGGER.info("Num hands detected: " + frame.hands().count());
		}
	}

	private void cleanup() {
		// TODO: Put all cleanup stuff here!
		// Close and stop stuff.
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
}
