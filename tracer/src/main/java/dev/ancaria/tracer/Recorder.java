package dev.ancaria.tracer;

import dev.ancaria.coderpack.api.Priority;
import dev.ancaria.coderpack.api.Subscribe;
import dev.ancaria.coderpack.api.event.Event;
import dev.ancaria.coderpack.api.event.Unknown;
import dev.ancaria.coderpack.api.event.Amount;
import dev.ancaria.coderpack.api.event.Decision;
import dev.ancaria.coderpack.api.event.World;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * One listener for everything.
 *
 * <p>SAL dispatches by parameter type, so a method taking the base
 * {@link Event} receives every event there is, including the ones the SDK has
 * no class for yet, which arrive as {@link Unknown}. Nothing here needs
 * touching when the agent grows a new event.
 *
 * <p>It runs at {@link Priority#MONITOR}, so every other mod has already had
 * its say and a decidable event is recorded together with the verdict it ended
 * up with, the thing worth knowing when a boost did not take effect. A
 * monitor cannot change anything either, which is exactly right for a tracer.
 */
final class Recorder {

    private static final DateTimeFormatter CLOCK =
            DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
    private static final int NAME_WIDTH = 20;

    private final Sink sink;

    Recorder(Sink sink) {
        this.sink = sink;
    }

    @Subscribe(priority = Priority.MONITOR)
    public void onAny(Event event) {
        StringBuilder line = new StringBuilder(160);
        line.append(LocalTime.now().format(CLOCK)).append("  ");
        String name = name(event);
        line.append(name);
        for (int pad = name.length(); pad < NAME_WIDTH; pad++) {
            line.append(' ');
        }
        for (Map.Entry<String, String> field : event.fields().entrySet()) {
            line.append(' ').append(field.getKey()).append('=').append(field.getValue());
        }
        verdict(event, line);
        sink.write(line.toString());
    }

    /** The wire name where the SDK has no type, the type's own name otherwise. */
    private static String name(Event event) {
        if (event instanceof Unknown unknown) {
            return unknown.name();
        }
        if (event instanceof World world) {
            return "World." + world.phase();
        }
        return event.getClass().getSimpleName();
    }

    /**
     * What the mods ahead of us decided, if this was a decidable event.
     *
     * <p>A tracer is MONITOR, so it runs last and reads the fold rather than a
     * list of pending writes. That is the whole reason this is trustworthy now:
     * value() is what the game is about to be told, not what it proposed.
     */
    private static void verdict(Event event, StringBuilder line) {
        if (!(event instanceof Decision decision)) {
            return;
        }
        if (decision.vetoed()) {
            line.append("   → vetoed");
            return;
        }
        if (event instanceof Amount amount && amount.value() != amount.initial()) {
            line.append("   → ").append(amount.initial())
                .append(" → ").append(amount.value());
        }
    }
}
