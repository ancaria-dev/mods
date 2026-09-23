package dev.ancaria.selfcheck.view;

import dev.ancaria.selfcheck.model.HeroInfo;
import dev.ancaria.selfcheck.model.Line;
import dev.ancaria.selfcheck.model.Status;
import dev.ancaria.selfcheck.viewmodel.CheckRow;
import dev.ancaria.selfcheck.viewmodel.SelfCheckModel;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import javafx.stage.Stage;

import java.time.format.DateTimeFormatter;
import java.util.Objects;

/**
 * The window. It reads the view model and writes nothing back, and there is
 * nothing to write back, since the game is what drives every value here.
 */
public final class SelfCheckWindow {

    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final SelfCheckModel model;

    public SelfCheckWindow(SelfCheckModel model) {
        this.model = model;
    }

    /** Call on the FX thread. */
    public void show() {
        Stage stage = new Stage();
        stage.setTitle("Self Check");
        stage.setScene(scene());
        stage.setWidth(1040);
        stage.setHeight(800);
        stage.show();
    }

    private Scene scene() {
        BorderPane root = new BorderPane();
        root.getStyleClass().add("root-pane");
        root.setTop(header());
        root.setCenter(body());

        Scene scene = new Scene(root);
        scene.getStylesheets().add(
                Objects.requireNonNull(getClass().getResource("/self-check.css")).toExternalForm());
        return scene;
    }

    private Region header() {
        Label title = new Label("Self Check");
        title.getStyleClass().add("title");

        Label summary = new Label();
        summary.getStyleClass().add("summary");
        summary.textProperty().bind(model.summary());

        ProgressBar progress = new ProgressBar();
        progress.getStyleClass().add("progress");
        progress.progressProperty().bind(model.progress());
        progress.setPrefWidth(220);

        Region gap = new Region();
        HBox.setHgrow(gap, Priority.ALWAYS);

        HBox bar = new HBox(14, title, summary, gap, progress);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.getStyleClass().add("header");
        bar.setPadding(new Insets(18, 22, 18, 22));
        return bar;
    }

    private Region body() {
        VBox left = column("Scenarios", checkList());
        VBox hero = column("Hero", heroPanel());
        VBox log = column("Log", logList());
        // The hero gets what it needs and the log the rest, so a long session
        // still has room to scroll through.
        VBox.setVgrow(hero, Priority.NEVER);
        VBox.setVgrow(log, Priority.ALWAYS);
        VBox right = new VBox(0, hero, log);
        HBox.setHgrow(left, Priority.ALWAYS);
        HBox.setHgrow(right, Priority.ALWAYS);
        left.setPrefWidth(430);
        right.setPrefWidth(590);

        HBox split = new HBox(16, left, right);
        split.setPadding(new Insets(0, 22, 20, 22));
        return split;
    }

    private VBox column(String heading, Region content) {
        Label label = new Label(heading.toUpperCase());
        label.getStyleClass().add("heading");
        VBox.setVgrow(content, Priority.ALWAYS);
        VBox box = new VBox(8, label, content);
        box.getStyleClass().add("panel");
        return box;
    }

    private ListView<CheckRow> checkList() {
        ListView<CheckRow> list = new ListView<>(model.checks());
        list.getStyleClass().add("checks");
        list.setCellFactory(view -> new ListCell<>() {
            @Override
            protected void updateItem(CheckRow row, boolean empty) {
                super.updateItem(row, empty);
                if (empty || row == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                Label mark = new Label();
                mark.getStyleClass().add("mark");
                mark.textProperty().bind(row.statusProperty().map(Status::mark));

                Label title = new Label(row.title());
                title.getStyleClass().add("check-title");

                // How to get there, under the name, so a waiting row says
                // what it is waiting for.
                Label hint = new Label(row.hint());
                hint.getStyleClass().add("check-hint");
                hint.setWrapText(true);

                Label detail = new Label();
                detail.getStyleClass().add("check-detail");
                detail.textProperty().bind(row.detailProperty());
                detail.visibleProperty().bind(row.detailProperty().isNotEmpty());
                detail.managedProperty().bind(detail.visibleProperty());

                HBox line = new HBox(10, mark, new VBox(2, title, hint, detail));
                line.setAlignment(Pos.CENTER_LEFT);
                setGraphic(line);
            }
        });
        return list;
    }

    /**
     * The hero as the last reading left it. Rebuilt whole on every reading
     * rather than bound field by field: a reading is replaced, never edited,
     * and there are few enough labels that building them is nothing.
     */
    private Region heroPanel() {
        Label status = new Label();
        status.getStyleClass().add("hero-status");

        GridPane sections = new GridPane();
        sections.setHgap(22);
        sections.setVgap(12);
        ColumnConstraints half = new ColumnConstraints();
        half.setPercentWidth(50);
        sections.getColumnConstraints().addAll(half, half);

        VBox content = new VBox(10, status, sections);
        content.getStyleClass().add("hero");

        Runnable show = () -> {
            HeroInfo info = model.hero().get();
            status.setText(info.status());
            sections.getChildren().clear();
            int index = 0;
            for (HeroInfo.Section section : info.sections()) {
                sections.add(section(section), index % 2, index / 2);
                index++;
            }
        };
        show.run();
        model.hero().addListener((observable, before, after) -> show.run());

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.getStyleClass().add("hero-scroll");
        scroll.setPrefHeight(300);
        scroll.setMinHeight(300);
        return scroll;
    }

    private static VBox section(HeroInfo.Section section) {
        Label title = new Label(section.title().toUpperCase());
        title.getStyleClass().add("hero-section");
        GridPane fields = new GridPane();
        fields.setHgap(10);
        fields.setVgap(2);
        int row = 0;
        for (HeroInfo.Field field : section.fields()) {
            Label label = new Label(field.label());
            label.getStyleClass().add("hero-label");
            label.setMinWidth(Region.USE_PREF_SIZE);
            Label value = new Label(field.value());
            value.getStyleClass().add("hero-value");
            value.setWrapText(true);
            fields.add(label, 0, row);
            fields.add(value, 1, row);
            row++;
        }
        return new VBox(4, title, fields);
    }

    /**
     * The log is a list of styled text rather than a text area: a TextArea would
     * be one string to append to, and appending to a string a thousand times is
     * how a UI thread stops being one.
     */
    private ListView<Line> logList() {
        ListView<Line> list = new ListView<>(model.log());
        list.getStyleClass().add("log");
        list.setCellFactory(view -> new ListCell<>() {
            @Override
            protected void updateItem(Line line, boolean empty) {
                super.updateItem(line, empty);
                if (empty || line == null) {
                    setGraphic(null);
                    setText(null);
                    return;
                }
                Text at = new Text(CLOCK.format(line.at()) + "  ");
                at.getStyleClass().add("log-time");
                Text body = new Text(line.text());
                body.getStyleClass().addAll("log-text", "log-" + line.level().name().toLowerCase());
                TextFlow flow = new TextFlow(at, body);
                flow.setMaxWidth(530);
                setGraphic(flow);
            }
        });
        // Newest line visible without anybody scrolling.
        model.log().addListener((javafx.collections.ListChangeListener<Line>) change ->
                Platform.runLater(() -> {
                    if (!model.log().isEmpty()) {
                        list.scrollTo(model.log().size() - 1);
                    }
                }));
        return list;
    }
}
