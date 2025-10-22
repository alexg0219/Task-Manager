package org.suffers.aag.demo;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Callback;
import javafx.util.Duration;
import javafx.util.converter.DefaultStringConverter;
import org.suffers.aag.demo.entities.Task;
import org.suffers.aag.demo.entities.User;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;


public class ProgrammerController {
    private final JdbcDao jdbcDao = new JdbcDao();
    private final User currentUser;
    private final ObservableList<User> allUsers = FXCollections.observableArrayList();

    @FXML
    private TableView<Task> tasksTable;
    @FXML
    private TableColumn<Task, String> titleColumn;
    @FXML
    private TableColumn<Task, String> descriptionColumn;
    @FXML
    private TableColumn<Task, String> statusColumn;
    @FXML
    private TableColumn<Task, User> creatorColumn;
    @FXML
    private TableColumn<Task, User> assigneeColumn;
    @FXML
    private TableColumn<Task, String> createdAtColumn;
    @FXML
    private TableColumn<Task, String> deadlineColumn;
    @FXML
    private final TableView<Task> archiveTasksTable = new TableView<Task>();
    @FXML
    private final TableColumn<Task, String> archiveTitleColumn = new TableColumn<>("Название");
    @FXML
    private final TableColumn<Task, String> archiveDescriptionColumn = new TableColumn<>("Описание");
    @FXML
    private final TableColumn<Task, String> archiveStatusColumn = new TableColumn<>("Статус");
    @FXML
    private final TableColumn<Task, User> archiveCreatorColumn = new TableColumn<>("Создатель");
    @FXML
    private final TableColumn<Task, User> archiveAssigneeColumn = new TableColumn<>("Исполнитель");
    @FXML
    private final TableColumn<Task, String> archiveCreatedAtColumn = new TableColumn<>("Дата создания");
    @FXML
    private final TableColumn<Task, String> archiveDeadlineColumn = new TableColumn<>("Крайний срок выполения");

    public ProgrammerController(User currentUser) {
        this.currentUser = currentUser;
    }

    @FXML
    public void initialize() {
        configureMainTable();
        configureArchiveTable();
    }

    private boolean isRowEditable(Task task) {
        return task != null && task.getCreator().equals(currentUser);
    }

