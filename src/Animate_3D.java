/*
 * Author: Mitchell Proulx
 * 
 * Purpose: Draw a robot that moves across the screen, using hierarchical
 * transformations to animate the limbs to look realistic. There is also a 
 * spaceship .obj file that launches in the background.
 * 
 */

import javax.swing.*;
import java.awt.event.*;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

import com.jogamp.opengl.*;
import com.jogamp.opengl.awt.*;
import com.jogamp.opengl.glu.*;

public class Animate_3D implements GLEventListener, KeyListener {
	public static final boolean TRACE = false;

	public static final String WINDOW_TITLE = "Mitchell Proulx";
	public static final int INITIAL_WIDTH = 640;
	public static final int INITIAL_HEIGHT = 640;

	public static boolean displayFloor = true, displayRobot = false;
	public static int flameCount = 0;
	public static float[][] flameArray = new float[][] {{0.545f, 0, 0}, {0.698f, 0.133f, 0.133f}, 
		{1,0,0}, {0.863f, 0.078f, 0.235f}, {0.980f, 0.502f, 0.447f}, {1, 0.498f, 0.314f}, 
		{1, 0.388f, 0.278f}, {1, 0.271f, 0}, {1, 0.549f, 0},{1, 0.647f, 0}, {1, 1, 0}};

	// Name of the input file path
	public static final String INPUT_PATH_NAME = "resources/";

	private static final GLU glu = new GLU();

