package dev.ancaria.tracer;

import java.util.ArrayList;
import java.util.List;

/**
 * Fixed-size ring of pending lines.
 *
 * <p>The point is that {@link #add} never blocks and never touches a file: it
 * runs on SAL's dispatch thread, and for a vetoable event that thread is
 * holding the game still. Writing from there -- even to a buffered stream --
 * would put disk latency inside the game's frame.
 *
 * <p>When the writer falls behind, the oldest lines are overwritten rather than
 * the newest dropped. A trace is read backwards from whatever just went wrong,
 * so recent history is the half worth keeping.
 */
final class Ring {

    private final String[] slots;

    private int head;
    private int size;
    private long dropped;

    Ring(int capacity) {
        slots = new String[capacity];
    }

    synchronized void add(String line) {
        if (size == slots.length) {
            head = (head + 1) % slots.length;
            size--;
            dropped++;
        }
        slots[(head + size) % slots.length] = line;
        size++;
    }

    /** Everything buffered, oldest first. Empty rather than null when idle. */
    synchronized List<String> drain() {
        if (size == 0) {
            return List.of();
        }
        List<String> out = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            int slot = (head + i) % slots.length;
            out.add(slots[slot]);
            slots[slot] = null;
        }
        head = 0;
        size = 0;
        return out;
    }

    /** Lines lost since the last call, so the gap can be marked in the file. */
    synchronized long takeDropped() {
        long lost = dropped;
        dropped = 0;
        return lost;
    }
}
