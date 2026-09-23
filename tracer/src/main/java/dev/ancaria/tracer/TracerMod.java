package dev.ancaria.tracer;

import dev.ancaria.coderpack.api.SacredMod;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Writes every event SAL sees to {@code <Sacred Gold>/logs/logs-<time>.txt}.
 *
 * <p>A new file per run, so one session's trace is never mixed with the last
 * one's. Buffered through a ring and written by its own thread: a tracer that
 * stalled the game thread would change the very timings it exists to show.
 *
 * <p>The trace is its own file rather than lines in {@code mods.log}: it is
 * what this mod produces, one file per run that can be handed to somebody,
 * and it would drown every other mod's lines in there.
 */
public final class TracerMod extends SacredMod {

    private Sink sink;

    @Override
    public void onLoad() {
        Path directory = getContext().getGame().getDirectory().resolve("logs");
        try {
            sink = Sink.open(directory);
            getContext().getRegistry().getEventRegistry().register(new Recorder(sink));
            getContext().log("Writing events to " + sink.file());
        } catch (IOException failure) {
            getContext().log("Could not write a trace in " + directory + ": " + failure);
        }
    }

    /**
     * The last batch reaches the disk here. The loader calls this on BYE and
     * when the host's pipe closes, before the JVM exits, and also when the mod
     * is unregistered, which the shutdown hook this replaced never covered.
     */
    @Override
    public void onUnload() {
        if (sink != null) {
            sink.close();
        }
    }
}
