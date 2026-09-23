package dev.ancaria.selfcheck.viewmodel;

import dev.ancaria.selfcheck.model.HeroInfo;
import dev.ancaria.selfcheck.model.Line;
import dev.ancaria.selfcheck.model.Scenario;
import dev.ancaria.selfcheck.model.Status;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The only thing the window knows about, and the only thing the mod talks to.
 *
 * <p>Events arrive on the loader's bus thread and JavaFX will not be touched
 * from there, so every mutation hops to the FX thread here rather than in the
 * fifteen call sites that would otherwise have to remember.
 */
public final class SelfCheckModel {

    /** A window kept for an hour of play should not grow without limit. */
    private static final int MAX_LINES = 500;

    private final ObservableList<CheckRow> checks = FXCollections.observableArrayList();
    private final Map<String, CheckRow> byKey = new LinkedHashMap<>();
    private final ObservableList<Line> log = FXCollections.observableArrayList();
    private final ObjectProperty<HeroInfo> hero =
            new SimpleObjectProperty<>(HeroInfo.waiting("No hero is loaded"));
    /** Hits not yet shown, per scenario. Written on any thread, drained on FX. */
    private final Map<String, Tally> tallies = new ConcurrentHashMap<>();
    private final AtomicBoolean tallyQueued = new AtomicBoolean();
    /** For a thread other than FX that wants the count, the console answer. */
    private volatile int passedSoFar;

    private record Tally(int hits, String note) {
    }

    public SelfCheckModel(List<Scenario> scenarios) {
        for (Scenario scenario : scenarios) {
            CheckRow row = new CheckRow(scenario);
            checks.add(row);
            byKey.put(scenario.key(), row);
        }
    }

    public ObservableList<CheckRow> checks() {
        return checks;
    }

    public ObservableList<Line> log() {
        return log;
    }

    /** The latest reading of the hero, replaced whole each time. */
    public ReadOnlyObjectProperty<HeroInfo> hero() {
        return hero;
    }

    /** A new reading of the hero, from any thread. */
    public void hero(HeroInfo info) {
        onFx(() -> hero.set(info));
    }

    /** Passed out of total, recomputed by the list itself whenever a row changes. */
    public ObservableValue<String> summary() {
        return Bindings.createStringBinding(
                () -> passed() + " of " + checks.size() + " scenarios passed",
                checks);
    }

    public DoubleBinding progress() {
        return Bindings.createDoubleBinding(
                () -> checks.isEmpty() ? 0 : (double) passed() / checks.size(),
                checks);
    }

    /** A scenario worked. */
    public void pass(String key, String note) {
        settle(key, Status.PASSED, note, Line.Level.PASS);
    }

    /**
     * A scenario worked, for an event that can arrive tens of times a second.
     *
     * <p>{@link #pass} costs one FX task and one log line per call, which a
     * crowded fight turns into a queue the window never catches up with. This
     * gathers hits and shows them in one task however many arrived meanwhile,
     * and logs only the first.
     */
    public void tally(String key, String note) {
        tallies.merge(key, new Tally(1, note),
                      (before, now) -> new Tally(before.hits() + 1, now.note()));
        if (tallyQueued.compareAndSet(false, true)) {
            onFx(this::drainTallies);
        }
    }

    /** Scenarios passed so far, from any thread. */
    public int passedCount() {
        return passedSoFar;
    }

    public int total() {
        return checks.size();
    }

    /** A scenario arrived and did not do what it said it would. */
    public void fail(String key, String note) {
        settle(key, Status.FAILED, note, Line.Level.FAIL);
    }

    public void event(String text) {
        add(Line.of(Line.Level.EVENT, text));
    }

    /** Something was rewritten in the running game. Worth its own colour. */
    public void change(String text) {
        add(Line.of(Line.Level.CHANGE, text));
    }

    private void settle(String key, Status result, String note, Line.Level level) {
        onFx(() -> {
            CheckRow row = byKey.get(key);
            if (row == null) {
                return;
            }
            row.record(result, note);
            // The list has no idea a row's properties changed, and the summary
            // binding watches the list. Nudging it is what keeps the count and
            // the progress bar honest.
            int index = checks.indexOf(row);
            checks.set(index, row);
            passedSoFar = passed();
            push(Line.of(level, row.title() + ": " + note));
        });
    }

    private void drainTallies() {
        // Cleared before draining, so a hit that lands meanwhile queues
        // another drain rather than waiting for the next one.
        tallyQueued.set(false);
        for (String key : List.copyOf(tallies.keySet())) {
            Tally tally = tallies.remove(key);
            CheckRow row = byKey.get(key);
            if (tally == null || row == null) {
                continue;
            }
            boolean first = row.status() != Status.PASSED;
            row.record(Status.PASSED, tally.note(), tally.hits());
            checks.set(checks.indexOf(row), row);
            passedSoFar = passed();
            if (first) {
                push(Line.of(Line.Level.PASS, row.title() + ": " + tally.note()));
            }
        }
    }

    private void add(Line line) {
        onFx(() -> push(line));
    }

    private void push(Line line) {
        log.add(line);
        if (log.size() > MAX_LINES) {
            log.remove(0, log.size() - MAX_LINES);
        }
    }

    private int passed() {
        return (int) checks.stream().filter(row -> row.status() == Status.PASSED).count();
    }

    private static void onFx(Runnable work) {
        if (Platform.isFxApplicationThread()) {
            work.run();
        } else {
            Platform.runLater(work);
        }
    }
}
