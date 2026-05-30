import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Holds all game data and advances game logic each tick.
 * No rendering, no input handling — pure state and rules.
 * <p>
 * Hit detection is done entirely in SCREEN space so it matches
 * exactly what the player sees — zoom and camera are both factored in.
 */
public class GameState {

    public enum Phase {PLAYING, PAUSED, GAME_OVER}

    // — Config —
    public static final int GAME_DURATION_MS = 30_000;
    public static final int TARGET_WIDTH = 80;
    public static final int TARGET_HEIGHT = 80;
    public static final double DEFAULT_ZOOM = 0.8;

    // — Camera (world space) —
    public int cameraX = 0;
    public int cameraY = 0;

    // — Sensitivity —
    public double sensitivity = 0.2;

    // — Game —
    public int score = 0;
    public int timeRemainingMs = GAME_DURATION_MS;
    public Phase phase = Phase.PLAYING;

    // — Targets (world space) —
    public final List<Block> targetList = new ArrayList<>();

    // — Internal —
    private final Random rng = new Random();
    private final Image targetImage;
    private final Image backgroundImage;

    // Input deltas consumed each tick
    public int mouseDeltaX = 0;
    public int mouseDeltaY = 0;
    public boolean clickPending = false;

    // Minimum distance between consecutive spawns
    private int lastTargetX = -1;
    private int lastTargetY = -1;
    private static final int MIN_DISTANCE = 200;

    public GameState(Image targetImage, Image backgroundImage) {
        this.targetImage = targetImage;
        this.backgroundImage = backgroundImage;

        int bgW = backgroundImage.getWidth(null);
        int bgH = backgroundImage.getHeight(null);
        cameraX = bgW / 2;
        cameraY = bgH / 2;
    }

    /**
     * Called once the panel has a known size so we can properly centre the camera.
     */
    public void initCamera(int panelWidth, int panelHeight) {
        int viewW = (int) (panelWidth / DEFAULT_ZOOM);
        int viewH = (int) (panelHeight / DEFAULT_ZOOM);
        int bgW = backgroundImage.getWidth(null);
        int bgH = backgroundImage.getHeight(null);
        cameraX = (bgW - viewW) / 2;
        cameraY = (bgH - viewH) / 2;
    }

    /**
     * Advance one tick. deltaMs is the real milliseconds since the last tick.
     */
    public void tick(long deltaMs, int panelWidth, int panelHeight) {
        if (phase != Phase.PLAYING) return;

        // Count down timer
        timeRemainingMs -= (int) deltaMs;
        if (timeRemainingMs <= 0) {
            timeRemainingMs = 0;
            phase = Phase.GAME_OVER;
            return;
        }

        // Move camera with mouse input
        cameraX += (int) (mouseDeltaX * sensitivity);
        cameraY += (int) (mouseDeltaY * sensitivity);
        clampCamera(panelWidth, panelHeight);

        // Spawn target if none present
        if (targetList.isEmpty()) {
            spawnTarget();
        }

        // ── Hit detection in SCREEN space ────────────────────────────────────
        // The crosshair is always drawn at the panel centre.
        // The target is drawn at:  screen_x = (world_x - cameraX) * zoom
        //                          screen_y = (world_y - cameraY) * zoom
        // We compare both in screen space so zoom and camera are accounted for.
        if (clickPending && !targetList.isEmpty()) {
            Block t = targetList.get(0);

            // Crosshair screen position (always dead centre)
            int crossScreenX = panelWidth / 2;
            int crossScreenY = panelHeight / 2;

            // Target screen bounds (where it is actually rendered)
            int tScreenX = (int) ((t.getX() - cameraX) * DEFAULT_ZOOM);
            int tScreenY = (int) ((t.getY() - cameraY) * DEFAULT_ZOOM);
            int tScreenW = (int) (t.getWidth() * DEFAULT_ZOOM);
            int tScreenH = (int) (t.getHeight() * DEFAULT_ZOOM);

            Rectangle screenBounds = new Rectangle(tScreenX, tScreenY, tScreenW, tScreenH);
            if (screenBounds.contains(crossScreenX, crossScreenY)) {
                score++;
                targetList.clear();
            }
        }

        // Consume input
        mouseDeltaX = 0;
        mouseDeltaY = 0;
        clickPending = false;
    }

    public void pause() {
        if (phase == Phase.PLAYING) phase = Phase.PAUSED;
    }

    public void resume() {
        if (phase == Phase.PAUSED) phase = Phase.PLAYING;
    }

    public void togglePause() {
        if (phase == Phase.PLAYING) pause();
        else if (phase == Phase.PAUSED) resume();
    }

    /**
     * Reset all game state to start a new game.
     *
     */
    public void reset(int panelWidth, int panelHeight) {
        score = 0;
        timeRemainingMs = GAME_DURATION_MS;
        phase = Phase.PLAYING;
        mouseDeltaX = 0;
        mouseDeltaY = 0;
        clickPending = false;
        targetList.clear();
        initCamera(panelWidth, panelHeight);
    }

    // — Helpers —

    private void clampCamera(int panelWidth, int panelHeight) {
        int viewW = (int) (panelWidth / DEFAULT_ZOOM);
        int viewH = (int) (panelHeight / DEFAULT_ZOOM);
        int bgW = backgroundImage.getWidth(null);
        int bgH = backgroundImage.getHeight(null);
        int maxX = bgW - viewW;
        int maxY = bgH - viewH;
        cameraX = Math.max(0, Math.min(cameraX, maxX));
        cameraY = Math.max(0, Math.min(cameraY, maxY));
    }

    /**
     * Spawns target at a random position within the allowed background region
     * Retries up to 10 times to avoid spawning too close to the previous target
     */
    private void spawnTarget() {
        int bgW = backgroundImage.getWidth(null);
        int bgH = backgroundImage.getHeight(null);

        // Margins tuned to keep targets inside the visible played area of the background image
        int marginLeft = (int) (bgW / 2.8);
        int marginRight = (int) (bgW / 2.25);
        int marginTop = bgH / 3;
        int marginBottom = (int) (bgH / 2.1);

        int usableW = bgW - marginLeft - marginRight;
        int usableH = bgH - marginTop - marginBottom;

        int tx, ty;
        int attempts = 0;

        do {
            tx = rng.nextInt(Math.max(1, usableW - TARGET_WIDTH)) + marginLeft;
            ty = rng.nextInt(Math.max(1, usableH - TARGET_HEIGHT)) + marginTop;
            attempts++;
            if (attempts >= 10) break;   // avoid infinite loop
        } while (isTooClose(tx, ty));

        lastTargetX = tx;
        lastTargetY = ty;
        targetList.add(new Block(tx, ty, TARGET_WIDTH, TARGET_HEIGHT, targetImage));
    }

    /**
     * Checks if the given position is too close to the previous target's position.
     *
     * @param tx the x coordinate of the candidate spawn position
     * @param ty the y coordinate of the candidate spawn position
     * @return true if the distance to the last target's position is less than MIN_DISTANCE, false otherwise
     */
    private boolean isTooClose(int tx, int ty) {
        if (lastTargetX == -1) return false;
        int dx = tx - lastTargetX;
        int dy = ty - lastTargetY;
        return Math.sqrt(dx * dx + dy * dy) < MIN_DISTANCE;
    }
}