	public static void main(String[] args) {
		final JFrame frame = new JFrame(WINDOW_TITLE);

		frame.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				if (TRACE)
					System.out.println("closing window '" + ((JFrame)e.getWindow()).getTitle() + "'");
				System.exit(0);
			}
		});

		final GLProfile profile = GLProfile.get(GLProfile.GL2);
		final GLCapabilities capabilities = new GLCapabilities(profile);
		final GLCanvas canvas = new GLCanvas(capabilities);
		try {
			Object self = self().getConstructor().newInstance();
			self.getClass().getMethod("setup", new Class[] { GLCanvas.class }).invoke(self, canvas);
			canvas.addGLEventListener((GLEventListener)self);
			canvas.addKeyListener((KeyListener)self);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
		canvas.setSize(INITIAL_WIDTH, INITIAL_HEIGHT);

		frame.getContentPane().add(canvas);
		frame.pack();
		frame.setVisible(true);

		canvas.requestFocusInWindow();

		if (TRACE)
			System.out.println("-> end of main().");
	}

	private static Class<?> self() {
		// This gives us the containing class of a static method 
		return new Object() { }.getClass().getEnclosingClass();
	}

	/*** Instance variables and methods ***/
	private float ar;
	private int projection = 1;
	private int cameraAngle = 0;
	private boolean viewChanged = true;
	private float INTERVAL = 0.005f; // inc/dec t by this amount 60 times a second
	private boolean reverse = false;
	private float t = 0.0f;
	public float startX = -4f;
	public float robotSpeed = 8;

	// ENVIRONMENT
	private Shape floor, flame;

	// ROBOT
	private Shape head, arm, chest, leg;
	public Structure robot = null;
	ComplexStructure dodecahedron = null, sandal = null, shuttle = null;
	public ArrayList<Shape> robotShapes = null;
	public boolean drawShuttle = false;

	// starting position of the robot --> head[0], arm[1], chest[2], leg[3], foot[4]
	public float[][] robotPositions = {{startX, 1.05f, 0}, {startX, 0.8f, -0.15f}, {startX, 0.8f, 0f}, {startX, 0.5f, -0.05f}};

	// for animating the motion of the arms
	public static final int numArms = 2, numLegs = 2, numFeet = 2;
	private static final double PERIOD = 88, SCALE = 50;
	private int index = 90, rotatePos = 0;

	public void updatePosition() {
		// will simulate an oscillation for the legs and arms using the sine function
		index++;
		rotatePos = (int)(Math.sin(index*2*Math.PI/PERIOD)*(SCALE/2) + (SCALE/2)) - 90;
		if (index == PERIOD*2) {
			index = 0;
		}
	}

	public void setup(final GLCanvas canvas) {
		// Called for one-time setup
		if (TRACE)
			System.out.println("-> executing setup()");

		new Timer().scheduleAtFixedRate(new TimerTask() {
			public void run() {
				if (t >= 1) {
					reverse = !reverse;
					t = 0;
				} else {
					t += INTERVAL;
				}
				canvas.repaint();
			}
		}, 1000, 1000/60);

		// Next 3 objects from --> [ http://people.sc.fsu.edu/~jburkardt/data/obj/obj.html ]
		dodecahedron = new ComplexStructure(new Shape[] { new Shape(INPUT_PATH_NAME + "dodecahedron.obj") }, new float[][] {{0.0f, 2.0f, 0.0f}});
		sandal = new ComplexStructure(new Shape[] { new Shape(INPUT_PATH_NAME + "sandal.obj") }, new float[][] {{0.0f, 2.0f, 0.0f}});
		shuttle = new ComplexStructure(new Shape[] { new Shape(INPUT_PATH_NAME + "shuttle.obj") }, new float[][] {{0.0f, 2.0f, 0.0f}});
		
		flame = new Shape(0.025f, 0.25f, 0.025f, 1, 0, 0);
		flame.line_colour = new float[] {0,0,0};
		
		robot = new Structure();
		robotShapes = new ArrayList<Shape>();

		// Shape( width, height, depth, red, green, blue )
		floor = new Shape(3f, 0.05f, 0.25f, 1, 0.84f, 0);
		head = new Shape(0.1f-0.025f, 0.05f+0.015f, 0.05f, 0.45f, 0.82f, 0.28f);
		leg = new Shape(0.05f, 0.15f, 0.05f, 0.7f, 0.82f, 0.5f);
		chest = new Shape(0.07f, 0.2f, 0.1f, 0.2f, 0.2f, 0.8f);
		arm = new Shape(0.04f, 0.1f, 0.05f, 0.67f, 0.55f, 0.41f);

		// head[0], arm[1], chest[2], leg[3]
		robotShapes.add(head);
		robotShapes.add(arm);
		robotShapes.add(chest);
		robotShapes.add(leg);

		for (int i = 0; i < robotShapes.size() && robotShapes.size() == robotPositions.length; i++) {
			robot.addComponent(robotShapes.get(i), robotPositions[i]);
		}
	}

	@Override
	public void init(GLAutoDrawable drawable) {
		// Called when the canvas is (re-)created - use it for initial GL setup
		if (TRACE)
			System.out.println("-> executing init()");

		final GL2 gl = drawable.getGL().getGL2();

		// TODO: Add code here
		gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
		gl.glEnable(GL2.GL_DEPTH_TEST);
		gl.glDepthFunc(GL2.GL_LEQUAL);
		gl.glEnable(GL2.GL_BLEND);
		gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);
	}

	@Override
	public void display(GLAutoDrawable drawable) {
		// Draws the display
		if (TRACE)
			System.out.println("-> executing display()");

		final GL2 gl = drawable.getGL().getGL2();

		gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);

		if (viewChanged) {
			gl.glMatrixMode(GL2.GL_PROJECTION);
			gl.glLoadIdentity();

			if (0 == projection) {
				gl.glOrthof(ar < 1 ? -1.0f : -ar, ar < 1 ? 1.0f : ar, ar > 1 ? -1.0f : -1/ar, ar > 1 ? 1.0f : 1/ar, 1.0f, 4.0f);
			} else {
				gl.glFrustumf(ar < 1 ? -1.0f : -ar, ar < 1 ? 1.0f : ar, ar > 1 ? -1.0f : -1/ar, ar > 1 ? 1.0f : 1/ar, 1.0f, 4.0f);
			}

			gl.glMatrixMode(GL2.GL_MODELVIEW);
			gl.glLoadIdentity();

			if (cameraAngle == 0 && projection == 0) {
				gl.glTranslatef(0.0f, -0.5f, -2f - 0.3f * cameraAngle);
				gl.glRotatef(15.0f * cameraAngle, 0.0f, 0.0f, 1.0f);
				gl.glRotatef(30.0f * cameraAngle, 1.0f, 0.0f, 0.0f);
				gl.glScalef(0.3f, 0.3f, 0.3f);
			}else if (cameraAngle == 1 && projection == 0) {
				gl.glTranslatef(0, -0.25f, -2f - 0.3f * cameraAngle);
				gl.glRotatef(75.0f * cameraAngle, 0.0f, 1.0f, 0.0f);
				gl.glRotatef(30.0f * cameraAngle, 0.0f, 0.0f, 1.0f);
				gl.glScalef(0.4f, 0.4f, 0.4f);
			}else if (cameraAngle == 2 && projection == 0) {
				gl.glTranslatef(-0.15f, -0.6f, -2f - 0.3f * cameraAngle);
				gl.glRotatef(-60.0f * cameraAngle, 0.0f, 1.0f, 0.0f);
				gl.glRotatef(-10.0f * cameraAngle, 0.0f, 0.0f, 1.0f);
				gl.glRotatef(-10.0f * cameraAngle, 1.0f, 0.0f, 0f);
				gl.glScalef(0.35f, 0.35f, 0.35f);
			}else if (cameraAngle == 1 && projection == 1) {
				gl.glTranslatef(0.5f, 0, -2f - 0.3f * cameraAngle);
				gl.glRotatef(30.0f * cameraAngle, 0.0f, 0.0f, 1.0f);
				gl.glRotatef(60.0f * cameraAngle, 1.0f, 0.0f, 0.0f);
				gl.glRotatef(25.0f * cameraAngle, 0.0f, 1.0f, 0.0f);
				gl.glScalef(0.6f, 0.6f, 0.6f);
			}else if (cameraAngle == 2 && projection == 1) {
				gl.glTranslatef(0.0f, 0, -2f - 0.3f * cameraAngle);
				gl.glRotatef(30 * cameraAngle, 1.0f, 0.0f, 0.0f);
				gl.glRotatef(0 * cameraAngle, 0.0f, 0.0f, 1.0f);
				gl.glScalef(0.7f, 0.7f, 0.7f);
			}else {
				gl.glTranslatef(0.0f, -0.5f, -2f - 0.3f * cameraAngle);
				gl.glRotatef(15.0f * cameraAngle, 0.0f, 0.0f, 1.0f);
				gl.glRotatef(60.0f * cameraAngle, 1.0f, 0.0f, 0.0f);
				gl.glScalef(0.5f, 0.5f, 0.5f);
			}
			viewChanged = false;
		}

		boolean changeColor = false;
		int change = 0;
		// draw the checker board floor
		for (int z = -3; z < 4; z++) {
			if (changeColor) {
				change = 1;
			}else{
				change = 0;
			}
			gl.glPushMatrix();
			gl.glScalef(0.125f, 1, 1);
			gl.glTranslatef(-25, 0, z*0.5f);
			floor.setColor(0+change, 0+change, 0+change);
			floor.line_colour = new float[] {0.2f, 0.2f, 0.2f};
			floor.draw(gl);
			for (int i = 0; i < 8; i++){
				gl.glTranslatef(6,0,0);
				for (int j = 0; j < 8; j++){
					if ( i % 2 == 0 ) {
						//color = white;
						floor.setColor(1-change, 1-change, 1-change);
						floor.draw(gl);
					} else {
						//color = black;
						floor.setColor(0+change, 0+change, 0+change);
						floor.draw(gl);
					}
				}
			}	
			if (!changeColor) {
				changeColor = true;
			}else{
				changeColor = false;
			}
			gl.glPopMatrix();
		}

		// draw a space shuttle
		gl.glPushMatrix();
		gl.glTranslatef(0, 10*t*t, -0.75f);
		gl.glTranslatef(0, 0.65f, -0.75f);
		gl.glRotatef(-90, 1, 0, 0);
		gl.glRotatef(90, 0, 1, 0);
		gl.glScalef(0.1f, 0.05f, 0.1f);
		drawShuttle = true;
		shuttle.draw(gl);
		drawShuttle = false;
		gl.glPopMatrix();

		// draw the flames for the space shuttle
		for (int m = 0; m < 5; m++) {
			gl.glPushMatrix();
			if (m == 1) {
				gl.glTranslatef(0, 10.2f*t*t, -1.6f-(m*0.025f));
				gl.glScalef(1.25f, -4.8f*t*t, 1);
			}else if (m == 2){
				gl.glTranslatef(0, 10.1f*t*t, -1.6f+(m*0.0025f));
				gl.glScalef(1.25f, -4.9f*t*t, 1);
			}else if (m == 3) {
				gl.glTranslatef(0.05f, 10*t*t, -1.65f);
				gl.glScalef(1.25f, -5f*t*t, 1);
			}else if (m == 4) {
				gl.glTranslatef(-0.025f, 9.9f*t*t, -1.65f);
				gl.glScalef(1.25f, -5.1f*t*t, 1);
			}else{
				gl.glTranslatef(0, 9.8f*t*t, -1.65f);
				gl.glScalef(1.25f, -5.2f*t*t, 1);
			}
			gl.glTranslatef(0,(float)Math.random(),0);
			flame.setColor(flameArray[flameCount][0], flameArray[flameCount][1], flameArray[flameCount][2]);
			flame.draw(gl);
			flameCount = (flameCount+1) % flameArray.length;
			gl.glPopMatrix();
		}

		// draw the robot moving across the screen
		gl.glPushMatrix();
		gl.glTranslatef(robotSpeed*t, 0, 0);
		drawRobot(gl);
		gl.glPopMatrix();
	}

	public void drawRobot(GL2 gl) {
		updatePosition();				// will oscillate the rotation of the arms and legs
		drawHead(gl);
		drawArms(gl);
		drawLegs(gl);
		drawBody(gl);
	}

	public void drawHead(GL2 gl) {
		// head[0], arm[1], chest[2], leg[3]
		Component head = robot.contents.get(0);

		// HEAD
		gl.glPushMatrix();
		gl.glTranslatef(head.position[0], head.position[1], head.position[2]);
		gl.glRotatef(rotatePos*2-90, 0, 1, 0);
		gl.glScalef(1, 1.25f, 1);
		head.shape.setColor(0.2f, 0.84f, 0.2f);
		head.shape.draw(gl);
		
        // GLU SPHERE (connects the helecopter blade to the head of the robot)
		gl.glTranslatef(0, 0.1f, 0);
        gl.glColor3f(0.3f, 0.5f, 1f);
        GLUquadric sphere = glu.gluNewQuadric();
        GLUquadric cylinder = glu.gluNewQuadric();
        glu.gluQuadricDrawStyle(sphere, GLU.GLU_FILL);
        final float radius = 0.05f;
        final int slices = 16;
        final int stacks = 16;
        glu.gluSphere(sphere, radius, slices, stacks);
        glu.gluDeleteQuadric(sphere);
        glu.gluDeleteQuadric(cylinder);

		// HELECOPTER HAT
		gl.glRotatef(rotatePos*16, 0, 1, 0);
		gl.glScalef(3.5f, 0.25f, 0.5f);
		head.shape.setColor(1.0f, 0.2f, 0.2f);
		head.shape.draw(gl);
		
		gl.glPopMatrix();
	}
	public void drawBody(GL2 gl) {
		// head[0], arm[1], chest[2], leg[3]
		Component chest = robot.contents.get(2);

		// CHEST
		gl.glPushMatrix();
		gl.glTranslatef(chest.position[0], chest.position[1], chest.position[2]);
		gl.glScalef(1.5f, 1, 1);
		chest.shape.setColor(0.2f, 0.2f, 0.8f);
		chest.shape.draw(gl);
		gl.glPopMatrix();
	}

	public void drawArms(GL2 gl){
		// head[0], arm[1], chest[2], leg[3]
		Component arm = robot.contents.get(1);
		int changeSide = 1;

		for (int i = 0; i < numArms; i++) {
			gl.glPushMatrix();

			if (i % 1 == 0) {
				changeSide *= -1;	// each odd number change, side of the body
			}
			// UPPER ARM
			gl.glTranslatef(arm.position[0], arm.position[1], changeSide * arm.position[2]);
			if (changeSide < 1) {
				gl.glRotatef(changeSide*rotatePos*2+15, 0, 0, 1);
			}else{
				gl.glRotatef(changeSide*rotatePos*2-15, 0, 0, 1);
			}
			gl.glScalef(1.25f, 1, 1);
			arm.shape.setColor(0.75f, 0.25f, 0.25f);
			arm.shape.draw(gl);

			// LOWER ARM
			if (changeSide < 1) {			// right arm
				gl.glTranslatef(-0.055f, 0.1125f, 0);
				gl.glRotatef(changeSide*rotatePos*2+115, 0, 0, 1);
				gl.glScalef(1, 1.15f, 1);

			}else{							// left arm
				gl.glTranslatef(-0.025f, 0.145f, 0);
				gl.glRotatef(changeSide*rotatePos*2-15, 0, 0, 1);
				gl.glScalef(1, 1.15f, 1);
			}
			arm.shape.setColor(0.75f, 0.25f, 0.25f);
			arm.shape.draw(gl);

			// ROTATING HAND
			gl.glTranslatef(0, -arm.shape.defaultHeight*3, 0);	
			gl.glRotatef(rotatePos*32, 0, 1, 0);
			gl.glScalef(0.05f, 0.1f, 0.05f);
			dodecahedron.draw(gl);

			// 2 FINGERS
			arm.shape.setColor(1, 1, 0);
			gl.glTranslatef(0, arm.shape.defaultHeight*8, 0.25f);	
			gl.glScalef(5f, 5f, 5f);
			arm.shape.line_colour = new float[] {0.5f, 0.5f, 0.5f};
			arm.shape.draw(gl);
			gl.glTranslatef(0,0, -0.25f);
			arm.shape.draw(gl);
			arm.shape.line_colour = new float[] {0.75f, 0.75f, 0.75f};

			gl.glPopMatrix();
		}
	}

	public void drawLegs(GL2 gl){
		// head[0], arm[1], chest[2], leg[3]
		Component leg = robot.contents.get(3);
		int changeSide = 1;
		float shift = 0.3f;

		for (int i = 0; i < numLegs; i++) {
			gl.glPushMatrix();
			if (i % 1 == 0) {
				changeSide *= -1;	// each odd number change, side of the body
			}

			// UPPER LEG
			gl.glTranslatef(leg.position[0], leg.position[1]+shift, changeSide * leg.position[2]);
			if (changeSide < 1) {		// right leg
				gl.glTranslatef( 0, 0, 0.05f);
				gl.glRotatef(rotatePos*2-45, 0, 0, 1);
			}else{						// left leg
				gl.glTranslatef( 0, 0, 0);
				gl.glRotatef(-rotatePos*2+45, 0, 0, 1);
			}
			gl.glTranslatef(0, shift, 0);
			leg.shape.setColor(0.2f, 0.6f, 0.2f);
			leg.shape.draw(gl);

			// LOWER LEG
			if (changeSide < 1) {		// right leg
				gl.glTranslatef(-0.2f + leg.shape.defaultHeight*2, 0.175f, 0);
				gl.glRotatef(rotatePos*2+75, 0, 0, 1);
			}else{						// left leg
				gl.glTranslatef(0.0675f, 0.2f, 0);
				gl.glRotatef(-rotatePos*2+180, 0, 0, 1);
			}
			leg.shape.draw(gl);

			gl.glTranslatef(-0.05f, 0.2f, 0);
			gl.glRotatef(-90, 0, 1, 0);
			if (changeSide < 1) {		// right foot
				gl.glScalef(0.025f, -0.05f, 0.0325f);
			}else{						// left foot
				gl.glScalef(-1*0.025f, -0.05f, 0.0325f);
			}
			sandal.draw(gl);
			gl.glPopMatrix();
		}
	}

	@Override
	public void dispose(GLAutoDrawable drawable) {
		// Called when the canvas is destroyed (reverse anything from init) 
		if (TRACE)
			System.out.println("-> executing dispose()");
	}

	@Override
	public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
		// Called when the canvas has been resized
		// Note: glViewport(x, y, width, height) has already been called so don't bother if that's what you want
		if (TRACE)
			System.out.println("-> executing reshape(" + x + ", " + y + ", " + width + ", " + height + ")");

		final GL2 gl = drawable.getGL().getGL2();
		ar = (float)width / (height == 0 ? 1 : height);
	}

	@Override
	public void keyPressed(KeyEvent e) {
	}

	@Override
	public void keyReleased(KeyEvent e) {
	}

	@Override
	public void keyTyped(KeyEvent e) {
		// TODO Auto-generated method stub
		if (e.getKeyChar() == ' ') {
			cameraAngle++;
			if (cameraAngle == 3) {
				cameraAngle = 0;
				projection = (projection + 1) % 2;
			}
			System.out.println("Camera=" + cameraAngle + " Projection=" + projection + "\n");
			viewChanged = true;
			((GLCanvas)e.getSource()).repaint();
		}
	}
}