    private void configureMainTable() {

        titleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        creatorColumn.setCellValueFactory(new PropertyValueFactory<>("creator"));
        assigneeColumn.setCellValueFactory(new PropertyValueFactory<>("assignee"));
        createdAtColumn.setCellValueFactory(cellData -> {
            LocalDateTime createdAt = cellData.getValue().getCreatedAt();
            String formatted = createdAt != null ? formatDateTime(createdAt) : "";
            return new SimpleStringProperty(formatted);
        });

        titleColumn.setCellFactory(column -> new TextFieldTableCell<Task, String>(new DefaultStringConverter()) {
            @Override
            public void commitEdit(String newValue) {
                if (isValid(newValue)) {
                    super.commitEdit(newValue);
                    Task task = getTableRow().getItem();
                    if (task != null) {
                        task.setTitle(newValue);
                        task.markModified();
                    }
                } else {
                    cancelEdit();
                    getTableView().refresh();
                }
            }

            @Override
            public void startEdit() {
                if (isRowEditable(getTableRow().getItem())) {
                    super.startEdit();
                }

            }

            private boolean isValid(String value) {
                return value != null && !value.isEmpty() && value.length() <= 255;
            }
        });

        descriptionColumn.setCellFactory(column -> new TextFieldTableCell<Task, String>(new DefaultStringConverter()) {
            @Override
            public void commitEdit(String newValue) {
                if (isValid(newValue)) {
                    super.commitEdit(newValue);
                    Task task = getTableRow().getItem();
                    if (task != null) {
                        task.setDescription(newValue);
                        task.markModified();
                    }
                } else {
                    cancelEdit();
                    getTableView().refresh();
                }
            }

            @Override
            public void startEdit() {
                if (isRowEditable(getTableRow().getItem())) {
                    super.startEdit();
                }

            }

            private boolean isValid(String value) {
                return value != null && !value.isEmpty() && value.length() <= 255;
            }
        });

        deadlineColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getDeadline().toString()));
        deadlineColumn.setCellFactory(createDeadlineCellFactory());

        tasksTable.setItems(FXCollections.observableArrayList(jdbcDao.getAllTasksByMonthAndProgrammer(currentUser.getId())));
        tasksTable.setRowFactory(tv -> {
            TableRow<Task> row = new TableRow<Task>() {
                @Override
                protected void updateItem(Task task, boolean empty) {
                    super.updateItem(task, empty);
                    updateRowStyle(this, task, empty);
                }
            };
            return row;
        });
        tasksTable.setEditable(true);
    }

    private void configureArchiveTable() {
        List<Task> archivedTasks = jdbcDao.getArchivedTasks();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        archiveTitleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        archiveDescriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
        archiveStatusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        archiveCreatedAtColumn.setCellValueFactory(new PropertyValueFactory<>("createdAt"));
        archiveCreatorColumn.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().getCreator()));
        archiveAssigneeColumn.setCellValueFactory(cellData -> new SimpleObjectProperty<>(cellData.getValue().getAssignee()));
        archiveDeadlineColumn.setCellValueFactory(cellData -> {
            LocalDateTime deadline = cellData.getValue().getDeadline();
            String formattedDate = deadline != null ? deadline.format(formatter) : "";
            return new SimpleStringProperty(formattedDate);
        });

        archiveCreatedAtColumn.setCellValueFactory(cellData -> {
            LocalDateTime createdAt = cellData.getValue().getCreatedAt();
            String formattedDate = createdAt != null ? createdAt.format(formatter) : "";
            return new SimpleStringProperty(formattedDate);
        });

        archiveTasksTable.getColumns().addAll(archiveTitleColumn, archiveDescriptionColumn, archiveStatusColumn,
                archiveCreatorColumn, archiveAssigneeColumn, archiveCreatedAtColumn, archiveDeadlineColumn);
        archiveTasksTable.setItems(FXCollections.observableArrayList(archivedTasks));
    }

    private String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "";
        }
        return dateTime.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));
    }

    private void updateRowStyle(TableRow<Task> row, Task task, boolean empty) {
        if (empty || task == null) {
            row.setStyle("");
            return;
        }

        row.setStyle("");

        boolean isClosed = "CLOSED".equalsIgnoreCase(task.getStatus());
        boolean isOpen = "OPEN".equalsIgnoreCase(task.getStatus());

        try {
            LocalDateTime deadline = task.getDeadline();
            LocalDateTime now = LocalDateTime.now();

            if (isOpen) {
                if (deadline.isBefore(now)) {
                    row.setStyle("-fx-background-color: #ffdddd;");
                } else if (deadline.isBefore(now.plusDays(1))) {
                    row.setStyle("-fx-background-color: #fff3cd;");
                } else if (deadline.isBefore(now.plusDays(3))) {
                    row.setStyle("-fx-background-color: #ffffcc;");
                } else {
                    row.setStyle("-fx-background-color: #ddffdd;");
                }
            } else if (isClosed) {
                row.setStyle("-fx-background-color: #f0f0f0;");
            }
        } catch (NullPointerException e) {
            System.err.println("Deadline is null for task: " + task.getId());
            if (isClosed) {
                row.setStyle("-fx-background-color: #f0f0f0;");
            }
        }
    }

    private Callback<TableColumn<Task, String>, TableCell<Task, String>> createDeadlineCellFactory() {
        return column -> new TableCell<Task, String>() {
            private HBox editingBox;
            private DatePicker datePicker;
            private TextField timeField;
            private final DateTimeFormatter displayFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
            private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
            private String originalValue;

            {
                this.getStylesheets().add(getClass().getResource("css/style.css").toExternalForm());

                focusedProperty().addListener((obs, oldVal, newVal) -> {
                    if (!newVal && isEditing()) {
                        Platform.runLater(() -> {
                            if (datePicker != null && !datePicker.isShowing()) {
                                cancelEdit();
                            }
                        });
                    }
                });
            }

            @Override
            public void startEdit() {
                if (!isEmpty() && isRowEditable(getTableRow().getItem())) {
                    super.startEdit();
                    originalValue = getItem();
                    createEditingBox();
                    setText(null);
                    setGraphic(editingBox);

                    Platform.runLater(() -> {
                        if (datePicker != null && !datePicker.isShowing()) {
                            timeField.requestFocus();
                            timeField.selectAll();
                        }
                    });
                }
            }

            @Override
            public void cancelEdit() {
                super.cancelEdit();
                if (originalValue != null) {
                    LocalDateTime dateTime = parseDateTime(originalValue);
                    setText(formatDisplayDateTime(dateTime));
                } else {
                    setText(null);
                }
                setGraphic(null);
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    if (isEditing()) {
                        setGraphic(editingBox);
                        setText(null);
                    } else {
                        setText(formatDisplayDateTime(parseDateTime(item)));
                        setGraphic(null);
                    }
                }
            }

            private void createEditingBox() {
                Task task = getTask();
                if (task == null) return;

                LocalDateTime deadline = parseDateTime(getItem());
                LocalDateTime createdDate = task.getCreatedAt();

                datePicker = new DatePicker(deadline.toLocalDate());
                datePicker.setDayCellFactory(dp -> new DateCell() {
                    @Override
                    public void updateItem(LocalDate date, boolean empty) {
                        super.updateItem(date, empty);
                        setDisable(empty || date.isBefore(createdDate.toLocalDate()));
                    }
                });

                datePicker.addEventFilter(MouseEvent.MOUSE_PRESSED, e -> {
                    Node target = (Node) e.getTarget();
                    if (target.getStyleClass().contains("arrow-button")) {
                        datePicker.show();
                        e.consume();
                    }
                });

                timeField = new TextField(deadline.toLocalTime().format(timeFormatter));
                timeField.setPromptText("HH:mm");
                timeField.textProperty().addListener((obs, oldVal, newVal) -> {
                    if (!newVal.matches("^([0-1]?[0-9]|2[0-3]):[0-5][0-9]$")) {
                        timeField.setStyle("-fx-text-fill: red;");
                    } else {
                        timeField.setStyle("");
                    }
                });

                Button saveButton = new Button("✓");
                saveButton.getStyleClass().add("edit-button");
                saveButton.setOnAction(e -> commitChanges());

                editingBox = new HBox(5, datePicker, timeField, saveButton);
                editingBox.setAlignment(Pos.CENTER_LEFT);
            }

            private void commitChanges() {
                if (validateDateTime()) {
                    LocalDateTime newDeadline = LocalDateTime.of(
                            datePicker.getValue(),
                            LocalTime.parse(timeField.getText(), timeFormatter)
                    );

                    String formattedValue = formatDateTimeForStorage(newDeadline);
                    commitEdit(formattedValue);

                    Task task = getTask();
                    if (task != null) {
                        task.setDeadline(newDeadline);
                        task.markModified();
                    }
                }
            }

            private boolean validateDateTime() {
                try {
                    LocalDate date = datePicker.getValue();
                    LocalTime time = LocalTime.parse(timeField.getText(), timeFormatter);
                    LocalDateTime newDeadline = LocalDateTime.of(date, time);
                    LocalDateTime createdDate = getTask().getCreatedAt();

                    if (newDeadline.isBefore(createdDate)) {
                        showStyledAlert("Дедлайн не может быть раньше даты создания задачи", Alert.AlertType.ERROR);
                        return false;
                    }
                    return true;
                } catch (DateTimeParseException e) {
                    showStyledAlert("Введите время в формате HH:mm", Alert.AlertType.ERROR);
                    return false;
                }
            }

            private LocalDateTime parseDateTime(String dateTimeStr) {
                try {
                    return LocalDateTime.parse(dateTimeStr);
                } catch (DateTimeParseException e) {
                    return LocalDateTime.parse(dateTimeStr, displayFormatter);
                }
            }

            private String formatDisplayDateTime(LocalDateTime dateTime) {
                return dateTime.format(displayFormatter);
            }

            private String formatDateTimeForStorage(LocalDateTime dateTime) {
                return dateTime.toString();
            }

            private Task getTask() {
                int index = getIndex();
                return (index >= 0 && index < getTableView().getItems().size())
                        ? getTableView().getItems().get(index)
                        : null;
            }
        };
    }


    @FXML
    private void saveAllChanges(ActionEvent event) {
        Button btnSave = (Button) event.getSource();
        btnSave.setDisable(true);

        ProgressIndicator progress = new ProgressIndicator();
        progress.setMaxSize(30, 30);

        StackPane stackPane = new StackPane(progress);
        stackPane.setPrefSize(btnSave.getWidth(), btnSave.getHeight());

        HBox buttonContainer = (HBox) btnSave.getParent();
        int btnIndex = buttonContainer.getChildren().indexOf(btnSave);
        buttonContainer.getChildren().set(btnIndex, stackPane);

        List<Task> tasksToSave = tasksTable.getItems().stream()
                .filter(task -> task.isModified() || task.isNew())
                .toList();

        Timeline saveTimeline = new Timeline();
        for (int i = 0; i < tasksToSave.size(); i++) {
            Task task = tasksToSave.get(i);
            saveTimeline.getKeyFrames().add(
                    new KeyFrame(Duration.millis(50 * i),
                            e -> {
                                if (task.isNew()) {
                                    jdbcDao.createTask(task);
                                    task.setNew(false); // Сбрасываем флаг
                                } else {
                                    // Обновляем существующую задачу
                                    jdbcDao.updateTask(task);
                                }
                                task.resetModified();
                            }
                    )
            );
        }

        saveTimeline.getKeyFrames().add(
                new KeyFrame(Duration.millis(50 * tasksToSave.size() + 300),
                        e -> {
                            buttonContainer.getChildren().set(btnIndex, btnSave);
                            btnSave.setDisable(false);
                            tasksTable.refresh();
                            allUsers.setAll(jdbcDao.getAllUsers());
                            showNotification(tasksTable.getScene());
                        }
                )
        );
        saveTimeline.play();
    }

    private void showNotification(Scene scene) {
        Text text = new Text("Изменения успешно сохранены!");
        text.setFill(Color.WHITE);
        text.setFont(Font.font(14));

        Rectangle background = new Rectangle();
        background.setFill(Color.web("#4CAF50"));
        background.setArcHeight(10);
        background.setArcWidth(10);

        text.snapshot(null, null);
        double padding = 20;
        background.setWidth(text.getLayoutBounds().getWidth() + padding * 2);
        background.setHeight(text.getLayoutBounds().getHeight() + padding);

        text.setLayoutX(padding);
        text.setLayoutY(text.getLayoutBounds().getHeight() + padding / 2 - 2);

        StackPane container = new StackPane(new Group(background, text));
        container.setAlignment(Pos.TOP_CENTER);
        container.setPadding(new Insets(20, 0, 0, 1050));
        container.setMouseTransparent(true);

        Pane rootPane = (Pane) scene.getRoot();
        rootPane.getChildren().add(container);

        Timeline timeline = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(container.opacityProperty(), 0),
                        new KeyValue(container.translateYProperty(), -30)
                ),
                new KeyFrame(Duration.millis(300),
                        new KeyValue(container.opacityProperty(), 1),
                        new KeyValue(container.translateYProperty(), 0)

                ),
                new KeyFrame(Duration.millis(2300)),
                new KeyFrame(Duration.millis(2600),
                        new KeyValue(container.opacityProperty(), 0),
                        new KeyValue(container.translateYProperty(), -30)
                )
        );

        timeline.setOnFinished(e -> rootPane.getChildren().remove(container));
        timeline.play();
    }

    @FXML
    private void openTask(ActionEvent event) {
        changeTaskStatus("OPEN");
    }

    @FXML
    private void closeTask(ActionEvent event) {
        changeTaskStatus("CLOSED");
    }

    private void changeTaskStatus(String status) {
        Task selected = tasksTable.getSelectionModel().getSelectedItem();
        if (selected != null && selected.getStatus() != status) {
            System.out.println(selected.getStatus());
            if (status.equals("CLOSED")) {
                selected.setCloseAt(LocalDateTime.parse(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)));
            } else {
                selected.setCloseAt(null);
            }
            selected.setStatus(status);
            selected.markModified();
            //jdbcDao.updateTaskStatus(selected.getId(), status);
            tasksTable.refresh();
            //tasksTable.setItems(FXCollections.observableArrayList(jdbcDao.getAllTasks()));
        }
    }

    @FXML
    private void createNewTask(ActionEvent event) {
        Dialog<Task> dialog = new Dialog<>();
        dialog.setTitle("Новая задача");
        dialog.initStyle(StageStyle.UTILITY);
        dialog.getDialogPane().getStylesheets().add(
                Objects.requireNonNull(getClass().getResource("css/style.css")).toExternalForm()
        );
        dialog.getDialogPane().getStyleClass().add("custom-dialog");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        grid.getStyleClass().add("dialog-grid");


        TextField titleField = new TextField();
        titleField.getStyleClass().add("dialog-field");

        TextArea descriptionField = new TextArea();
        descriptionField.setWrapText(true);
        descriptionField.getStyleClass().add("dialog-field");
        descriptionField.setPrefRowCount(3);

        DatePicker deadlineDatePicker = new DatePicker();
        deadlineDatePicker.getStyleClass().add("dialog-datepicker");
        deadlineDatePicker.setValue(LocalDate.now());

        TextField timeField = new TextField();
        timeField.getStyleClass().add("dialog-field");
        timeField.setPromptText("HH:mm");
        timeField.setText("18:00");

        timeField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("^([0-1]?[0-9]|2[0-3]):[0-5][0-9]$")) {
                timeField.setStyle("-fx-text-fill: red;");
            } else {
                timeField.setStyle("");
            }
        });

        grid.add(new Label("Название:"), 0, 0);
        grid.add(titleField, 1, 0);
        grid.add(new Label("Описание:"), 0, 1);
        grid.add(descriptionField, 1, 1);
        grid.add(new Label("Дедлайн (дата):"), 0, 4);
        grid.add(deadlineDatePicker, 1, 4);
        grid.add(new Label("Дедлайн (время):"), 0, 5);
        grid.add(timeField, 1, 5);

        dialog.getDialogPane().setContent(grid);

        ButtonType createButton = new ButtonType("Создать", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButton = new ButtonType("Отмена", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(createButton, cancelButton);

        dialog.getDialogPane().lookupButton(createButton).getStyleClass().add("action-button");
        dialog.getDialogPane().lookupButton(cancelButton).getStyleClass().add("dialog-button");

        dialog.setResultConverter(buttonType -> {
            if (buttonType == createButton) {
                if (titleField.getText().isEmpty()) {
                    showStyledAlert("Название задачи обязательно", Alert.AlertType.ERROR);
                    return null;
                }

                if (titleField.getText().length() > 255) {
                    showStyledAlert("Название задачи не может превышать 255 символов", Alert.AlertType.ERROR);
                    return null;
                }

                if (descriptionField.getText().length() > 255) {
                    showStyledAlert("Описание задачи не может превышать 255 символов", Alert.AlertType.ERROR);
                    return null;
                }

                if (!timeField.getText().matches("^([0-1]?[0-9]|2[0-3]):[0-5][0-9]$")) {
                    showStyledAlert("Введите корректное время в формате HH:mm", Alert.AlertType.ERROR);
                    return null;
                }

                LocalTime time;
                try {
                    time = LocalTime.parse(timeField.getText());
                } catch (DateTimeParseException e) {
                    showStyledAlert("Некорректный формат времени", Alert.AlertType.ERROR);
                    return null;
                }

                LocalDate date = deadlineDatePicker.getValue();
                LocalDateTime deadline = LocalDateTime.of(date, time);

                if (deadline.isBefore(LocalDateTime.now())) {
                    showStyledAlert("Дедлайн не может быть раньше текущего времени", Alert.AlertType.ERROR);
                    return null;
                }

                return new Task(
                        0,
                        titleField.getText(),
                        descriptionField.getText(),
                        "OPEN",
                        LocalDateTime.now(),
                        currentUser,
                        currentUser,
                        false,
                        deadline,
                        null
                );
            }
            return null;
        });

        Optional<Task> result = dialog.showAndWait();
        result.ifPresent(task -> {
            task.markModified();
            task.setNew(true);
            tasksTable.getItems().add(task);
            tasksTable.refresh();
        });
    }

    private void showStyledAlert(String message, Alert.AlertType type) {
        Alert alert = new Alert(type, message);
        alert.initStyle(StageStyle.UTILITY);

        alert.getDialogPane().getStylesheets().add(
                Objects.requireNonNull(getClass().getResource("css/style.css")).toExternalForm()
        );
        alert.getDialogPane().getStyleClass().add("custom-alert");

        Button okButton = (Button) alert.getDialogPane().lookupButton(ButtonType.OK);
        if (okButton != null) {
            okButton.getStyleClass().add("action-button");
        }

        alert.showAndWait();
    }

    @FXML
    private void showArchivedTasks() {
        Stage archiveStage = new Stage();
        archiveStage.setTitle("Архивные задачи (CLOSED > 1 месяца)");

        VBox root = new VBox(10);
        root.setPadding(new Insets(15));
        root.getStyleClass().add("root");

        Label titleLabel = new Label("Архивные задачи");
        titleLabel.getStyleClass().add("section-label");

        archiveTasksTable.getStyleClass().add("minimal-table");

        ScrollPane scrollPane = new ScrollPane(archiveTasksTable);
        scrollPane.getStyleClass().add("modern-scroll");
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);

        scrollPane.setPrefViewportWidth(800);
        scrollPane.setPrefViewportHeight(600);
        scrollPane.prefWidthProperty().bind(root.widthProperty().subtract(30));
        scrollPane.prefHeightProperty().bind(root.heightProperty().subtract(70));


        root.getChildren().addAll(titleLabel, scrollPane);

        Scene scene = new Scene(root, 800, 600);
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("css/style.css")).toExternalForm());

        archiveStage.setScene(scene);
        archiveStage.initModality(Modality.NONE);
        archiveStage.show();
    }
}