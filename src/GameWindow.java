import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Thin shell — owns the panel, wires GameState + InputHandler + Renderer + GameLoop together.
 * Contains almost no logic itself; it just connects the pieces.
 */
public class GameWindow extends JPanel {

    private final GameState state;
    private final Renderer renderer;
    private final InputHandler input;
    private final GameLoop gameLoop;
    private final ScoreRepository scoreRepo;
    private final JMenuBar menuBar;

    /**
     * Construct the main panel and wires all components together.
     * Initializes game state, input handling, and the game loop
     *
     * @param frame the parent frame this panel is attached to
     * @throws AWTException if the Robot (used for mouse confinement) is not created
     */
    public GameWindow(JFrame frame) throws AWTException {
        this.scoreRepo = new ScoreRepository();

        Image targetImg = new ImageIcon("target.png").getImage();
        Image bgImg = new ImageIcon("background.jpeg").getImage();

        state = new GameState(targetImg, bgImg);
        renderer = new Renderer(bgImg, state, scoreRepo, this);
        menuBar = renderer.buildMenuBar(state, frame);

        // Robot is required InputHandler to warp the cursor back
        Robot robot = new Robot();
        input = new InputHandler(
                state, this, robot,
                this::resume,
                this::restart,
                () -> System.exit(0)
        );

        addKeyListener(input);
        addMouseListener(input);
        addMouseMotionListener(input);
        setFocusable(true);

        // Blank cursor during play
        BufferedImage blank = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Cursor blankCursor = Toolkit.getDefaultToolkit()
                .createCustomCursor(blank, new Point(0, 0), "blank");

        gameLoop = new GameLoop(
                deltaMs -> {
                    input.updateCenter();
                    state.tick(deltaMs, getWidth(), getHeight());

                    // Cursor management
                    if (state.phase == GameState.Phase.PLAYING) {
                        setCursor(blankCursor);
                    } else {
                        setCursor(Cursor.getDefaultCursor());
                    }

                    // Show/hide menu bar based on phase
                    boolean showMenu = state.phase != GameState.Phase.PLAYING;
                    SwingUtilities.invokeLater(() -> menuBar.setVisible(showMenu));

                    // Save score once when game ends
                    if (state.phase == GameState.Phase.GAME_OVER
                            && !scoreSaved) {
                        scoreRepo.saveScore(state.score);
                        scoreSaved = true;
                    }
                },
                () -> SwingUtilities.invokeLater(this::repaint)
        );
    }

    private boolean scoreSaved = false; // ensures score is written once per game

    /**
     * Called by Swing when the panel is added to visible window.
     * Used to initialise the camera and start the game loop once the panel size is known.
     */
    @Override
    public void addNotify() {
        super.addNotify();
        state.initCamera(getWidth(), getHeight());
        gameLoop.start();
        requestFocusInWindow();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        renderer.draw(g, getWidth(), getHeight(), state);

        // Sync button rects from renderer back to input handler
        input.resumeButton = renderer.resumeButton;
        input.restartButton = renderer.restartButton;
        input.leaveButton = renderer.leaveButton;
    }

    private void resume() {
        state.resume();
    }

    private void restart() {
        scoreSaved = false;
        state.reset(getWidth(), getHeight());
        gameLoop.start(); // safe — start() is a no-op if already running
    }

    public JMenuBar getMenuBar() {
        return menuBar;
    }
}