class Face {
	private int[] indices;
	private float[] colour;

	public Face(int[] indices, float[] colour) {
		this.indices = new int[indices.length];
		this.colour = new float[colour.length];
		System.arraycopy(indices, 0, this.indices, 0, indices.length);
		System.arraycopy(colour, 0, this.colour, 0, colour.length);
	}

	public void draw(GL2 gl, ArrayList<float[]> vertices, boolean useColour) {
		if (useColour) {
			if (colour.length == 3)
				gl.glColor3f(colour[0], colour[1], colour[2]);
			else
				gl.glColor4f(colour[0], colour[1], colour[2], colour[3]);
		}

		if (indices.length == 1) {
			gl.glBegin(GL2.GL_POINTS);
		} else if (indices.length == 2) {
			gl.glBegin(GL2.GL_LINES);
		} else if (indices.length == 3) {
			gl.glBegin(GL2.GL_TRIANGLES);
		} else if (indices.length == 4) {
			gl.glBegin(GL2.GL_QUADS);
		} else {
			gl.glBegin(GL2.GL_POLYGON);
		}

		for (int i: indices) {
			gl.glVertex3f(vertices.get(i)[0], vertices.get(i)[1], vertices.get(i)[2]);
		}

		gl.glEnd();
	}
}

class Shape {
	// set this to NULL if you don't want outlines
	public float[] line_colour = null;
	public boolean visible = true;
	public float angle = 0;
	protected ArrayList<float[]> vertices;
	protected ArrayList<Face> faces;

