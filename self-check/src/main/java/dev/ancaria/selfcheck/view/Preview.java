package dev.ancaria.selfcheck.view;

import dev.ancaria.selfcheck.model.Scenario;
import dev.ancaria.selfcheck.viewmodel.SelfCheckModel;
import javafx.application.Platform;

import java.util.List;
import java.util.concurrent.FutureTask;

/**
 * Opens the window on its own, filled with made-up rows.
 *
 * <pre>
 * java -cp build/sacred-mod/self-check-0.99.2.jar dev.ancaria.selfcheck.view.Preview
 * </pre>
 *
 * <p>Worth its own class because the alternative for a layout change is starting
 * the game. It is also the shortest path to a real stack trace when the window
 * refuses to appear, which is otherwise swallowed by the FX thread inside a
 * process that has plenty of other output.
 */
public final class Preview {

    private Preview() {
    }

    public static void main(String[] args) throws Exception {
        if (!Ui.boot()) {
            System.err.println("JavaFX did not start");
            return;
        }
        SelfCheckModel model = new SelfCheckModel(List.of(
                Scenario.seen("world", "World loads and unloads"),
                Scenario.seen("hero", "Hero is captured"),
                Scenario.mutates("health", "Damage softened by 1 HP"),
                Scenario.mutates("gold", "Gold gain raised by 1"),
                Scenario.seen("level", "Level goes up")));

        // Through a FutureTask rather than a bare runLater: an exception on the
        // FX thread is otherwise printed into whatever else the process is
        // saying and easy to miss, and "the window did not appear" with no
        // reason attached is the least useful bug report there is.
        FutureTask<Boolean> shown = new FutureTask<>(() -> {
            new SelfCheckWindow(model).show();
            return true;
        });
        Platform.runLater(shown);
        System.out.println("Window shown: " + shown.get());

        model.pass("world", "Loaded");
        model.pass("hero", "Daemon, level 34");
        model.event("Damage 980 → 860 (120 damage)");
        model.change("Damage softened: 860 → 861");
        model.pass("health", "Kept 1 HP back (860 → 861)");
        model.fail("gold", "Pretend failure so the red box is visible too");
        Thread.sleep(120_000);
        Platform.exit();
    }
}
