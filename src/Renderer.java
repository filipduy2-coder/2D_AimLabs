import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;

/**
 * Knows how to draw a GameState onto a Graphics2D context.
 * No logic, no input — rendering only.
 * <p>
 * Coordinate system
 * -----------------
 * Everything in PLAYING mode is drawn inside a g2d.scale(zoom) + g2d.translate(-cameraX,-cameraY)
 * transform so that world-space coordinates map 1-to-1 to what the player sees.
 * The crosshair is drawn AFTER the transform is popped, always at screen centre.
 * Hit detection in GameState uses the same math, so collision = visual.
 */
public class Renderer {

    // Passed out so InputHandler can read updated button bounds each frame
    public Rectangle resumeButton = new Rectangle();
    public Rectangle restartButton = new Rectangle();
    public Rectangle leaveButton = new Rectangle();

    private final Image backgroundImage;
    private final ScoreRepository scoreRepo;

    // Sensitivity widgets
    private final JSlider sensitivitySlider;
    private JTextField sensitivityTF;
    private boolean draggingSlider = false;

    private static final int SCALE = 100;   // slider int = sensitivity * SCALE

    public Renderer(Image backgroundImage, GameState state, ScoreRepository scoreRepo, JPanel panel) {
        this.backgroundImage = backgroundImage;
        this.scoreRepo = scoreRepo;

        sensitivitySlider = new JSlider(JSlider.HORIZONTAL, 1, 300,
                (int) (state.sensitivity * SCALE));
        sensitivitySlider.setOpaque(false);
        sensitivitySlider.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                int min = sensitivitySlider.getMinimum();
                int max = sensitivitySlider.getMaximum();
                int width = sensitivitySlider.getWidth();
                int value = min + (int) ((e.getX() / (double) width) * (max - min));
                sensitivitySlider.setValue(value);
                draggingSlider = true;
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                draggingSlider = false;
            }
        });
        sensitivitySlider.addChangeListener(e -> {
            state.sensitivity = (double) sensitivitySlider.getValue() / SCALE;
            sensitivityTF.setText(String.format("%.2f", state.sensitivity));
        });

        sensitivityTF = new JTextField(String.format("%.2f", state.sensitivity));
        sensitivityTF.setColumns(5);
        sensitivityTF.setHorizontalAlignment(JTextField.CENTER);
        sensitivityTF.setFont(new Font("Arial", Font.PLAIN, 20));
        sensitivityTF.setOpaque(false);
        sensitivityTF.setForeground(Color.WHITE);
        sensitivityTF.setBorder(null);
        sensitivityTF.addActionListener(e -> {
            try {
                double v = Double.parseDouble(sensitivityTF.getText());
                state.sensitivity = Math.max(0.01, Math.min(v, 3.0));
                sensitivitySlider.setValue((int) (state.sensitivity * SCALE));
            } catch (NumberFormatException ex) {
                sensitivityTF.setText(String.format("%.2f", state.sensitivity));
            }
        });
        sensitivityTF.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                sensitivityTF.requestFocusInWindow();
            }
        });

        panel.add(sensitivitySlider);
        panel.add(sensitivityTF);
    }

    /**
     * Main entry point - routes to the correct draw methods based on game phase
     */
    public void draw(Graphics g, int panelW, int panelH, GameState state) {
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

        sensitivitySlider.setVisible(false);
        sensitivityTF.setVisible(false);

        switch (state.phase) {
            case GAME_OVER -> {
                drawWorld(g2d, panelW, panelH, state);
                drawGameOver(g2d, panelW, panelH, state);
            }
            case PAUSED -> {
                drawWorld(g2d, panelW, panelH, state);
                drawPaused(g2d, panelW, panelH, state);
            }
            case PLAYING -> {
                drawWorld(g2d, panelW, panelH, state);
                drawHUD(g2d, panelW, panelH, state);
                drawCrosshair(g2d, panelW, panelH);
            }
        }
    }

    // ── World rendering (background + targets) ────────────────────────────────

    /**
     * Applies zoom + camera transform, draws background and all targets, then
     * restores the original transform.  After this call g2d is back in pure
     * screen space (0,0 = top-left of panel).
     * <p>
     * Screen = World mapping (same formula used in GameState hit detection):
     * screenX = (worldX - cameraX) * zoom
     * worldX  = screenX / zoom + cameraX
     */
    private void drawWorld(Graphics2D g2d, int panelW, int panelH, GameState state) {
        // Save transform
        AffineTransform saved = g2d.getTransform();

        double zoom = GameState.DEFAULT_ZOOM;
        g2d.scale(zoom, zoom);
        // After scale, (0,0) maps to screen (0,0); we need world cameraX,cameraY
        // to map to screen (0,0), so translate by -camera
        g2d.translate(-state.cameraX, -state.cameraY);

        // Background — draw at full background image size in world space
        g2d.drawImage(backgroundImage, 0, 0, null);

        // Targets — world-space position, no extra math needed
        for (Block target : state.targetList) {
            g2d.drawImage(target.getImage(), target.getX(), target.getY(), null);
        }

        // Restore to screen space
        g2d.setTransform(saved);
    }

    // ── HUD (score + timer) ───────────────────────────────────────────────────

    private void drawHUD(Graphics2D g2d, int w, int h, GameState state) {
        int fontSize = Math.max(6, w / 50);
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, fontSize));
        FontMetrics fm = g2d.getFontMetrics();

        String scoreText = "Score: " + state.score;
        g2d.drawString(scoreText, (w - fm.stringWidth(scoreText)) / 2, fm.getHeight());

        String timeText = "Time: " + (state.timeRemainingMs / 1000) + "s";
        g2d.drawString(timeText, w - fm.stringWidth(timeText) - 10, fm.getHeight());
    }

    // ── Crosshair (always screen-centre) ─────────────────────────────────────

    private void drawCrosshair(Graphics2D g2d, int w, int h) {
        int cx = w / 2;
        int cy = h / 2;

        // thickness = line width, size = arm length, gap = space around centre
        int thickness = 3, size = 10, gap = 8;

        g2d.setColor(Color.CYAN);
        g2d.fillRect(cx - thickness / 2, cy - gap / 2 - size, thickness, size); // top
        g2d.fillRect(cx - thickness / 2, cy + gap / 2, thickness, size); // bottom
        g2d.fillRect(cx - gap / 2 - size, cy - thickness / 2, size, thickness); // left
        g2d.fillRect(cx + gap / 2, cy - thickness / 2, size, thickness); // right
    }

    // ── Pause overlay ─────────────────────────────────────────────────────────

    private void drawPaused(Graphics2D g2d, int w, int h, GameState state) {
        g2d.setColor(new Color(0, 0, 0, 150));
        g2d.fillRect(0, 0, w, h);

        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 40));
        FontMetrics fm = g2d.getFontMetrics();
        drawCentred(g2d, fm, "PAUSED", w, h / 2 - 120);

        g2d.setFont(new Font("Arial", Font.PLAIN, 30));
        fm = g2d.getFontMetrics();

        int bw = 200, bh = 50, bx = w / 2 - 100;

        drawButton(g2d, fm, "Resume", bx, h / 2 - 70, bw, bh);
        drawButton(g2d, fm, "Restart", bx, h / 2, bw, bh);
        drawButton(g2d, fm, "Leave", bx, h / 2 + 70, bw, bh);

        resumeButton.setBounds(bx, h / 2 - 70, bw, bh);
        restartButton.setBounds(bx, h / 2, bw, bh);
        leaveButton.setBounds(bx, h / 2 + 70, bw, bh);

        // Sensitivity slider
        int sliderW = 300, sliderH = 30;
        int sx = w / 2 - sliderW / 2;
        int sy = h / 2 + 160;
        sensitivitySlider.setBounds(sx, sy, sliderW, sliderH);
        sensitivitySlider.setVisible(true);

        g2d.setFont(new Font("Arial", Font.BOLD, 25));
        g2d.drawString("Sensitivity", sx - 120, sy + sliderH / 2 + 8);

        if (draggingSlider) {
            g2d.setFont(new Font("Arial", Font.BOLD, 15));
            int knobX = sx + sensitivitySlider.getValue() * sliderW
                    / sensitivitySlider.getMaximum();
            g2d.drawString(String.format("%.2f", state.sensitivity), knobX - 10, sy - 10);
        }

        sensitivityTF.setBounds(sx + sliderW + 10, sy, 50, sliderH);
        sensitivityTF.setVisible(true);
    }

    // ── Game over overlay ─────────────────────────────────────────────────────

    private void drawGameOver(Graphics2D g2d, int w, int h, GameState state) {
        g2d.setColor(new Color(0, 0, 0, 180));
        g2d.fillRect(0, 0, w, h);

        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 50));
        FontMetrics fm = g2d.getFontMetrics();
        drawCentred(g2d, fm, "GAME OVER", w, h / 2 - 120);

        g2d.setFont(new Font("Arial", Font.PLAIN, 30));
        fm = g2d.getFontMetrics();
        drawCentred(g2d, fm, "Your score: " + state.score, w, h / 2 - 60);
        drawCentred(g2d, fm, "Highest score: " + scoreRepo.getHighestScore(), w, h / 2 - 20);

        int bw = 200, bh = 50, bx = w / 2 - 100;

        int byRestart = h / 2 + 40;
        drawButton(g2d, fm, "Restart", bx, byRestart, bw, bh);
        restartButton.setBounds(bx, byRestart, bw, bh);

        int byLeave = h / 2 + 110;
        drawButton(g2d, fm, "Leave", bx, byLeave, bw, bh);
        leaveButton.setBounds(bx, byLeave, bw, bh);
    }

    // ── Utilities ─────────────────────────────────────────────────────────────

    private void drawCentred(Graphics2D g2d, FontMetrics fm, String text, int panelW, int y) {
        g2d.drawString(text, (panelW - fm.stringWidth(text)) / 2, y);
    }

    private void drawButton(Graphics2D g2d, FontMetrics fm, String label,
                            int x, int y, int w, int h) {
        g2d.drawRect(x, y, w, h);
        g2d.drawString(label, x + (w - fm.stringWidth(label)) / 2, y + h - 12);
    }

    /**
     * Builds the menu bar with the scoreboard option
     * Pauses the game while the scoreboard window is open.
     */
    public JMenuBar buildMenuBar(GameState state, JFrame frame) {
        JMenuBar menuBar = new JMenuBar();
        JMenu menu = new JMenu("Scoreboard");
        JMenuItem open = new JMenuItem("Open Scoreboard");
        open.addActionListener(e -> {
            state.pause();
            ScoreWindow sw = new ScoreWindow();
            sw.addWindowListener(new java.awt.event.WindowAdapter() {
                @Override
                public void windowClosed(java.awt.event.WindowEvent we) {
                    state.resume();
                }
            });
        });
        menu.add(open);
        menuBar.add(menu);
        return menuBar;
    }
}