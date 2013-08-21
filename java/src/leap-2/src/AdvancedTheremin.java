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
import ddf.minim.signals.*;

// TODO: "mcgaremin"
public class AdvancedTheremin extends PApplet {
	// Logging:
	private final static Logger LOGGER = Logger.getLogger(AdvancedTheremin.class.getName() + "Logger");
	private final static int WINDOW_WIDTH = 1600;
	private final static int WINDOW_HEIGHT = 800;
	private final static int FILE_BUFFER_SIZE = 1024;
	private final static float FREQUENCY_MIN = 200f;
	// Set max frequency to be a multiple of min frequency so that we get full octaves (sort of).
	private final static float FREQUENCY_MAX = FREQUENCY_MIN * 4;
	private final static float AMPLITUDE_MIN = -40f;
	private final static float AMPLITUDE_MAX = 6f;
	// The portamento speed on the oscillator, in milliseconds.
	private final static int SINE_PORTAMENTO = 50; 

	private static Controller lmController;
	private static float centerX;
	private static float centerY;
	private static int waveformMultiplier = 100;
	private static float waveformXOffest;
	private static float waveformYOffest;
	private static float leftRightSpread;
	private static float radiusMin = 5f;
	private static Minim minim;
	private static AudioOutput audioOutput;
	private static SineWave sineWave;
	private static SineWave sineWave3;
	private static SineWave sineWave5;
	private static TriangleWave triangleWave;
	private static SquareWave squareWave;
	private static boolean enableSineWave = true;
	private static boolean enableTriangleWave = false;
	private static boolean enableSquareWave = false;

