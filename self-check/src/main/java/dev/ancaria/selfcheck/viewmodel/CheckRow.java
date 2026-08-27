package dev.ancaria.selfcheck.viewmodel;

import dev.ancaria.selfcheck.model.Scenario;
import dev.ancaria.selfcheck.model.Status;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyStringProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/** One row of the checklist, observable so the view never has to be told. */
public final class CheckRow {

    private final Scenario scenario;
    private final ObjectProperty<Status> status = new SimpleObjectProperty<>(Status.WAITING);
    private final StringProperty detail = new SimpleStringProperty("");
    private int hits;

    public CheckRow(Scenario scenario) {
        this.scenario = scenario;
    }

    public Scenario scenario() {
        return scenario;
    }

    public String title() {
        return scenario.title();
    }

    public ReadOnlyObjectProperty<Status> statusProperty() {
        return status;
    }

    public Status status() {
        return status.get();
    }

    public ReadOnlyStringProperty detailProperty() {
        return detail;
    }

    /** Called on the FX thread by the view model, never from a listener. */
    void record(Status result, String note) {
        hits++;
        status.set(result);
        detail.set(hits > 1 ? note + "  (×" + hits + ")" : note);
    }
}
