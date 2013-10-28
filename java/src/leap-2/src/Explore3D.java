import java.io.File;
import java.util.logging.ConsoleHandler;
import java.util.logging.Logger;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.commons.lang3.exception.ExceptionUtils;

import processing.core.*;
import util.LeapMotionUtil;
import util.ShapeUtil;

import com.leapmotion.leap.Controller;
import com.leapmotion.leap.Frame;
import com.leapmotion.leap.Gesture;
import com.leapmotion.leap.Pointable;
import com.leapmotion.leap.SwipeGesture;
import com.leapmotion.leap.Vector;

/**
 * Sources: http://processing.org/tutorials/transform2d/ http://processing.org/tutorials/p3d/
 * 
 * @author jameson.edwards
 */
public class Explore3D extends PApplet {
	// Logging:
	private final static Logger LOGGER = Logger.getLogger(Explore3D.class.getName() + "Logger");

	// Constants:
	private final static int WINDOW_WIDTH = 1280;
	private final static int WINDOW_HEIGHT = 720;
	private final static float Z_AXIS_MULTIPLIER = 2.5f;
	private final static float SCALE_MIN = 0.4f;
	private final static int SHAPE_COUNT = 2;
	private final static float SWIPE_X_THESHOLD = 0.5f;
	private final static float GLOBE_ROTATION_MULTIPLIER = 0.00005f;

	// Global variables:
	private static Controller lmController;
	private static float xmag, ymag = 0;
	private static float newXmag, newYmag = 0;
	private static float pointerX = 0;
	private static float pointerY = 0;
	private static float pointerZ = 1 / Z_AXIS_MULTIPLIER;
	private static float pPointerX = 0;
	private static float pPointerY = 0;
	private static float pPointerZ = 1 / Z_AXIS_MULTIPLIER;
	private static int currentShape = 0;
	private static String assetPath = System.getProperty("user.dir") + File.separator + "assets" + File.separator;
	private static boolean disableMouse = false;

	// Globe stuff:
	private static PImage bg;
	private static PImage texmap;
	private static int sDetail = 35; // Sphere detail setting
	private static float rotationX = 0;
	private static float rotationY = 0;
	private static float velocityX = 0;
	private static float velocityY = 0;
	private static float globeRadius = 400;
	private static float pushBack = 0;
	private static float[] cx, cz, sphereX, sphereY, sphereZ;
	private static float sinLUT[];
	private static float cosLUT[];
	private static float SINCOS_PRECISION = 0.5f;
	private static int SINCOS_LENGTH = (int) (360.0f / SINCOS_PRECISION);
	private static String globeImgPath = assetPath + "world32k.jpg";

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
		smooth();

		// Setup logging.
		LOGGER.setLevel(java.util.logging.Level.INFO);
		LOGGER.addHandler(new ConsoleHandler());

