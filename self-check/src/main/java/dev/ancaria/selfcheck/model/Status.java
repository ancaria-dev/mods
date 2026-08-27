package dev.ancaria.selfcheck.model;

/** Where one scenario stands. */
public enum Status {

    /** Not seen yet. Play the game and it will be. */
    WAITING("\u2B1C"),
    /** Seen, and whatever the scenario claimed to do was done. */
    PASSED("\u2705"),
    /** Seen, and it did not work. This is the only interesting one. */
    FAILED("\u274C");

    private final String mark;

    Status(String mark) {
        this.mark = mark;
    }

    /** The box shown in the list. */
    public String mark() {
        return mark;
    }
}