	float defaultWidth = 1, defaultHeight = 1, defaultDepth = 1;
	float r = 1, g = 1, b = 1;

	public Shape() {
		init();
	}

	public void setColor(float r, float g, float b) {
		this.r = r;
		this.g = g;
		this.b = b;

		for (int i = 0; i < faces.size(); i ++) {
			faces.remove(i);
		}
		faces.add(new Face(new int[] { 0, 1, 2, 3 }, new float[] { r, g, b } ));
		faces.add(new Face(new int[] { 0, 3, 7, 4 }, new float[] { r, g, b } ));
		faces.add(new Face(new int[] { 7, 6, 5, 4 }, new float[] { r, g, b } ));
		faces.add(new Face(new int[] { 2, 1, 5, 6 }, new float[] { r, g, b } ));
		faces.add(new Face(new int[] { 3, 2, 6, 7 }, new float[] { r, g, b } ));
		faces.add(new Face(new int[] { 1, 0, 4, 5 }, new float[] { r, g, b } ));
	}
	
	public Shape(Shape other) {
		init();
		for (int i = 0; i < other.vertices.size(); i++) {
			this.vertices.add(other.vertices.get(i));
		}
		for (int f = 0; f < other.faces.size(); f++) {
			this.faces.add(other.faces.get(f));
		}
		this.defaultWidth = other.defaultWidth;
		this.defaultHeight = other.defaultHeight;
		this.defaultDepth = other.defaultDepth;
		this.r = other.r;
		this.g = other.g;
		this.b = other.b;
	}