		try {
			// Disable window resizing.
			ShapeUtil.findFrame(this).setResizable(false);

			// Globe:
			texmap = loadImage(globeImgPath);
			initializeSphere(sDetail);

			// Instantiate the Leap Motion controller.
			lmController = new Controller();

			// Set gestures to track.
			lmController.enableGesture(Gesture.Type.TYPE_SWIPE);
			if (lmController.config().setFloat("Gesture.Swipe.MinLength", 400.0f)
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
		/*
		 * Swipe between shapes (cube, sphere, etc) - rotate in 3D space.
		 * Zoom in/out in 3D space.
		 */
		background(0.5f);

		if (lmController.isConnected()) {
			Frame frame = lmController.frame();
			Frame pFrame = lmController.frame(-1);
			LOGGER.fine("FPS: " + frame.currentFramesPerSecond());

			Pointable foremost = frame.pointables().frontmost();
			if (foremost.isValid() && frame.hands().count() == 1) {
				if (mousePressed || disableMouse) {

					Vector translatedPosition = LeapMotionUtil.leapToProcessingVector(this, lmController, foremost.tipPosition());
					pointerX = translatedPosition.getX();
					pointerY = translatedPosition.getY();
					pointerZ = translatedPosition.getZ();

					// Previous frame.
					if (pFrame.isValid()) {
						Vector pTranslatedPosition = LeapMotionUtil.leapToProcessingVector(this, lmController, pFrame.pointables().frontmost()
								.tipPosition());
						pPointerX = pTranslatedPosition.getX();
						pPointerY = pTranslatedPosition.getY();
						pPointerZ = pTranslatedPosition.getZ();
					}
				}
			}

			for (Gesture gesture : frame.gestures()) {
				if (gesture.type() == Gesture.Type.TYPE_SWIPE) {
					LOGGER.info("Swipe gesture detected.");
					SwipeGesture swipe = new SwipeGesture(gesture);
					if (swipe.direction().getX() > SWIPE_X_THESHOLD) {
						// Swiped right.
						incrementShape();
					} else if (swipe.direction().getX() < -SWIPE_X_THESHOLD) {
						// Swiped left.
						decrementShape();
					} else {
						LOGGER.info("NOT SURE HOW WE GOT HERE.");
					}
				}
			}
		}

		switch (currentShape) {
		case 0:
			drawCube(pointerX, pointerY, pointerZ * Z_AXIS_MULTIPLIER);
			break;
		case 1:
			drawGlobe(pointerX, pointerY, pointerZ * Z_AXIS_MULTIPLIER, pPointerX, pPointerY, pPointerZ * Z_AXIS_MULTIPLIER);
			break;
		}
	}

	public void keyPressed() {
		switch (key) {
		case 'm':
			disableMouse = !disableMouse;
			break;
		}
	}

	/**
	 * Source: http://processing.org/examples/rgbcube.html
	 * 
	 * @param x
	 * @param y
	 */
	private void drawCube(float x, float y, float z) {
		LOGGER.fine("(" + x + ", " + y + ", " + z + ")");

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

		// scale(90);
		// Control scaling with the z coordinate (with lower zoom limit).
		z = z < SCALE_MIN ? SCALE_MIN : z;
		scale(90f * z);
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

	/**
	 * Source: http://processing.org/examples/texturesphere.html
	 * 
	 * @param x
	 * @param y
	 * @param z
	 */
	private void drawGlobe(float x, float y, float z, float px, float py, float pz) {
		renderGlobe(x, y, z, px, py, pz);
	}

	private void incrementShape() {
		if (currentShape >= SHAPE_COUNT - 1)
			currentShape = 0;
		else
			currentShape++;
		LOGGER.info("Swiping to forward to shape: " + currentShape);
	}

	private void decrementShape() {
		if (currentShape <= 0)
			currentShape = SHAPE_COUNT - 1;
		else
			currentShape--;
		LOGGER.info("Swiping to back to shape: " + currentShape);
	}

	// Globe stuff:
	private void initializeSphere(int res) {
		sinLUT = new float[SINCOS_LENGTH];
		cosLUT = new float[SINCOS_LENGTH];

		for (int i = 0; i < SINCOS_LENGTH; i++) {
			sinLUT[i] = (float) Math.sin(i * DEG_TO_RAD * SINCOS_PRECISION);
			cosLUT[i] = (float) Math.cos(i * DEG_TO_RAD * SINCOS_PRECISION);
		}

		float delta = (float) SINCOS_LENGTH / res;
		float[] cx = new float[res];
		float[] cz = new float[res];

		// Calc unit circle in XZ plane
		for (int i = 0; i < res; i++) {
			cx[i] = -cosLUT[(int) (i * delta) % SINCOS_LENGTH];
			cz[i] = sinLUT[(int) (i * delta) % SINCOS_LENGTH];
		}

		// Computing vertexlist vertexlist starts at south pole
		int vertCount = res * (res - 1) + 2;
		int currVert = 0;

		// Re-init arrays to store vertices
		sphereX = new float[vertCount];
		sphereY = new float[vertCount];
		sphereZ = new float[vertCount];
		float angle_step = (SINCOS_LENGTH * 0.5f) / res;
		float angle = angle_step;

		// Step along Y axis
		for (int i = 1; i < res; i++) {
			float curradius = sinLUT[(int) angle % SINCOS_LENGTH];
			float currY = -cosLUT[(int) angle % SINCOS_LENGTH];
			for (int j = 0; j < res; j++) {
				sphereX[currVert] = cx[j] * curradius;
				sphereY[currVert] = currY;
				sphereZ[currVert++] = cz[j] * curradius;
			}
			angle += angle_step;
		}
		sDetail = res;
	}

	private void renderGlobe(float x, float y, float z, float px, float py, float pz) {
		pushMatrix();
		translate(width * 0.33f, height * 0.5f, pushBack);
		pushMatrix();
		noFill();
		stroke(255, 200);
		strokeWeight(2);
		// smooth();
		popMatrix();
		lights();
		pushMatrix();
		rotateX(radians(-rotationX));
		rotateY(radians(270 - rotationY));
		fill(200);
		noStroke();
		textureMode(IMAGE);
		texturedSphere(globeRadius, texmap);
		popMatrix();
		popMatrix();
		rotationX += velocityX;
		rotationY += velocityY;
		velocityX *= 0.95f;
		velocityY *= 0.95f;

		// Replace this with Leap motions.
		// Implements mouse control (interaction will be inverse when sphere is upside down)
		// if (mousePressed) {
		// velocityX += (mouseY - pmouseY) * 0.01;
		// velocityY -= (mouseX - pmouseX) * 0.01;
		// }

		velocityX += (y - py) * GLOBE_ROTATION_MULTIPLIER;
		velocityY -= (x - px) * GLOBE_ROTATION_MULTIPLIER;
	}

	// Generic routine to draw textured sphere
	private void texturedSphere(float r, PImage t) {
		int v1, v11, v2;
		r = (r + 240) * 0.33f;
		beginShape(TRIANGLE_STRIP);
		texture(t);
		float iu = (float) (t.width - 1) / (sDetail);
		float iv = (float) (t.height - 1) / (sDetail);
		float u = 0, v = iv;
		for (int i = 0; i < sDetail; i++) {
			vertex(0, -r, 0, u, 0);
			vertex(sphereX[i] * r, sphereY[i] * r, sphereZ[i] * r, u, v);
			u += iu;
		}
		vertex(0, -r, 0, u, 0);
		vertex(sphereX[0] * r, sphereY[0] * r, sphereZ[0] * r, u, v);
		endShape();

		// Middle rings
		int voff = 0;
		for (int i = 2; i < sDetail; i++) {
			v1 = v11 = voff;
			voff += sDetail;
			v2 = voff;
			u = 0;
			beginShape(TRIANGLE_STRIP);
			texture(t);
			for (int j = 0; j < sDetail; j++) {
				vertex(sphereX[v1] * r, sphereY[v1] * r, sphereZ[v1++] * r, u, v);
				vertex(sphereX[v2] * r, sphereY[v2] * r, sphereZ[v2++] * r, u, v + iv);
				u += iu;
			}

			// Close each ring
			v1 = v11;
			v2 = voff;
			vertex(sphereX[v1] * r, sphereY[v1] * r, sphereZ[v1] * r, u, v);
			vertex(sphereX[v2] * r, sphereY[v2] * r, sphereZ[v2] * r, u, v + iv);
			endShape();
			v += iv;
		}
		u = 0;

		// Add the northern cap
		beginShape(TRIANGLE_STRIP);
		texture(t);
		for (int i = 0; i < sDetail; i++) {
			v2 = voff + i;
			vertex(sphereX[v2] * r, sphereY[v2] * r, sphereZ[v2] * r, u, v);
			vertex(0, r, 0, u, v + iv);
			u += iu;
		}
		vertex(sphereX[voff] * r, sphereY[voff] * r, sphereZ[voff] * r, u, v);
		endShape();
	}

	/**
	 * Put all cleanup stuff here.
	 */
	private void cleanup() {

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
