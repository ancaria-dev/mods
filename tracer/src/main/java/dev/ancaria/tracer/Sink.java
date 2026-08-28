package dev.ancaria.tracer;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * The file end: one daemon thread draining the ring and writing batches.
 *
 * <p>It flushes per batch rather than per line. A trace is usually read after
 * something took the game with it, so what is already on disk has to be
 * complete up to a fraction of a second ago -- but not at the cost of a flush
 * for every hit during combat.
 */
final class Sink {

    private static final int CAPACITY = 8192;
    private static final long IDLE_MS = 100;
    private static final DateTimeFormatter STAMP =
            DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final Ring ring = new Ring(CAPACITY);
    private final Path file;

    private volatile boolean running = true;
    private volatile boolean writing = true;

    private Sink(Path file) {
        this.file = file;
    }

    static Sink open(Path directory) throws IOException {
        Files.createDirectories(directory);
        Sink sink = new Sink(directory.resolve(
                "logs-" + LocalDateTime.now().format(STAMP) + ".txt"));
        Thread thread = new Thread(sink::run, "tracer");
        thread.setDaemon(true);
        thread.start();
        return sink;
    }

    Path file() {
        return file;
    }

    void write(String line) {
        ring.add(line);
    }

    /** Stops the writer and gives the last batch time to reach the disk. */
    void close() {
        running = false;
        for (int i = 0; i < 40; i++) {
            if (!writing) {
                return;
            }
            try {
                Thread.sleep(25);
            } catch (InterruptedException stop) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    private void run() {
        try (BufferedWriter out = Files.newBufferedWriter(
                file, StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
            while (running) {
                if (!flush(out)) {
                    Thread.sleep(IDLE_MS);
                }
            }
            flush(out);
        } catch (InterruptedException stop) {
            Thread.currentThread().interrupt();
        } catch (IOException failure) {
            System.err.println("[tracer] Stopped writing to " + file + ": " + failure);
        } finally {
            writing = false;
        }
    }

    private boolean flush(BufferedWriter out) throws IOException {
        List<String> batch = ring.drain();
        if (batch.isEmpty()) {
            return false;
        }
        long lost = ring.takeDropped();
        if (lost > 0) {
            out.write("  … Dropped " + lost + (lost == 1 ? " event" : " events")
                    + " because the tracer fell behind");
            out.newLine();
        }
        for (String line : batch) {
            out.write(line);
            out.newLine();
        }
        out.flush();
        return true;
    }
}