	public Shape(float w, float h, float d, float r, float g, float b) {
		init();
		this.defaultWidth = w;
		this.defaultHeight = h;
		this.defaultDepth = d;
		this.r = r;
		this.g = g;
		this.b = b;

		if (visible) {
			vertices.add(new float[] { -defaultWidth, -defaultHeight, defaultDepth });
			vertices.add(new float[] { defaultWidth, -defaultHeight, defaultDepth });
			vertices.add(new float[] { defaultWidth, defaultHeight, defaultDepth });
			vertices.add(new float[] { -defaultWidth, defaultHeight, defaultDepth });
			vertices.add(new float[] { -defaultWidth, -defaultHeight, -defaultDepth });
			vertices.add(new float[] { defaultWidth, -defaultHeight, -defaultDepth });
			vertices.add(new float[] { defaultWidth, defaultHeight, -defaultDepth });
			vertices.add(new float[] { -defaultWidth, defaultHeight, -defaultDepth });
			
			faces.add(new Face(new int[] { 0, 1, 2, 3 }, new float[] { r, g, b } ));
			faces.add(new Face(new int[] { 0, 3, 7, 4 }, new float[] { r, g, b } ));
			faces.add(new Face(new int[] { 7, 6, 5, 4 }, new float[] { r, g, b } ));
			faces.add(new Face(new int[] { 2, 1, 5, 6 }, new float[] { r, g, b } ));
			faces.add(new Face(new int[] { 3, 2, 6, 7 }, new float[] { r, g, b } ));
			faces.add(new Face(new int[] { 1, 0, 4, 5 }, new float[] { r, g, b } ));
		}
	}

