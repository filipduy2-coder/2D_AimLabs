import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

/**
 * Captures all mouse and keyboard events and translates them into
 * GameState mutations. No drawing, no timing — input only.
 */
public class InputHandler implements KeyListener, MouseListener, MouseMotionListener {

    private final GameState state;
    private final JPanel panel;
    private final Robot robot;

    private int centerX;
    private int centerY;
    private boolean recentering = false;

    // Callbacks for UI-level actions (pause menu buttons, restart, leave)
    private final Runnable onRestart;
    private final Runnable onLeave;
    private final Runnable onResume;

    // Rectangles for pause/gameover overlay buttons — set by Renderer each frame
    public Rectangle resumeButton = new Rectangle();
    public Rectangle restartButton = new Rectangle();
    public Rectangle leaveButton = new Rectangle();

    public InputHandler(GameState state, JPanel panel, Robot robot,
                        Runnable onResume, Runnable onRestart, Runnable onLeave) {
        this.state = state;
        this.panel = panel;
        this.robot = robot;
        this.onResume = onResume;
        this.onRestart = onRestart;
        this.onLeave = onLeave;
    }

    public void updateCenter() {
        centerX = panel.getWidth() / 2;
        centerY = panel.getHeight() / 2;
    }

    // — MouseMotionListener —

    @Override
    public void mouseMoved(MouseEvent e) {
        handleMouseMove(e.getX(), e.getY());
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        handleMouseMove(e.getX(), e.getY());
    }

    /**
     * Handles mouse movement during gameplay by calculating cursor delta
     * and warping the cursor back to the panel center to allow unlimited camera movement.
     *
     * @param mx the current x position of the cursor in panel-local coordinates
     * @param my the current y position of the cursor in panel-local coordinates
     */
    private void handleMouseMove(int mx, int my) {
        // Mouse move freely outside of playing state
        if (state.phase != GameState.Phase.PLAYING) {
            state.mouseDeltaX = 0;
            state.mouseDeltaY = 0;
            return;
        }

        // Saying that the next mouseMoved event receive is fake
        if (recentering) {
            recentering = false;
            return;
        }
        state.mouseDeltaX = mx - centerX;
        state.mouseDeltaY = my - centerY;

        Point loc = panel.getLocationOnScreen();
        recentering = true;
        robot.mouseMove(loc.x + centerX, loc.y + centerY);
    }

    // — MouseListener —

    /**
     * Handles mouse button presses
     *
     * @param e the event to be processed
     */
    @Override
    public void mousePressed(MouseEvent e) {
        int mx = e.getX();
        int my = e.getY();

        if (state.phase == GameState.Phase.PAUSED) {
            if (resumeButton.contains(mx, my)) onResume.run();
            else if (restartButton.contains(mx, my)) onRestart.run();
            else if (leaveButton.contains(mx, my)) onLeave.run();
            return;
        }
        if (state.phase == GameState.Phase.GAME_OVER) {
            if (restartButton.contains(mx, my)) onRestart.run();
            else if (leaveButton.contains(mx, my)) onLeave.run();
            return;
        }
        if (e.getButton() == MouseEvent.BUTTON1) {
            state.clickPending = true;
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) {
    }

    @Override
    public void mouseReleased(MouseEvent e) {
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }

    // — KeyListener —

    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
            state.togglePause();
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    @Override
    public void keyReleased(KeyEvent e) {
    }
}