/*
 * Author: Mitchell Proulx
 * 
 * Purpose: Draw bubbles using a bubble wand, with bubbles that
 * move proportionally to the speed of the bubble wand and pop at the edges of the screen.
 * Additional features are the directional wind buttons on the bottom of the screen and
 * when the mouse is double clicked, it will spawn a bunch of bubbles on mouse location.
 * 
 */

import javax.swing.*;

import java.awt.Cursor;
import java.awt.Image;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import com.jogamp.opengl.*;
import com.jogamp.opengl.awt.*;

public class BubblePopper implements GLEventListener, MouseListener, MouseMotionListener {
	public static final boolean TRACE = false;

	public static final String WINDOW_TITLE = "A3Q1: Mitchell Proulx";
	public static final int INITIAL_WIDTH = 640;
	public static final int INITIAL_HEIGHT = 480;

	public static int moveCount = 0, moves = 50, colorCounter = 0, iX = 0;
	public static int totalScore = 0, digitScore = 0, tenScore = 0, hundScore = 0, thouScore = 0, achievement = 0;
	public static float currX = 0.0f, currY = 0.0f;
	public static boolean MOVING_RIGHT = false, MOVING_LEFT = false, MOVING_FAST = false, WINNING = false, WIN_OFF = false;
	public static boolean WIND_RIGHT = false, WIND_LEFT = false, WIND_MID = false, MOVING_NORMAL = false, SUPER_FAST = false;

	public static float[] lastFiveX = { 0, 0, 0, 0, 0 };

	public static float [][] colorArray = { 
		{1.0f, 0.0f, 0.0f}, {0.75f, 0.25f, 0.0f}, {0.50f, 0.50f, 0.0f}, {0.25f, 0.75f, 0.0f},
		{0.0f, 1.0f, 0.0f}, {0.0f, 0.75f, 0.25f}, {0.0f, 0.50f, 0.50f}, {0.0f, 0.25f, 0.75f},
		{0.0f, 0.0f, 1.0f}, {0.25f, 0.0f, 0.75f}, {0.50f, 0.0f, 0.50f}, {0.75f, 0.0f, 0.25f}
	};
	public static float [][] randArray = { 
		{0.804f, 0.361f, 0.361f}, {0.863f, 0.078f, 0.235f}, {0.698f, 0.133f, 0.133f}, {1, 0.714f, 0.757f},
		{1f, 0.078f, 0.576f}, {0.780f, 0.082f, 0.522f}, {1f, 0.627f, 0.478f}, {1, 0.388f, 0.278f},
		{1, 0.271f, 0}, {1, 0.647f, 0}, {1, 1, 0f}, {0.980f, 0.980f, 0.824f}, {0.941f, 0.902f, 0.549f}, 
		{0.902f, 0.902f, 0.980f}, {0.867f, 0.627f, 0.867f}, {0.855f, 0.439f, 0.839f}, {1, 0, 1}, 
		{0.576f, 0.439f, 0.859f}, {0.541f, 0.169f, 0.886f}, {0.545f, 0, 0.545f}, {0.416f, 0.353f, 0.804f}, 
		{0.678f, 1, 0.184f}, {0, 1, 0}, {0.196f, 0.804f, 0.196f}, {0.565f, 0.933f, 0.565f}, {0, 0.980f, 0.604f}, 
		{0.180f, 0.545f, 0.341f}, {0, 0.392f, 0}, {0.604f, 0.804f, 0.196f}, {0.4f, 0.804f, 0.667f}, {0.125f, 0.698f, 0.667f}, 
		{0, 1, 1}, {0.686f, 0.933f, 0.933f}, {0.251f, 0.878f, 0.816f}, {0.373f, 0.620f, 0.627f}, 
		{0.275f, 0.510f, 0.706f}, {0.529f, 0.808f, 0.980f}, {0.482f, 0.408f, 0.933f}, {0.255f, 0.412f, 0.882f}, {0, 0, 0.804f}
	};
	public static float [][] blueArray = { 
		{1.0f, 1.0f, 1.0f}, {0.75f, 0.75f, 0.25f}, {0.50f, 0.50f, 0.50f}, 
		{0.25f, 0.25f, 0.75f}, {0.0f, 0.0f, 1.0f} 
	};
	public static float [][] scoreArray = {
		{1.0f, 0.2f, 0.2f}, {0.80f, 0.47f, 0.13f}, {0.2f, 0.2f, 1.0f}, {0.82f, 0.37f, 0.93f}, 
		{1.0f, 0.51f, 0.98f}, {1.0f, 1.0f, 1.0f}, {1.0f, 0.84f, 0.0f}
	};

	public static ArrayList<Bubble> theBubbles = new ArrayList<Bubble>();

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
		capabilities.setDoubleBuffered(true);
		final GLCanvas canvas = new GLCanvas(capabilities);
		try {
			Object self = self().getConstructor().newInstance();
			self.getClass().getMethod("setup", new Class[] { GLCanvas.class }).invoke(self, canvas);
			canvas.addGLEventListener((GLEventListener)self);
			canvas.addMouseListener((MouseListener)self);
			canvas.addMouseMotionListener((MouseMotionListener)self);
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
		canvas.setSize(INITIAL_WIDTH, INITIAL_HEIGHT);
		canvas.setAutoSwapBufferMode(true);

		frame.getContentPane().add(canvas);
		frame.pack();
		frame.setVisible(true);

		// Hide the cursor/pointer
		Toolkit toolkit = Toolkit.getDefaultToolkit();
		Image image = toolkit.getImage("icons/handwriting.gif");
		Cursor c = toolkit.createCustomCursor(image , new Point(frame.getX(), frame.getY()), "img");
		//frame.setCursor(Cursor.getDefaultCursor());
		frame.setCursor(c);

		if (TRACE)
			System.out.println("-> end of main().");
	}