	public Shape(String filename) {
		init();

		// NOTE that there is limited error checking, to make this as flexible as possible
		BufferedReader input;
		String line;
		String[] tokens;

		float[] vertex;
		float[] colour;
		String specifyingMaterial = null;
		String selectedMaterial;
		int[] face;

		HashMap<String, float[]> materials = new HashMap<String, float[]>();
		materials.put("default", new float[] {0.25f,0.25f,1});
		selectedMaterial = "default";

		// vertex positions start at 1
		vertices.add(new float[] {0,0,0});
		int currentColourIndex = 0;
		// these are for error checking (which you don't need to do)
		int lineCount = 0, vertexCount = 0, colourCount = 0, faceCount = 0;

		try {
			input = new BufferedReader(new FileReader(filename));
			line = input.readLine();
			while (line != null) {
				lineCount++;
				tokens = line.split("\\s+");
				if (tokens[0].equals("v")) {
					assert tokens.length == 4 : "Invalid vertex specification (line " + lineCount + "): " + line;
					vertex = new float[3];
					try {
						vertex[0] = Float.parseFloat(tokens[1]);
						vertex[1] = Float.parseFloat(tokens[2]);
						vertex[2] = Float.parseFloat(tokens[3]);
					} catch (NumberFormatException nfe) {
						assert false : "Invalid vertex coordinate (line " + lineCount + "): " + line;
					}
					//System.out.printf("vertex %d: (%f, %f, %f)\n", vertexCount + 1, vertex[0], vertex[1], vertex[2]);
					vertices.add(vertex);
					vertexCount++;
				} else if (tokens[0].equals("newmtl")) {
					assert tokens.length == 2 : "Invalid material name (line " + lineCount + "): " + line;
					specifyingMaterial = tokens[1];
				} else if (tokens[0].equals("Kd")) {
					assert tokens.length == 4 : "Invalid colour specification (line " + lineCount + "): " + line;
					assert faceCount == 0 && currentColourIndex == 0 : "Unexpected (late) colour (line " + lineCount + "): " + line;
					colour = new float[3];
					try {
						colour[0] = Float.parseFloat(tokens[1]);
						colour[1] = Float.parseFloat(tokens[2]);
						colour[2] = Float.parseFloat(tokens[3]);
					} catch (NumberFormatException nfe) {
						assert false : "Invalid colour value (line " + lineCount + "): " + line;
					}
					for (float colourValue: colour) {
						assert colourValue >= 0.0f && colourValue <= 1.0f : "Colour value out of range (line " + lineCount + "): " + line;
					}
					if (specifyingMaterial == null) {
						//System.out.printf("Error: no material name for colour %d: (%f %f %f)\n", colourCount + 1, colour[0], colour[1], colour[2]);
					} else {
						//System.out.printf("material %s: (%f %f %f)\n", specifyingMaterial, colour[0], colour[1], colour[2]);
						materials.put(specifyingMaterial, colour);
					}
					colourCount++;
				} else if (tokens[0].equals("usemtl")) {
					assert tokens.length == 2 : "Invalid material selection (line " + lineCount + "): " + line;
					selectedMaterial = tokens[1];
				} else if (tokens[0].equals("f")) {
					assert tokens.length > 1 : "Invalid face specification (line " + lineCount + "): " + line;
					face = new int[tokens.length - 1];
					try {
						for (int i = 1; i < tokens.length; i++) {
							face[i - 1] = Integer.parseInt(tokens[i].split("/")[0]);
						}
					} catch (NumberFormatException nfe) {
						assert false : "Invalid vertex index (line " + lineCount + "): " + line;
					}
					//System.out.printf("face %d: [ ", faceCount + 1);
					for (int index: face) {
						//System.out.printf("%d ", index);
					}
					//System.out.printf("] using material %s\n", selectedMaterial);
					colour = materials.get(selectedMaterial);
					if (colour == null) {
						//System.out.println("Error: material " + selectedMaterial + " not found, using default.");
						colour = materials.get("default");
					}
					faces.add(new Face(face, colour));
					faceCount++;
				} else {
					//System.out.println("Ignoring: " + line);
				}
				line = input.readLine();
			}
		} catch (IOException ioe) {
			System.out.println(ioe.getMessage());
			assert false : "Error reading input file " + filename;
		}
	}
	protected void init() {
		vertices = new ArrayList<float[]>();
		faces = new ArrayList<Face>();
		line_colour = new float[] { 0.75f, 0.75f, 0.75f };
	}
	public void draw(GL2 gl) {
		for (Face f: faces) {
			if (line_colour == null) {
				f.draw(gl, vertices, true);
			} else {
				gl.glEnable(GL2.GL_POLYGON_OFFSET_FILL);
				gl.glPolygonOffset(1.0f, 1.0f);
				f.draw(gl, vertices, true);
				gl.glDisable(GL2.GL_POLYGON_OFFSET_FILL);

				gl.glPolygonMode(GL2.GL_FRONT_AND_BACK, GL2.GL_LINE);
				gl.glLineWidth(0.75f);
				gl.glColor3f(line_colour[0], line_colour[1], line_colour[2]);
				f.draw(gl, vertices, false);
				gl.glPolygonMode(GL2.GL_FRONT_AND_BACK, GL2.GL_FILL);
			}
		}
	}
}

