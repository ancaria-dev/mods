package dev.ancaria.selfcheck.view;

import javafx.application.Platform;

/**
 * Starts JavaFX inside a JVM that was never meant to have a UI.
 *
 * <p>No {@code Application} subclass and no {@code launch()}: that path checks
 * how JavaFX was put on the classpath and refuses when the answer is "inside
 * somebody's fat jar", which is exactly how a mod ships. {@code Platform
 * .startup} boots the same toolkit without asking.
 */
public final class Ui {

    private static boolean started;

    private Ui() {
    }

    /** @return false when this JVM cannot show a window at all */
    public static synchronized boolean boot() {
        if (started) {
            return true;
        }
        try {
            Platform.startup(() -> { });
        } catch (IllegalStateException alreadyRunning) {
            // Another mod got here first, which is fine.
        } catch (Throwable failure) {
            return false;
        }
        // The loader's JVM has to outlive this window. Without this, closing it
        // would shut down the toolkit and take every other mod with it.
        Platform.setImplicitExit(false);
        started = true;
        return true;
    }
}
