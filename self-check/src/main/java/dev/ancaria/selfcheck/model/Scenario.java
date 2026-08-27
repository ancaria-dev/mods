package dev.ancaria.selfcheck.model;

/**
 * One thing worth proving.
 *
 * @param key      the wire name, or a made-up one for checks that are not events
 * @param title    what the window shows
 * @param mutating whether passing means "we changed something and it stuck",
 *                 rather than only "it arrived"
 */
public record Scenario(String key, String title, boolean mutating) {

    public static Scenario seen(String key, String title) {
        return new Scenario(key, title, false);
    }

    public static Scenario mutates(String key, String title) {
        return new Scenario(key, title, true);
    }
}