	private static Class<?> self() {
		// This gives us the containing class of a static method 
		return new Object() { }.getClass().getEnclosingClass();
	}
	
	private float t = 0.0f;
	int width, height;
	float left, top, right, bottom;
	long time = 0;
	float[] lastDragPos = null;
	//private float INTERVAL = 0.005f;
	private float INTERVAL = 0.01f; // inc/dec t by this amount 60 times a second
	private boolean reverse = false;

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

	}

	@Override
	public void init(GLAutoDrawable drawable) {
		// Called when the canvas is (re-)created - use it for initial GL setup
		if (TRACE)
			System.out.println("-> executing init()");
		final GL2 gl = drawable.getGL().getGL2();
		gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
		gl.glEnable(GL2.GL_BLEND);
		gl.glBlendFunc(GL2.GL_SRC_ALPHA, GL2.GL_ONE_MINUS_SRC_ALPHA);
	}

	@Override
	public void display(GLAutoDrawable drawable) {
		long delta = 0;
		long now = System.nanoTime();
		if (time != 0 && now - time < 100000000) {
			delta = now - time;
		}
		time = now;

		// Draws the display
		if (TRACE)
			System.out.println("-> executing display()");

		final GL2 gl = drawable.getGL().getGL2();

		gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);

		// Update the world, and draw it	
		drawWindButtons(gl);

		// first draw the bubble wand handle
		float cx = currX, cy = currY+25;
		int num_segments = 20; // number of segments used for circle
		float radius = 25.0f;
		float twicePi = (float) (2.0f * Math.PI);
		float x1 = currX, x2 = currX;
		float y1 = currY, y2 = currY-50.0f;

		// draw the handle of the bubble wand
		gl.glPointSize(3.5f);
		gl.glBegin(GL2.GL_POINTS);
		for (float t = 0.0f; t <= 1.0f; t += 0.02f) {
			gl.glColor3f(colorArray[colorCounter][0], colorArray[colorCounter][1], colorArray[colorCounter][2]);
			float x = x1 - x1 * t + x2 * t;
			float y = y1 - y1 * t + y2 * t;
			gl.glVertex2f(x, y);
			colorCounter = (colorCounter + 1) % colorArray.length;
		}
		gl.glEnd();

		// if dragging the mouse around the screen
		if (lastDragPos != null) {
			gl.glBegin(GL2.GL_POLYGON);
			for(int i = 0; i < num_segments; i++) {
				gl.glColor3f(0.1f, 0.55f, 0.55f);
				float theta = (float) (twicePi * i / num_segments);		// current angle
				float x = (float) (radius * Math.cos(theta));			// adjustment of x component
				float y = (float) (radius * Math.sin(theta));			// adjustment of y component
				gl.glVertex2f((x/2.0f) + cx, y + cy);					// make an ellipse but divide the width in half
			}
			gl.glEnd();

			// depending which direction the mouse is moving, determine the bezier curve length
			if (MOVING_RIGHT) {
				float bx = 0.0f, by = 0.0f;
				float p1x = cx, p1y = cy+25;
				float p2x = cx+50, p2y = cy;
				float p3x = cx, p3y = cy-25;
				if (MOVING_NORMAL){
					p2x = cx+100;
				}else if (MOVING_FAST){
					p2x = cx+125;
				}else if (SUPER_FAST){
					p1y = cy+30;
					p2x = cx+150;
					p3y = cy-30;
				}else{
					MOVING_NORMAL = false;
					MOVING_FAST = false;
					SUPER_FAST = false;
				}

				// draw the PRE-bubble stretching right
				gl.glBegin(GL2.GL_TRIANGLE_FAN);
				gl.glColor3f(0.1f, 0.55f, 0.55f);
				for(float t = 0.0f; t < 1.0f; t += 0.001){
					bx = (float)(Math.pow((1-t), 2.0)*p1x + (2*(1-t)*t*p2x) + (Math.pow(t,2.0)*p3x));
					by = (float)(Math.pow((1-t), 2.0)*p1y + (2*(1-t)*t*p2y) + (Math.pow(t,2.0)*p3y));
					gl.glVertex2f(bx,by);
				}
				gl.glEnd();

			}else if (MOVING_LEFT) {
				float bx = 0.0f, by = 0.0f;
				float p1x = cx, p1y = cy+25; 
				float p2x = cx-50, p2y = cy;
				float p3x = cx, p3y = cy-25;
				if (MOVING_NORMAL){
					p2x = cx-100;
				}else if (MOVING_FAST) {
					p2x = cx-125;
				}else if (SUPER_FAST){
					p1y = cy+30;
					p2x = cx-150;
					p3y = cy-30;
				}else{
					MOVING_NORMAL = false;
					MOVING_FAST = false;
					SUPER_FAST = false;
				}

				// draw the PRE-bubble stretching left
				gl.glBegin(GL2.GL_TRIANGLE_FAN);
				gl.glColor3f(0.1f, 0.55f, 0.55f);
				for(float t = 0.0f; t < 1.0f; t += 0.001){
					bx = (float)(Math.pow((1-t), 2.0)*p1x + (2*(1-t)*t*p2x) + (Math.pow(t,2.0)*p3x));
					by = (float)(Math.pow((1-t), 2.0)*p1y + (2*(1-t)*t*p2y) + (Math.pow(t,2.0)*p3y));
					gl.glVertex2f(bx,by);
				}
				gl.glEnd();
			}else{
				MOVING_RIGHT = false;
				MOVING_LEFT = false;
				MOVING_NORMAL = false;
				MOVING_FAST = false;
				SUPER_FAST = false;
			}
		}

		// draw the bubble wand oval part
		gl.glLineWidth(2.5f);
		gl.glBegin(GL2.GL_LINE_LOOP);
		for(int i = 0; i < num_segments; i++) {
			gl.glColor3f(colorArray[colorCounter][0], colorArray[colorCounter][1], colorArray[colorCounter][2]);
			float theta = (float) (twicePi * i / num_segments);		// current angle
			float x = (float) (radius * Math.cos(theta));			// adjustment of x component
			float y = (float) (radius * Math.sin(theta));			// adjustment of y component
			gl.glVertex2f((x/2.0f) + cx, y + cy);					// make an ellipse but divide the width in half
			colorCounter = (colorCounter + 1) % colorArray.length;
		}
		gl.glEnd();
		gl.glLineWidth(1f);

		if (theBubbles.size() > 0) {
			drawBubbles(gl);
		}
		drawScoreBoard(gl);
		drawNumbers(gl);

		if (WINNING && !WIN_OFF) {
			win(gl);
		}
	}

	public void win(GL2 gl){
		/* If the player scores more than 1500 points (bubble pops) */
		gl.glLineWidth(25);

		// draw W
		gl.glBegin(GL2.GL_LINES);
		// cycle through the color array
		gl.glColor3f(colorArray[colorCounter][0], colorArray[colorCounter][1], colorArray[colorCounter][2]);
		gl.glVertex2f(140, 400);
		gl.glVertex2f(160, 200);
		gl.glVertex2f(160, 200);
		gl.glVertex2f(180, 350);
		gl.glVertex2f(180, 350);
		gl.glVertex2f(200, 200);
		gl.glVertex2f(200, 200);
		gl.glVertex2f(220, 400);
		// draw I
		gl.glVertex2f(250, 375);
		gl.glVertex2f(250, 325);
		gl.glVertex2f(250, 300);
		gl.glVertex2f(250, 200);
		// draw first N
		gl.glVertex2f(280, 200);
		gl.glVertex2f(280, 400);
		gl.glVertex2f(280, 400);
		gl.glVertex2f(320, 200);
		gl.glVertex2f(320, 200);
		gl.glVertex2f(320, 400);
		// draw second N
		gl.glVertex2f(350, 200);
		gl.glVertex2f(350, 400);
		gl.glVertex2f(350, 400);
		gl.glVertex2f(390, 200);
		gl.glVertex2f(390, 200);
		gl.glVertex2f(390, 400);		
		// draw E
		gl.glVertex2f(420, 385);
		gl.glVertex2f(480, 385);
		gl.glVertex2f(420, 215);
		gl.glVertex2f(420, 385);
		gl.glVertex2f(420, 300);
		gl.glVertex2f(465, 300);
		gl.glVertex2f(420, 215);
		gl.glVertex2f(480, 215);
		// draw R
		gl.glVertex2f(500, 200);
		gl.glVertex2f(500, 400);
		gl.glVertex2f(500, 300);
		gl.glVertex2f(550, 200);
		gl.glEnd();

		// draw the curve of the letter R
		gl.glPointSize(15);
		gl.glBegin(GL2.GL_POINTS);
		float p1x = 500, p1y = 400;
		float p2x = 580, p2y = 350;
		float p3x = 500, p3y = 300;
		for(float t = 0.0f; t < 1.0f; t += 0.01){
			float bx = (float)(Math.pow((1-t), 2.0)*p1x + (2*(1-t)*t*p2x) + (Math.pow(t,2.0)*p3x));
			float by = (float)(Math.pow((1-t), 2.0)*p1y + (2*(1-t)*t*p2y) + (Math.pow(t,2.0)*p3y));
			gl.glVertex2f(bx, by);
		}
		gl.glEnd();
	}

	public void drawScoreBoard(GL2 gl) {
		/* Draw values for */
		float minX = width-(width/8)-(2*(width/16)), minY = height-(height/8);
		float maxX = width, maxY = height;

		// draw the score board
		gl.glBegin(GL2.GL_QUADS);
		gl.glPointSize(10);
		gl.glColor3f(0.2f, 0.2f, 0.2f);
		gl.glVertex2f(maxX, maxY);		// TOP RIGHT
		gl.glVertex2f(maxX, minY);		// BOTTOM RIGHT
		gl.glVertex2f(minX, minY);		// BOTTOM LEFT
		gl.glVertex2f(minX, maxY);		// TOP LEFT
		gl.glEnd();

		// draw the separators
		gl.glBegin(GL2.GL_LINES);
		gl.glLineWidth(20);
		if (totalScore >= 1500) {
			gl.glColor3f(1.0f, 0.84f, 0.0f);	// gold
		}else if (totalScore >= 1000) {
			gl.glColor3f(1.0f, 1.0f, 1.0f);		// white
		}else if (totalScore >= 750) {
			gl.glColor3f(1.0f, 0.51f, 0.98f);	// pink
		}else if (totalScore >= 500) {
			gl.glColor3f(0.82f, 0.37f, 0.93f);	// purple
		}else if (totalScore >= 250) {
			gl.glColor3f(0.2f, 0.2f, 1.0f);		// blue
		}else if (totalScore >= 100) {
			gl.glColor3f(0.80f, 0.47f, 0.13f);	// orange
		}else{
			gl.glColor3f(1.0f, 0.2f, 0.2f);		// red
		}
		gl.glVertex2f(maxX-(1*(maxX/16)), maxY);		
		gl.glVertex2f(maxX-(1*(maxX/16)), minY);	
		gl.glVertex2f(maxX-(2*(maxX/16)), maxY);		
		gl.glVertex2f(maxX-(2*(maxX/16)), minY);	
		gl.glVertex2f(maxX-(3*(maxX/16)), maxY);		
		gl.glVertex2f(maxX-(3*(maxX/16)), minY);	
		gl.glVertex2f(minX, maxY);				// TOP LEFT
		gl.glVertex2f(minX, minY);				// BOTTOM LEFT
		gl.glVertex2f(minX, minY);				// BOTTOM LEFT
		gl.glVertex2f(maxX, minY);				// BOTTOM RIGHT
		gl.glEnd();
		gl.glLineWidth(1);
		gl.glPointSize(1);
	}

	public void drawNumbers(GL2 gl) {
		ArrayList<Edge> digit = null, tenDigit = null, hundDigit = null, thouDigit;
		Number thisNumber = new Number();

		gl.glLineWidth(5);
		gl.glBegin(GL2.GL_LINES);
		digit = thisNumber.getNumber(digitScore);
		tenDigit = thisNumber.getNumber(tenScore);
		hundDigit = thisNumber.getNumber(hundScore);
		thouDigit = thisNumber.getNumber(thouScore);

		// display the digit*1 position number
		gl.glColor3f(1.0f, 0.2f, 0.2f);
		for (int j = 0; j < digit.size(); j++) {	
			gl.glVertex2f( digit.get(j).v1.x, digit.get(j).v1.y );
			gl.glVertex2f( digit.get(j).v2.x, digit.get(j).v2.y );
		}
		// display the digit*10 position number
		gl.glColor3f(0.2f, 1.0f, 0.2f);
		for (int k = 0; k < tenDigit.size(); k++) {
			gl.glVertex2f( tenDigit.get(k).v1.x-(width/16), tenDigit.get(k).v1.y );
			gl.glVertex2f( tenDigit.get(k).v2.x-(width/16), tenDigit.get(k).v2.y );
		}
		// display the digit*100 position number
		gl.glColor3f(0.2f, 1.0f, 0.2f);
		for (int n = 0; n < hundDigit.size(); n++) {
			gl.glVertex2f( hundDigit.get(n).v1.x-(2*(width/16)), hundDigit.get(n).v1.y );
			gl.glVertex2f( hundDigit.get(n).v2.x-(2*(width/16)), hundDigit.get(n).v2.y );
		}
		// display the digit*1000 position number
		for (int m = 0; m < thouDigit.size(); m++) {
			gl.glVertex2f( thouDigit.get(m).v1.x-(3*(width/16)), thouDigit.get(m).v1.y );
			gl.glVertex2f( thouDigit.get(m).v2.x-(3*(width/16)), thouDigit.get(m).v2.y );
		}
		gl.glEnd();

		gl.glLineWidth(1);
		gl.glPointSize(1);
	}

	public void drawWindButtons(GL2 gl) {
		float bx = 0.0f, by = 0.0f;

		if (WINNING) {
			gl.glLineWidth(10);
			gl.glBegin(GL2.GL_LINES);
			gl.glColor3f(1, 0.2f, 0.2f);
			gl.glVertex2f(width-(7*(width/8)), height-(height/8));
			gl.glVertex2f(0,height);
			gl.glVertex2f(width-(7*(width/8)), height);
			gl.glVertex2f(0,height-(height/8));
			gl.glEnd();
		}

		/* RIGHT WIND BUTTON = will push all the bubbles on the screen to the right
		width-((width/8)), height/6			// TOP-LEFT
		width, height/6						// TOP-RIGHT
		width, 0							// BOTTOM RIGHT
		width-((width/8)), 0				// BOTTOM LEFT
		 */
		gl.glBegin(GL2.GL_TRIANGLE_FAN);
		if (!WIND_RIGHT || theBubbles.size() == 0) {
			gl.glColor3f(0.2f, 0.2f, 0.8f);
		}else{
			gl.glColor3f(colorArray[colorCounter][0], colorArray[colorCounter][1], colorArray[colorCounter][2]);
		}
		float p1x = width-((width/8)), p2x = (width-((width/8)))-25, p3x = width;
		float p1y = 0, p2y = (height/6)+25, p3y = height/6;
		for(float t = 0.0f; t < 1.0f; t += 0.001){
			bx = (float)(Math.pow((1-t), 2.0)*p1x + (2*(1-t)*t*p2x) + (Math.pow(t,2.0)*p3x));
			by = (float)(Math.pow((1-t), 2.0)*p1y + (2*(1-t)*t*p2y) + (Math.pow(t,2.0)*p3y));
			gl.glVertex2f(bx,by);
		}
		p2x = width + 25;
		p2y = -100;
		for(float t = 0.0f; t < 1.0f; t += 0.001){
			bx = (float)(Math.pow((1-t), 2.0)*p1x + (2*(1-t)*t*p2x) + (Math.pow(t,2.0)*p3x));
			by = (float)(Math.pow((1-t), 2.0)*p1y + (2*(1-t)*t*p2y) + (Math.pow(t,2.0)*p3y));
			gl.glVertex2f(bx,by);
		}
		gl.glEnd();

		// fixed an issue with an uncolored diagonal line
		gl.glBegin(GL2.GL_LINES);
		if (!WIND_RIGHT || theBubbles.size() == 0) {
			gl.glColor3f(0.2f, 0.2f, 0.8f);
		}else{
			gl.glColor3f(colorArray[colorCounter][0], colorArray[colorCounter][1], colorArray[colorCounter][2]);
		}
		gl.glVertex2f(width-((width/8)), 0);				// BOTTOM LEFT
		gl.glVertex2f(width, height/6);						// TOP-RIGHT	
		gl.glEnd();

		// LEFT WIND BUTTON = will push all the bubbles on the screen to the left
		/*
		0, 0								// BOTTOM LEFT
		0, height/6)						// TOP LEFT
		width-(7*(width/8)), height/6		// TOP RIGHT
		width-(7*(width/8)), 0				// BOTTOM RIGHT
		 */
		gl.glBegin(GL2.GL_TRIANGLE_FAN);
		if (!WIND_LEFT || theBubbles.size() == 0) {
			gl.glColor3f(0.8f, 0.2f, 0.2f);
		}else{
			gl.glColor3f(colorArray[colorCounter][0], colorArray[colorCounter][1], colorArray[colorCounter][2]);
		}
		p1x = 0;
		p1y = height/6;
		p2x = (width-(7*(width/8)))+25;
		p2y = (height/6)+25;
		p3x = width-(7*(width/8));
		p3y = 0;
		for(float t = 0.0f; t < 1.0f; t += 0.001){
			bx = (float)(Math.pow((1-t), 2.0)*p1x + (2*(1-t)*t*p2x) + (Math.pow(t,2.0)*p3x));
			by = (float)(Math.pow((1-t), 2.0)*p1y + (2*(1-t)*t*p2y) + (Math.pow(t,2.0)*p3y));
			gl.glVertex2f(bx,by);
		}
		p2x = -25;
		p2y = -100;
		for(float t = 0.0f; t < 1.0f; t += 0.001){
			bx = (float)(Math.pow((1-t), 2.0)*p1x + (2*(1-t)*t*p2x) + (Math.pow(t,2.0)*p3x));
			by = (float)(Math.pow((1-t), 2.0)*p1y + (2*(1-t)*t*p2y) + (Math.pow(t,2.0)*p3y));
			gl.glVertex2f(bx,by);
		}
		gl.glEnd();

		// DRAW THE MIDDLE WIND BUTTON = will push all the bubbles on the screen upwards
		gl.glBegin(GL2.GL_TRIANGLE_FAN);
		if (!WIND_MID || theBubbles.size() == 0) {
			gl.glColor3f(0.2f, 0.8f, 0.2f);
		}else{
			gl.glColor3f(colorArray[colorCounter][0], colorArray[colorCounter][1], colorArray[colorCounter][2]);
		}
		p1x = (width/2)-(width/8);
		p1y = 0;
		p2x = (width/2);
		p2y = (height/8)+50;
		p3x = (width/2)+(width/8);
		p3y = 0;
		for(float t = 0.0f; t < 1.0f; t += 0.001){
			bx = (float)(Math.pow((1-t), 2.0)*p1x + (2*(1-t)*t*p2x) + (Math.pow(t,2.0)*p3x));
			by = (float)(Math.pow((1-t), 2.0)*p1y + (2*(1-t)*t*p2y) + (Math.pow(t,2.0)*p3y));
			gl.glVertex2f(bx,by);
		}
		gl.glEnd();

		gl.glLineWidth(7f);
		gl.glBegin(GL2.GL_LINES);

		// RIGHT WIND ARROW
		gl.glColor3f(0.0f, 0.8f, 0.0f);
		gl.glVertex2f(width-(width/10f), height/12f);
		gl.glVertex2f(width-(width/24f), height/12f);
		gl.glVertex2f(width-(width/16f), height/8f);
		gl.glVertex2f(width-(width/24f), height/12f);
		gl.glVertex2f(width-(width/16f), height/22f);
		gl.glVertex2f(width-(width/24f), height/12f);	

		// LEFT WIND ARROW
		gl.glColor3f(1.0f, 0.9f, 0.5f);
		gl.glVertex2f(width/10f, height/12f);
		gl.glVertex2f(width/24f, height/12f);
		gl.glVertex2f(width/16f, height/8f);
		gl.glVertex2f(width/24f, height/12f);
		gl.glVertex2f(width/16f, height/22f);
		gl.glVertex2f(width/24f, height/12f);	

		// MIDDLE WIND ARROW
		gl.glColor3f(0.0f, 0.0f, 1.0f);
		gl.glVertex2f(width/2, 0);
		gl.glVertex2f(width/2, (height/8)-25);
		gl.glVertex2f((width/2)-(width/32), 15);
		gl.glVertex2f(width/2, (height/8)-25);
		gl.glVertex2f((width/2)+(width/32)+0.5f, 15);
		gl.glVertex2f(width/2, (height/8)-25);	
		gl.glEnd();
		gl.glLineWidth(1f);
	}

	public void drawBubbles(GL2 gl) {
		gl.glMatrixMode(GL2.GL_MODELVIEW);

		boolean explode = false;
		float x = 0.0f, y = 0.0f, theta = 0.0f;
		int bubCount = 0;
		float rightWind = 2.5f, leftWind = 2.5f,  midWind = 4.5f;

		for (int i = 0; i < theBubbles.size(); i++) {
			Bubble bub = theBubbles.get(i);

			float cx = bub.x, cy = bub.y+25;
			int num_segments = 24; 				// number of segments used for circle
			float radius = bub.minRadius;
			float twicePi = (float) (2.0f * Math.PI);

			// set the min and max radius of the bubbles
			if (radius <= 5) {
				radius = bub.minRadius;
			}
			if (radius >= 49) {
				bub.radius = radius;
				radius = 50;
			}
			// gradually grow the bubble to look believable
			if (radius+3 < bub.radius) {
				bub.minRadius += 3;
				radius = bub.minRadius;
			}else if (radius+2 < bub.radius) {
				bub.minRadius += 2;
				radius = bub.minRadius;
			}else if (radius+1 < bub.radius) {
				bub.minRadius++;
				radius = bub.minRadius;
			}else{
				radius = bub.radius;
			}
			gl.glLoadIdentity();

			// calculate how much that bubble will move and if wind is applied
			if (WIND_RIGHT) {
				if (bub.vx < 0) {
					bub.x -= (bub.vx*rightWind);
				}else{
					bub.x += (bub.vx*rightWind);
				}
			}else if (WIND_LEFT) {
				if (bub.vx < 0) {
					bub.x += (bub.vx*leftWind);
				}else{
					bub.x -= (bub.vx*leftWind);
				}
			}else if (WIND_MID) {
				if (bub.vy > 0) {
					bub.y += (bub.vy*midWind);
				}else{
					bub.y -= (bub.vy*midWind);
				}
			}else{
				bub.x += bub.vx;
			}
			bub.y += bub.vy;
			cx = bub.x;
			cy = bub.y;

			// check if the bubble should explode
			if (cx <= radius || cx >= (width-radius-(radius/2))) {
				explode = true;
			}
			if (cy <= radius+25 || cy >= height-25) {
				explode = true;
			}
			if (bub.lifeTime == 0) {
				explode = true;
			}

			//draw the bubble outline
			gl.glLineWidth(2.5f);
			gl.glBegin(GL2.GL_LINE_LOOP);
			gl.glPushMatrix();
			gl.glTranslatef(cx, cy, 0);
			for(int n = 0; n < num_segments; n++) {
				theta = (float) (twicePi * n / num_segments);	// current angle
				x = (float) (radius * Math.cos(theta));			// adjustment of x component
				y = (float) (radius * Math.sin(theta));			// adjustment of y component

				// outline the bubble with different colors depending on the current score
				if (totalScore >= 1500) {
					if (!WIN_OFF) {
						WINNING = true;		// TOGGLES THE WINNER TEXT ON SCREEN
					}
					gl.glColor3f(scoreArray[6][0], scoreArray[6][1], scoreArray[6][2]);
				}else if (totalScore >= 1000) {
					gl.glColor3f(scoreArray[5][0], scoreArray[5][1], scoreArray[5][2]);
				}else if (totalScore >= 750) {
					gl.glColor3f(scoreArray[4][0], scoreArray[4][1], scoreArray[4][2]);
				}else if (totalScore >= 500) {
					gl.glColor3f(scoreArray[3][0], scoreArray[3][1], scoreArray[3][2]);
				}else if (totalScore >= 250) {
					gl.glColor3f(scoreArray[2][0], scoreArray[2][1], scoreArray[2][2]);
				}else if (totalScore >= 100) {
					gl.glColor3f(scoreArray[1][0], scoreArray[1][1], scoreArray[1][2]);
				}else{
					gl.glColor3f(1-(float)(Math.random()), 1-(float)(Math.random()), 1-(float)(Math.random()));
				}
				gl.glVertex2f(x + cx, y + cy);
			}
			gl.glPopMatrix();
			gl.glEnd();

			// draw the interior of the bubble
			gl.glBegin(GL2.GL_TRIANGLE_FAN);
			gl.glPushMatrix();
			gl.glTranslatef(cx, cy, 0);
			int count = bub.rNum;
			for(int j = 0; j < num_segments; j++) {
				theta = (float) (twicePi * j / num_segments);		// current angle
				x = (float) (radius * Math.cos(theta));			// adjustment of x component
				y = (float) (radius * Math.sin(theta));			// adjustment of y component
				if (totalScore >= 2500) {
					gl.glColor3f(0.80f, 0.47f, 0.13f);
				}else{
					gl.glColor3f(randArray[count][0], randArray[count][1], randArray[count][2]);	
				}
				gl.glVertex2f(x + cx, y + cy);
				if (num_segments % 8 == 0) {
					count  = (count + 1) % randArray.length;
				}
			}
			gl.glPopMatrix();
			gl.glEnd();		

			// the bubble has expired, time to explode shoots out lines of random color around a circle
			if (explode) {
				int explosionCount = 0;
				int explosionSegs = 48;
				gl.glPointSize(5);
				gl.glBegin(GL2.GL_POINTS);
				for (int m = 0; m < 96; m++) {
					for(int j = 0; j < explosionSegs; j++) {
						theta = (float) (twicePi * j / explosionSegs);		// current angle
						x = (float) ((radius+m/2) * Math.cos(theta));			// adjustment of x component
						y = (float) ((radius+m/2) * Math.sin(theta));			// adjustment of y component
						gl.glColor3f(colorArray[explosionCount][0], colorArray[explosionCount][1], colorArray[explosionCount][2]);
						gl.glVertex2f(x + cx, y + cy);
						explosionCount = (explosionCount+1) % colorArray.length;
					}
				}
				gl.glEnd();
			}
			if (explode){
				theBubbles.remove(i);			// dont draw the bubble anymore
				explode = false;
				totalScore++;
				if (totalScore >= 9999) {		// in case someone is CRAZY enough to get to the end, restart the score
					totalScore = 0;
				}
				digitScore = totalScore%10;
				tenScore = (totalScore/10)%10;
				hundScore = (totalScore/100)%10;
				thouScore = (totalScore/1000)%10;
			}
			bub.minusTime();
		}
	}

	public void activateDoubleBubble(float cx, float cy) {
		int rounds = 2, num_segments = 12;
		float theta = 0, x = 0, y = 0, dx = 0, dy = 0, addX = 0, addY = 0;
		float radius = 0;
		float twicePi = (float) (2.0f * Math.PI);

		if (WIND_MID) {
			rounds = 4;
			num_segments = 24;
		}
		for (int n = 0; n <= rounds; n++) {
			for(int i = 0; i < num_segments; i++) {
				theta = (float) (twicePi * i / num_segments);		// current angle
				x = (float) ((radius) * Math.cos(theta));			// adjustment of x component
				y = (float) ((radius) * Math.sin(theta));			// adjustment of y component
				dx = 0.5f;
				dy = 0.1f;
				addX = (float) (Math.random()*1.25);
				addY = (float) (Math.random()*1.25);

				if (Math.random() > 0.7) {
					dx = (dx + addX)*-1;
				}else{
					dx = dx + addX;
				}
				if (Math.random() < 0.4) {
					dy = (dy + addY)*-1;
				}else{
					dy = dy + addY;
				}
				radius = (float) ((Math.random()*100)/2);			// ran
				if (radius < 10) {
					radius = 4;
				}
				if (radius > 40) {
					radius /= 2;
				}
				theBubbles.add(new Bubble(x + cx, y + cy + 25, dx, dy, radius));	
			}
		}
	}

	public boolean inButton(float x, float y) {
		/* Checks to see if the point provided (x, y) is in one of the wind buttons */
		boolean result = false;

		/* RIGHT button  */
		if (x >= width-((width/8)) && x <= width) {	// check x bounds
			if (y >= 0 && y <= height/6) {			// check y bounds
				WIND_RIGHT = true;
				result = true;
			}else{
				WIND_RIGHT = false;
			}
		}else{
			WIND_RIGHT = false;
		}

		/* LEFT button */
		if (x >= 0 && x <= width-(7*(width/8))) {	// check x bounds
			if (y >= 0 && y <= height/6) {			// check y bounds
				WIND_LEFT = true;
				result = true;
			}else{
				WIND_LEFT = false;
			}
		}else{
			WIND_LEFT = false;
		}

		/* MIDDLE BUTTON */
		if (x >= (width/2)-(width/8) && x <= (width/2)+(width/8)) {		// check x bounds
			if (y >= 0 && y <= (height/8)+25) {							// check y bounds
				WIND_MID = true;
				result = true;
			}else{
				WIND_MID = false;
			}
		}else{
			WIND_MID = false;
		}
		// BUTTON TO MAKE THE WINNING MESSAGE DISAPPEAR
		if (WINNING) {
			if (x >= 0 && x <= width-(7*(width/8))) {
				// check y bounds
				if (y >= height-(height/8) && y <= height) {
					WINNING = false;
					WIN_OFF = true;
				}
			}
		}
		return result;
	}

	public float lerp(float t, float a, float b) {
		return (1 - t) * a + t * b;
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
		if (TRACE)
			System.out.println("-> executing reshape(" + x + ", " + y + ", " + width + ", " + height + ")");

		final GL2 gl = drawable.getGL().getGL2();

		this.width = width;
		this.height = height;
		// TODO: choose your coordinate system
		//		final float ar = (float)width / (height == 0 ? 1 : height);

		gl.glMatrixMode(GL2.GL_PROJECTION);
		gl.glLoadIdentity();
		//		left = ar < 1 ? -1.0f : -ar;
		//		right = ar < 1 ? 1.0f : ar;
		//		bottom = ar > 1 ? -1.0f : -1/ar;
		//		top = ar > 1 ? 1.0f : 1/ar;
		//		gl.glOrthof(left, right, bottom, top, -1.0f, 1.0f);
		gl.glOrthof(0, width, 0, height, 0.0f, 1.0f);
		gl.glMatrixMode(GL2.GL_MODELVIEW);
		gl.glLoadIdentity();
	}

	// TODO: use this class if you like, or your own
	class Bubble {
		public float x, y, vx, vy, radius, startX, startY, time, minRadius = 5;
		public int rNum, lifeTime = 1;

		public Bubble(float x, float y, float vx, float vy, float radius) {
			this.x = x;
			this.y = y;
			this.vx = vx;
			this.vy = vy;
			this.radius = radius;
			this.startX = x;
			this.startY = y;
			this.time = 0;
			this.lifeTime = (int)(Math.random()*100)*6;
			this.rNum = (int)((Math.random()*100) % randArray.length);
		}
		public void minusTime() {
			/* Used to calculate the remaining life time of the bubble before popping*/
			lifeTime--;
		}
	}
	class Vertex {
		float x, y;
		Vertex(float x, float y){
			this.x = x;
			this.y = y;
		}
		Vertex(Vertex v){
			this.x = v.x;
			this.y = v.y;
		}
	}
	public class Edge {	
		Vertex v1, v2;
		Edge(Vertex v1, Vertex v2) {
			this.v1 = v1;
			this.v2 = v2;
		}
		Edge(Edge e) {
			this.v1 = new Vertex(e.v1);
			this.v2 = new Vertex(e.v2);
		}
	}
	public class Number {
		/* This class is used to display the score board number on the top right of the viewport 
		 * Using a bunch of lines (edge) to create each number */
		float minX = width-width/8, minY = height-height/8;
		float maxX = width, maxY = height;
		float midY = (maxY-(maxY/64) + minY+(maxY/64))/2;

		boolean [] zero = {true, true, true , false, true, true, true};
		boolean [] one = {false, false, true , false, true, false, false};
		boolean [] two = {false, true, true , true, false, true, true};
		boolean [] three = {false, true, true , true, true, true, false};
		boolean [] four = {true, false, true , true, true, false, false};
		boolean [] five = {true, true, false , true, true, true, false};
		boolean [] six = {true, true, false , true, true, true, true};
		boolean [] seven = {false, true, true , false, true, false, false};
		boolean [] eight = {true, true, true , true, true, true, true};
		boolean [] nine = {true, true, true , true, true, false, false};

		boolean [][] numbers = {zero, one, two , three, four, five, six, seven, eight, nine};
		Edge topLeft, roof, topRight, middle, bottomRight, floor, bottomLeft;
		// NUMBER = { [0]topLeft - [1]roof - [2]topRight - [3]middle - [4]bottomRight - [5]floor - [6]bottomLeft }

		public Number(){
			topLeft = new Edge(new Vertex(maxX-(maxX/32)-(maxX/64), maxY-(maxY/64)), new Vertex(maxX-(maxX/32)-(maxX/64), midY));
			roof = new Edge(new Vertex(maxX-(maxX/32)-(maxX/64), maxY-(maxY/64)), new Vertex(maxX-(maxX/64), maxY-(maxY/64)));
			topRight = new Edge(new Vertex(maxX-(maxX/64), maxY-(maxY/64)), new Vertex(maxX-(maxX/64), midY));
			middle = new Edge(new Vertex(maxX-(maxX/64), midY), new Vertex(maxX-(maxX/32)-(maxX/64), midY));
			bottomRight = new Edge(new Vertex(maxX-(maxX/64), midY), new Vertex(maxX-(maxX/64), minY+(minY/64)));
			floor = new Edge(new Vertex(maxX-(maxX/64), minY+(minY/64)), new Vertex(maxX-(maxX/32)-(maxX/64), minY+(minY/64)));
			bottomLeft = new Edge(new Vertex(maxX-(maxX/32)-(maxX/64), minY+(minY/64)), new Vertex(maxX-(maxX/32)-(maxX/64), midY));
		}
		public ArrayList<Edge> getNumber(int i) {
			// will retrieve the lines necessary to draw the number asked for ( i )
			ArrayList<Edge> num = null;
			boolean [] numberInfo = null;

			// get the information about the number
			if (i >= 0 && i <= 9) {	
				numberInfo = numbers[i];
				num = new ArrayList<Edge>();
				for (int b = 0; b < numberInfo.length; b++) {
					if (numberInfo[b]) {
						switch(b){
						case 0:
							num.add(new Edge(topLeft));
							break;
						case 1:
							num.add(new Edge(roof));
							break;
						case 2:
							num.add(new Edge(topRight));
							break;
						case 3:
							num.add(new Edge(middle));
							break;
						case 4:
							num.add(new Edge(bottomRight));
							break;
						case 5:
							num.add(new Edge(floor));
							break;
						case 6:
						default:
							num.add(new Edge(bottomLeft));
							break;
						}
					}
				}
			}
			return num;
		}
	}

	@Override
	public void mouseDragged(MouseEvent e) {
		// TODO Auto-generated method stub
		currX += e.getX() - lastDragPos[0];
		currY -= e.getY() - lastDragPos[1];

		// mouse is moving left, bubbles go right (and vice versa)
		boolean checkX = ((e.getX() - lastDragPos[0]) > 0);	
		float avgX = 0.0f;
		moveCount++;

		// determine which direction the mouse is going
		if ((e.getX() - lastDragPos[0]) > 0) {
			MOVING_RIGHT = false;
			MOVING_LEFT = true;
		}else if((e.getX() - lastDragPos[0]) < 0){
			MOVING_RIGHT = true;
			MOVING_LEFT = false;
		}else{
			MOVING_RIGHT = false;
			MOVING_LEFT = false;
		}
		lastFiveX[iX % lastFiveX.length] = Math.abs(e.getX() - lastDragPos[0]);
		iX++;
		lastDragPos[0] = e.getX();
		lastDragPos[1] = e.getY();

		// calculate the average of the last five X positions
		for (int i = 0; i < lastFiveX.length; i++) {
			avgX += lastFiveX[i];
		}
		avgX /= (float)lastFiveX.length;

		if (moveCount == moves) {
			System.out.println("! BUBBLE !");

			float dx = 0.7f, dy = 0.3f;				// default movement
			float addX = 0, addY = 0;				

			// calculate the movement speed of the bubbles
			// using the average of the last 5 mouse movements
			if (avgX > 40) {				// SUPER FAST
				addX += 0.7*15;
				SUPER_FAST = true;
			}else if (avgX > 35) {			// SUPER FAST
				addX += 0.6*13;
				SUPER_FAST = true;
			}else if (avgX > 30) {			// FAST
				addX += 0.5*11;
				MOVING_FAST = true;
			}else if (avgX > 20) {			// FAST
				addX += 0.4*9;
				MOVING_FAST = true;
			}else if (avgX > 10) {			// NORMAL
				addX += 0.3*7;
				MOVING_NORMAL = true;
			}else if (avgX > 5) {			// NORMAL
				addX += 0.2*5;
				MOVING_NORMAL = true;
			}else {							// SLOW
				addX += 0.1*1;
				MOVING_NORMAL = false;
				MOVING_FAST = false;
				SUPER_FAST = false;
			}

			// give the bubble a random radius between 1-50
			float radius = (float) ((Math.random()*100)/2);

			if (checkX) {
				dx = (dx + addX)*-1;		// will change the X direction of the bubble based on the wand movement
			}else{
				dx = dx + addX;
			}
			if (Math.random() < 0.4) {		// randomly change the Y direction
				dy = (dy + addY)*-1;
			}else{
				dy = dy + addY;
			}
			theBubbles.add(new Bubble(currX, (currY+25), dx, dy, radius));

			// reset the number of moves until the next bubble to a random number
			moveCount = 0;
			moves = (int) ((Math.random()*100)/8);
			while ( moves <= 0 || moves >= 100 ) {		// while the random value is not valid
				moves = (int) ((Math.random()*100)/8);	// get a new 
			}
		}
	}
	@Override
	public void mouseMoved(MouseEvent e) {
		currX = e.getX();
		currY = INITIAL_HEIGHT - e.getY();
		inButton(currX, currY);
	}
	@Override
	public void mouseClicked(MouseEvent e) {
		lastDragPos = new float[] {e.getX(), e.getY()};
	}
	@Override
	public void mousePressed(MouseEvent e) {
		lastDragPos = new float[] {e.getX(), e.getY()};

		/*	DOUBLE CLICK ADDS A NEW EFFECT */
		if (e.getClickCount() == 2) {
			System.out.println("!! DOUBLE BUBBLE !!");
			activateDoubleBubble(currX, currY);
			((GLCanvas)e.getSource()).repaint();
		}
	}
	@Override
	public void mouseReleased(MouseEvent e) {
		//System.out.println("Mouse released on (" + e.getX() + "," + e.getY() + ")");
		lastDragPos = null;
		MOVING_NORMAL = false;
		MOVING_FAST = false;
		SUPER_FAST = false;
		MOVING_RIGHT = false;
		MOVING_LEFT = false;
	}
	@Override
	public void mouseEntered(MouseEvent e) {
	}
	@Override
	public void mouseExited(MouseEvent e) {
	}
}
