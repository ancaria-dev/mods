package dev.ancaria.selfcheck.model;

import java.time.LocalTime;

/**
 * One line in the log.
 *
 * <p>The mod does not print to the host console: with twenty scenarios firing
 * during combat it would bury everything else the loader has to say. The window
 * is the output.
 */
public record Line(LocalTime at, Level level, String text) {

    public enum Level { EVENT, CHANGE, PASS, FAIL }

    public static Line of(Level level, String text) {
        return new Line(LocalTime.now(), level, text);
    }
}
