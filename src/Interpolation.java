import javax.swing.*;
import java.awt.event.*;
import java.util.Timer;
import java.util.TimerTask;

import com.jogamp.opengl.*;
import com.jogamp.opengl.awt.*;

public class Interpolation extends MouseAdapter implements GLEventListener {
	public static final boolean TRACE = true;

	public static final String WINDOW_TITLE = "Interpolation";
	public static final int INITIAL_WIDTH = 640;
	public static final int INITIAL_HEIGHT = 480;

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
			canvas.addMouseListener((MouseListener)self);
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
	private float t = 0.0f;
	private float INTERVAL = 0.005f; // inc/dec t by this amount 60 times a second
	private boolean reverse = false;
	private int level = 0;

	public void setup(final GLCanvas canvas) {
		// Called for one-time setup
		if (TRACE)
			System.out.println("-> executing setup()");

		// TODO: Add code here
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

		// TODO: Add code here
		gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
		gl.glEnable(GL2.GL_DEPTH_TEST);
		
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

		// TODO: Replace this with your drawing code
		
		float start = 0;
		float end = INITIAL_WIDTH;
		float x;
		float ystep = INITIAL_HEIGHT / 6;

		// linear
		gl.glLoadIdentity();
		x = lerp(t, start, end);
		if (reverse) x = end - x;
		gl.glTranslatef(x, ystep * 5, 0);
		drawShape(gl);

		// ease-in
		gl.glLoadIdentity();
		x = lerp(1 - (float)Math.cos(t * Math.PI/2), start, end);
		if (reverse) x = end - x;
		gl.glTranslatef(x, ystep * 4, 0);
		if (level == 1 || level == 6)
			drawShape(gl);

		// ease-out
		gl.glLoadIdentity();
		x = lerp((float)Math.sin(t * Math.PI/2), start, end);
		if (reverse) x = end - x;
		gl.glTranslatef(x, ystep * 3, 0);
		if (level == 2 || level == 6)
			drawShape(gl);

		// ease-in-out
		gl.glLoadIdentity();
		x = lerp((1 - (float)Math.cos(t * Math.PI))/2, start, end);
		if (reverse) x = end - x;
		gl.glTranslatef(x, ystep * 2, 0);
		if (level == 3 || level >= 5)
			drawShape(gl);

		// leveled ease-in-out
		gl.glLoadIdentity();
		x = lerp((1 - (float)Math.cos(t * Math.PI))/4 + t/2, start, end);
		if (reverse) x = end - x;
		gl.glTranslatef(x, ystep * 1, 0);
		if (level >= 4)
			drawShape(gl);
	}
	
	public float lerp(float t, float a, float b) {
		return (1 - t) * a + t * b;
	}
	
	public void drawShape(GL2 gl) {
		gl.glBegin(GL2.GL_QUADS);
		gl.glVertex2f(-8, 8);
		gl.glVertex2f(-8, -8);
		gl.glVertex2f(8, -8);
		gl.glVertex2f(8, 8);
		gl.glEnd();
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

		gl.glMatrixMode(GL2.GL_PROJECTION);
		gl.glLoadIdentity();
		gl.glOrthof(0.0f, width, 0.0f, height, 0.0f, 1.0f);
		gl.glMatrixMode(GL2.GL_MODELVIEW);
		gl.glLoadIdentity();
	}
	
	@Override
	public void mouseClicked(MouseEvent e) {
		level = (level + 1) % 7;
		t = 0;
		reverse = false;
	}
}
