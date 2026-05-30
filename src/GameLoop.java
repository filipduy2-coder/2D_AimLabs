import java.util.function.LongConsumer;

/**
 * Drives the game at a fixed target rate using System.nanoTime().
 * Calls update (logic) and repaint (rendering) each tick.
 * Completely separate from Swing's Timer — no drift, no skipped frames.
 */
public class GameLoop implements Runnable {

    private static final int TARGET_FPS = 360;
    private static final long TICK_NS = 1_000_000_000L / TARGET_FPS;

    private volatile boolean running = false;
    private Thread thread;

    private final Runnable onTick;   // called with delta time each frame
    private final LongConsumer tickConsumer;

    /**
     * @param tickConsumer receives deltaMs each tick so GameState can update precisely
     * @param onRepaint    called after state update to schedule a repaint
     */
    public GameLoop(LongConsumer tickConsumer, Runnable onRepaint) {
        this.tickConsumer = tickConsumer;
        this.onTick = onRepaint;
    }

    /**
     * Starts the game loop thread.
     */
    public void start() {
        if (running) return;
        running = true;
        thread = new Thread(this, "GameLoop");
        thread.setDaemon(true);
        thread.start();
    }

    public void stop() {
        running = false;
    }


    /**
     * Runs the game loop, calling tick and repaint at a fixed interval.
     * Sleeps for the remaining time in each tick to maintain a consistent tick rate.
     */
    @Override
    public void run() {
        long lastTime = System.nanoTime(); // reference for the first delta calculation

        while (running) {
            long now = System.nanoTime();
            long deltaNs = now - lastTime; // how many nanosecond have passed since the last frame
            lastTime = now;

            long deltaMs = deltaNs / 1_000_000L;
            tickConsumer.accept(deltaMs);
            onTick.run();

            // Sleep for remaining time in this tick
            long elapsed = System.nanoTime() - now;
            long sleepNs = TICK_NS - elapsed;
            if (sleepNs > 0) {
                try {
                    /*
                    Timeline :
                    ──────────────────────────────────────────────────────────
                    now = 0ms
                    update and repaint takes 3ms
                    elapsed = 3ms
                    sleepNs = 10ms - 3ms = 7ms
                    so thread will sleep for 7ms then wake up at ~10ms -> next tick starts
                     */
                    Thread.sleep(sleepNs / 1_000_000L, (int) (sleepNs % 1_000_000L));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }
}