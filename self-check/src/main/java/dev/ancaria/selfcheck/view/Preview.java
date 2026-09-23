package dev.ancaria.selfcheck.view;

import dev.ancaria.selfcheck.model.HeroInfo;
import dev.ancaria.selfcheck.model.Scenario;
import dev.ancaria.selfcheck.viewmodel.SelfCheckModel;
import javafx.application.Platform;

import java.util.List;
import java.util.concurrent.FutureTask;

/**
 * Opens the window on its own, filled with made-up rows.
 *
 * <pre>
 * java -cp build/sacred-mod/self-check-0.100.0.jar dev.ancaria.selfcheck.view.Preview
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
                Scenario.seen("world", "World loads and unloads", "Load a save or start a new game"),
                Scenario.seen("hero", "Hero is captured", "Enter the game with any character"),
                Scenario.mutates("health", "Damage softened by 1 HP", "Let a monster hit you"),
                Scenario.mutates("gold", "Gold gain raised by 1", "Pick up gold"),
                Scenario.seen("level", "Level goes up",
                        "Earn enough experience to reach the next level")));

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
        model.hero(new HeroInfo("Read at 14:05:31", List.of(
                new HeroInfo.Section("Hero", List.of(
                        new HeroInfo.Field("Class", "Daemon"),
                        new HeroInfo.Field("Level", "34"),
                        new HeroInfo.Field("HP", "861 / 1200"),
                        new HeroInfo.Field("Gold", "51230"),
                        new HeroInfo.Field("Experience", "9400000"),
                        new HeroInfo.Field("Position", "222850, 137608"),
                        new HeroInfo.Field("Region", "12"),
                        new HeroInfo.Field("Sector", "40, 77"))),
                new HeroInfo.Section("Attributes", List.of(
                        new HeroInfo.Field("Strength", "141"),
                        new HeroInfo.Field("Endurance", "90"),
                        new HeroInfo.Field("Points to spend", "4"))),
                new HeroInfo.Section("Combat arts", List.of(
                        new HeroInfo.Field("Learned", "2 of 3"),
                        new HeroInfo.Field("Levels", "#12/0 15+3,  #14/1 8"))),
                new HeroInfo.Section("Character sheet", List.of(
                        new HeroInfo.Field("Armor", "62%"),
                        new HeroInfo.Field("Resistances",
                                "physical 30,  fire 45,  magic 20,  poison 50"))))));
        Thread.sleep(120_000);
        Platform.exit();
    }
}