class Component {
	public Shape shape;
	public float[] position;

	public Component(Shape s, float[] p){
		this.shape = s;
		this.position = p;
	}
}

class Structure {
	
	public ArrayList<Shape> shapes;
	public ArrayList<float[]> positions;
	public ArrayList<Component> contents;

	public Structure() {
		shapes = new ArrayList<Shape>();
		positions = new ArrayList<float[]>();
		contents = new ArrayList<Component>();
	}
	public void addComponent(Shape s, float[] p) {
		this.shapes.add(new Shape(s));
		this.positions.add(new float[] {p[0], p[1], p[2]});
		contents.add(new Component(new Shape(s), new float[] {p[0], p[1], p[2]}));
	}
	public void draw(GL2 gl) {
		for (int i = 0; i < contents.size(); i++) {
			Component c = contents.get(i);
			gl.glPushMatrix();
			gl.glTranslatef(c.position[0], c.position[1], c.position[2]);
			c.shape.draw(gl);
			gl.glPopMatrix();
		}
	}
}

class ComplexStructure extends Shape {
	private Shape[] contents;
	private float[][] positions;

	public ComplexStructure(Shape[] contents, float[][] positions) {
		super();
		init(contents, positions);
	}
	public ComplexStructure(String filename, Shape[] contents, float[][] positions) {
		super(filename);
		init(contents, positions);
	}
	private void init(Shape[] contents, float[][] positions) {
		this.contents = new Shape[contents.length];
		this.positions = new float[positions.length][3];
		System.arraycopy(contents, 0, this.contents, 0, contents.length);
		for (int i = 0; i < positions.length; i++) {
			System.arraycopy(positions[i], 0, this.positions[i], 0, 3);
		}
	}
	public void draw(GL2 gl) {
		super.draw(gl);
		for (int i = 0; i < contents.length; i++) {
			gl.glPushMatrix();
			gl.glTranslatef(positions[i][0], positions[i][1], positions[i][2]);
			contents[i].draw(gl);
			gl.glPopMatrix();
		}
	}
}