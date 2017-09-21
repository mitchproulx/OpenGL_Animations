import javax.swing.*;
import java.awt.event.*;
import java.util.Timer;
import java.util.TimerTask;

import com.jogamp.opengl.*;
import com.jogamp.opengl.awt.*;

public class Hierarchical implements GLEventListener {
	public static final boolean TRACE = true;

	public static final String WINDOW_TITLE = "Hierarchical animation";
	public static final int INITIAL_WIDTH = 640;
	public static final int INITIAL_HEIGHT = 640;
	
	public static final boolean UNIT_OUTLINE = true;

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
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
		canvas.setSize(INITIAL_WIDTH, INITIAL_HEIGHT);
		canvas.setAutoSwapBufferMode(true);

		frame.getContentPane().add(canvas);
		frame.pack();
		frame.setVisible(true);

		if (TRACE)
			System.out.println("-> end of main().");
	}

	private static Class<?> self() {
		// This gives us the containing class of a static method 
		return new Object() { }.getClass().getEnclosingClass();
	}

	/*** Instance variables and methods ***/
	float t = 0.0f;

	public void setup(final GLCanvas canvas) {
		// Called for one-time setup
		if (TRACE)
			System.out.println("-> executing setup()");

		new Timer().scheduleAtFixedRate(new TimerTask() {
			public void run() {
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
		// Draws the display
		if (TRACE)
			System.out.println("-> executing display()");

		final GL2 gl = drawable.getGL().getGL2();

		gl.glClear(GL2.GL_COLOR_BUFFER_BIT | GL2.GL_DEPTH_BUFFER_BIT);
		gl.glLoadIdentity();

		// t increment
		final float STEP = 0.004f;
		
		// view transformation
		//		gl.glRotatef(15, 0, 0, 1);
		
		// draw the road
		gl.glPushMatrix();
		gl.glScalef(3.0f, 0.1f, 1);
		gl.glColor3f(0.2f, 0.2f, 0.2f);
		bar(gl);
		gl.glPopMatrix();

		// position the bike and draw it
		gl.glPushMatrix();
		gl.glTranslatef(t * 2.4f - 1.2f, 0.1f, 0);
		gl.glScalef(0.2f, 0.2f, 1);
		drawUnitBike(gl);
		gl.glPopMatrix();

		// position the tree and draw it
		gl.glPushMatrix();
		gl.glTranslatef(0.5f, 0.1f, 0);
		gl.glScalef(0.3f, 0.3f, 1);
		drawUnitTree(gl);
		gl.glPopMatrix();
		
		t += STEP;
		if (t > 1) {
			t = 0;
		}
	}
	
	public void drawUnitBike(GL2 gl) {
		// rear wheel (spins)
		gl.glPushMatrix();
		gl.glTranslatef(-0.6f, -0.2f, 0);
		gl.glScalef(0.4f, 0.4f, 1);
		gl.glRotatef(-360 * t*2, 0, 0, 1);
		gl.glColor3f(0.8f, 0, 0);
		circle(gl);
		gl.glPopMatrix();

		// front wheel (spins)
		gl.glPushMatrix();
		gl.glTranslatef(0.6f, -0.2f, 0);
		gl.glScalef(0.4f, 0.4f, 1);
		gl.glRotatef(-360 * t*2, 0, 0, 1);
		gl.glColor3f(0.8f, 0, 0);
		circle(gl);
		gl.glPopMatrix();
		
		// frame
		gl.glPushMatrix();
		gl.glTranslatef(0.4f, 0.3f, 0);
		gl.glRotatef(20, 0, 0, 1);
		gl.glScalef(0.2f, 0.7f, 1);
		gl.glColor3f(0, 0, 0.8f);
		bar(gl);
		gl.glPopMatrix();
		gl.glPushMatrix();
		gl.glTranslatef(0f, 0.5f, 0);
		gl.glRotatef(90, 0, 0, 1);
		gl.glScalef(0.2f, 0.6f, 1);
		gl.glColor3f(0, 0, 0.8f);
		bar(gl);
		gl.glPopMatrix();
		gl.glPushMatrix();
		gl.glTranslatef(-0.4f, 0.15f, 0);
		gl.glRotatef(-30, 0, 0, 1);
		gl.glScalef(0.2f, 0.5f, 1);
		gl.glColor3f(0, 0, 0.8f);
		bar(gl);
		gl.glPopMatrix();
		
		outline(gl);
	}
	
	public void drawUnitTree(GL2 gl) {
		// trunk
		gl.glPushMatrix();
		gl.glTranslatef(0, -0.4f, 0);
		gl.glScalef(0.2f, 0.6f, 1);
		gl.glColor3f(0.8f, 0.2f, 0.2f);
		bar(gl);
		gl.glPopMatrix();
		
		// leaves
		gl.glPushMatrix();
		gl.glTranslatef(0, 0.5f, 0);
		gl.glScalef(0.5f, 0.5f, 1);
		gl.glColor3f(0, 1.0f, 0);
		circle(gl);
		gl.glPopMatrix();
		
		outline(gl);
	}
	
	public static void bar(GL2 gl) {
		gl.glBegin(GL2.GL_QUADS);
		gl.glVertex2f(-0.5f, 1.0f);
		gl.glVertex2f(-0.5f, -1.0f);
		gl.glVertex2f(0.5f, -1.0f);
		gl.glVertex2f(0.5f, 1.0f);
		gl.glEnd();

		gl.glLineWidth(2.0f);
		gl.glColor3f(1.0f,1.0f,1.0f);
		gl.glBegin(GL2.GL_LINE_LOOP);
		gl.glVertex2f(-0.5f, 1.0f);
		gl.glVertex2f(-0.5f, -1.0f);
		gl.glVertex2f(0.5f, -1.0f);
		gl.glVertex2f(0.5f, 1.0f);
		gl.glEnd();
	}
	
	public static void circle(GL2 gl) {
		final float INC = 0.1f;
		float x, y;

		// fill
		gl.glBegin(GL2.GL_TRIANGLE_FAN);
		gl.glVertex2f(0, 0);
		for (float t = 0.0f; t <= 1.01f; t += INC) { // extra .01f due to rounding error
			x = (float)(Math.cos(2.0f * Math.PI * t));
			y = (float)(Math.sin(2.0f * Math.PI * t));
			gl.glVertex2f(x, y);
		}
		gl.glEnd();

		// outline
		gl.glLineWidth(2.0f);
		gl.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
		gl.glBegin(GL2.GL_LINE_LOOP);
		for (float t = 0.0f; t <= 1.0f; t += INC) {
			x = (float)(Math.cos(2.0f * Math.PI * t));
			y = (float)(Math.sin(2.0f * Math.PI * t));
			gl.glVertex2f(x, y);
		}
		gl.glEnd();

		// show the "wedges"
		gl.glBegin(GL2.GL_LINES);
		for (float t = 0.0f; t <= 1.0f; t += INC) {
			x = (float)(Math.cos(2.0f * Math.PI * t));
			y = (float)(Math.sin(2.0f * Math.PI * t));
			gl.glVertex2f(0.0f, 0.0f);
			gl.glVertex2f(x, y);
		}
		gl.glEnd();
		
		outline(gl);
	}
	
	public static void outline(GL2 gl) {
		if (UNIT_OUTLINE) {
			gl.glPushMatrix();
			gl.glTranslatef(0, 0, 0.5f);
			gl.glLineWidth(1.0f);
			gl.glLineStipple(1, (short)0x0202);
			gl.glEnable(GL2.GL_LINE_STIPPLE);
			gl.glColor3f(1.0f,1.0f,1.0f);
			gl.glBegin(GL2.GL_LINE_LOOP);
			gl.glVertex2f(-1.0f, 1.0f);
			gl.glVertex2f(-1.0f, -1.0f);
			gl.glVertex2f(1.0f, -1.0f);
			gl.glVertex2f(1.0f, 1.0f);
			gl.glEnd();
			gl.glDisable(GL2.GL_LINE_STIPPLE);
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

		final float ar = (float)width / (height == 0 ? 1 : height);

		gl.glMatrixMode(GL2.GL_PROJECTION);
		gl.glLoadIdentity();
		gl.glOrthof(ar < 1 ? -1.0f : -ar, ar < 1 ? 1.0f : ar, ar > 1 ? -1.0f : -1/ar, ar > 1 ? 1.0f : 1/ar, -1.0f, 1.0f);
		gl.glMatrixMode(GL2.GL_MODELVIEW);
		gl.glLoadIdentity();
	}
}
