package dev.ancaria.selfcheck.model;

import java.util.List;

/**
 * One reading of the hero, as the panel shows it.
 *
 * <p>Already text. The panel only lays it out, so what it shows and what the
 * log says about the same reading cannot drift apart.
 *
 * @param status   one line over the sections: when it was read, or why not
 * @param sections the reading itself, empty until a hero has been read
 */
public record HeroInfo(String status, List<Section> sections) {

    public HeroInfo {
        sections = List.copyOf(sections);
    }

    /** A heading and the label/value pairs under it. */
    public record Section(String title, List<Field> fields) {

        public Section {
            fields = List.copyOf(fields);
        }
    }

    public record Field(String label, String value) {
    }

    /** Nothing to show yet, and the reason. */
    public static HeroInfo waiting(String status) {
        return new HeroInfo(status, List.of());
    }
}
