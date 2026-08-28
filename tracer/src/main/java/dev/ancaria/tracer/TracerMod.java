package dev.ancaria.tracer;

import dev.ancaria.coderpack.api.Context;
import dev.ancaria.coderpack.api.SacredMod;

import java.io.IOException;
import java.nio.file.Path;

/**
 * Writes every event SAL sees to {@code <Sacred Gold>/logs/logs-<time>.txt}.
 *
 * <p>A new file per run, so one session's trace is never mixed with the last
 * one's. Buffered through a ring and written by its own thread: a tracer that
 * stalled the game thread would change the very timings it exists to show.
 */
public final class TracerMod implements SacredMod {

    @Override
    public void onLoad(Context context) {
        Path directory = context.gameDir().resolve("logs");
        try {
            Sink sink = Sink.open(directory);
            context.events().register(new Recorder(sink));
            // The JVM is a child of the host and dies when it does, so the last
            // batch reaches the disk here or not at all.
            Runtime.getRuntime().addShutdownHook(new Thread(sink::close, "tracer-close"));
            context.log("Writing events to " + sink.file());
        } catch (IOException failure) {
            context.log("Could not write a trace in " + directory + ": " + failure);
        }
    }
}