	/**
	 * HACK: Get this PApplet to run from command line.
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		PApplet.main(AdvancedTheremin.class.getName());
	}

	public void setup() {
		// This must be the first line of code in setup():
		size(WINDOW_WIDTH, WINDOW_HEIGHT/* , P2D */);
		centerX = width / 2;
		centerY = height / 2;

		// Setup logging.
		LOGGER.setLevel(java.util.logging.Level.SEVERE);
		LOGGER.addHandler(new ConsoleHandler());

		try {
			// Disable window resizing.
			ShapeUtil.findFrame(this).setResizable(false);

			minim = new Minim(this);

			// Get a line out from Minim.
			audioOutput = minim.getLineOut(Minim.MONO, FILE_BUFFER_SIZE);
			/*
			 * Source: https://forum.processing.org/topic/i-want-to-hear-continues-sound-as-long-as-i-paint#25080000001421145
			 */
			// Default amplitude is zero so that it doesn't play until you interact with it.
			sineWave = new SineWave(0f, 0.5f, audioOutput.sampleRate());
			sineWave.portamento(SINE_PORTAMENTO);
			audioOutput.addSignal(sineWave);
			sineWave3 = new SineWave(0f, 0.4f, audioOutput.sampleRate());
			sineWave3.portamento(SINE_PORTAMENTO);
			audioOutput.addSignal(sineWave3);
			sineWave5 = new SineWave(0f, 0.4f, audioOutput.sampleRate());
			sineWave5.portamento(SINE_PORTAMENTO);
			audioOutput.addSignal(sineWave5);
			triangleWave = new TriangleWave(0f, 0.5f, audioOutput.sampleRate());
			triangleWave.portamento(SINE_PORTAMENTO);
			audioOutput.addSignal(triangleWave);
			squareWave = new SquareWave(0f, 0.5f, audioOutput.sampleRate());
			squareWave.portamento(SINE_PORTAMENTO);
			audioOutput.addSignal(squareWave);
			audioOutput.mute();
			
			// Set waveform position elements.
			waveformXOffest = centerX - audioOutput.bufferSize() / 2;
			waveformYOffest = centerY - waveformMultiplier;
			leftRightSpread = waveformMultiplier * 2;

			// Instantiate the Leap Motion controller.
			lmController = new Controller();
		} catch (Exception ex) {
			String msg = "Exception details: \nType: " + ex.getClass().toString() + "\nMessage: " + ex.getMessage() + "\nStack trace: "
					+ ExceptionUtils.getStackTrace(ex) + "\n\n" + "Object state: "
					+ ReflectionToStringBuilder.toString(this, ToStringStyle.MULTI_LINE_STYLE, true, true) + "\n\n";

			LOGGER.severe(msg);
			exit(); // Exit program.
		}
	}

	public void draw() {
		centerX = width / 2;
		centerY = height / 2;
		background(0);
		MinimUtil.drawWaveform(this, minim, audioOutput, waveformMultiplier, waveformXOffest, waveformYOffest, leftRightSpread, new int[] { 255, 255,
				255 });
		
		// Draw a line dividing the window.
		line(centerX, 0, centerX, height);
		
		// Indicate which waves are being used.
		String wavesText = "Waves used: ";
		String harmonicsText = "Hamonics used: root";
		String freqText = "Freq: ";
		String gainText = "Gain: ";

		if (lmController.isConnected()) {
			Frame frame = lmController.frame();

			if (frame.hands().count() == 2) {
				Hand lHand = frame.hands().leftmost();
				Pointable lPointer = lHand.pointables().rightmost();
				Hand rHand = frame.hands().rightmost();
				Pointable rPointer = rHand.pointables().leftmost();

				if (lPointer.isValid() && rPointer.isValid()) {
					// Transform coordinates from Leap to Processing.
					Vector lPosition = LeapMotionUtil.leapToProcessingVector(this, lmController, lPointer.tipPosition(), LeapMotionUtil.LEFT_HAND);
					Vector rPosition = LeapMotionUtil.leapToProcessingVector(this, lmController, rPointer.tipPosition(), LeapMotionUtil.RIGHT_HAND);
					LOGGER.fine("LEFT x: " + lPosition.getX() + ", RIGHT x: " + rPosition.getX());
					
					// Set the frequency based on the right pointer position.
					float freq = map(rPosition.getX(), centerX, width, FREQUENCY_MIN, FREQUENCY_MAX);
					freqText += String.format("%.0f", freq) + " hz";
					if (enableSineWave) {
						sineWave.setFreq(freq);
						wavesText += " sine";
						
						// Experiment: Add harmonics.
						if (rHand.pointables().count() > 1) {
							sineWave3.setFreq(freq * 1.5f);
							harmonicsText += " 3rd";
						} else {
							sineWave3.setFreq(0);
						}
						if (rHand.pointables().count() > 2) {
							sineWave5.setFreq(freq * 2f);
							harmonicsText += " 5th";
						} else {
							sineWave5.setFreq(0);
						}
					} else {
						sineWave.setFreq(0);
						sineWave3.setFreq(0);
						sineWave5.setFreq(0);
					}
					if (enableTriangleWave) {
						triangleWave.setFreq(freq);
						wavesText += " triangle";
					} else {
						triangleWave.setFreq(0);
					}
					if (enableSquareWave) {
						squareWave.setFreq(freq);
						wavesText += " square";
					} else {
						squareWave.setFreq(0);
					}

					// Set the gain based on the left pointer position.
					float gain = map(lPosition.getY(), height, 0, AMPLITUDE_MIN, AMPLITUDE_MAX);
					audioOutput.setGain(gain);
					gainText += String.format("%.0f", gain) + " db";
				    audioOutput.unmute();
					
					// Draw guide markers.
					ShapeUtil.drawCircle(this, lPosition.getX(), lPosition.getY(), radiusMin, new int[] { 255, 0, 0 });
					ShapeUtil.drawCircle(this, rPosition.getX(), rPosition.getY(), radiusMin, new int[] { 0, 255, 0 });
				}
			} else {
				audioOutput.mute();
			}
			
			text(wavesText, 50, 50);
			text(freqText, 50, 70);
			text(gainText, 50, 90);
			text(harmonicsText, 50, 110);
			LOGGER.fine("Num hands detected: " + frame.hands().count());
		}
	}

	public void keyPressed() {
		switch (key) {
		case '1':
			enableSineWave = !enableSineWave;
			break;
		case '2':
			enableTriangleWave = !enableTriangleWave;
			break;
		case '3':
			enableSquareWave = !enableSquareWave;
			break;
		}
	}
	
	private void cleanup() {
		// Put all cleanup stuff here!
		// Close and stop stuff.
		audioOutput.close();
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
