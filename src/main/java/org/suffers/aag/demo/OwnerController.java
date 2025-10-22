package org.suffers.aag.demo;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Modality;
import javafx.stage.Screen;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;


public class OwnerController {
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
    private ComboBox<User> adminComboBox;
    @FXML
    private ComboBox<User> assigneeComboBox;
    @FXML
    private ComboBox<User> adminActionComboBox;
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

    public OwnerController(User user) {
        currentUser = user;
    }

    @FXML
    public void initialize() {
        configureMainTable();
        configureArchiveTable();
    }

    private void configureMainTable() {

        titleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        createdAtColumn.setCellValueFactory(cellData -> {
            LocalDateTime createdAt = cellData.getValue().getCreatedAt();
            String formatted = createdAt != null ? formatDateTime(createdAt) : "";
            return new SimpleStringProperty(formatted);
        });

        // Для titleColumn
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

            private boolean isValid(String value) {
                return value != null && !value.isEmpty() && value.length() <= 255;
            }
        });

        // Для descriptionColumn
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

            private boolean isValid(String value) {
                return value != null && !value.isEmpty() && value.length() <= 255;
            }
        });

        allUsers.addListener((ListChangeListener<User>) change -> {
            refreshUserDisplays();
        });

        allUsers.setAll(jdbcDao.getAllUsers());

        creatorColumn.setCellValueFactory(cellData ->
                new SimpleObjectProperty<>(cellData.getValue().getCreator()));
        creatorColumn.setCellFactory(createUserComboBoxCell(true));

        assigneeColumn.setCellValueFactory(cellData ->
                new SimpleObjectProperty<>(cellData.getValue().getAssignee()));
        assigneeColumn.setCellFactory(createUserComboBoxCell(false));

        deadlineColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getDeadline().toString()));
        deadlineColumn.setCellFactory(createDeadlineCellFactory());
        //deadlineColumn.setOnEditCommit(handleEditCommit());

        adminComboBox.setItems(allUsers.filtered(u -> "ADMIN".equals(u.getRole())));
        assigneeComboBox.setItems(allUsers.filtered(u -> "PROGRAMMER".equals(u.getRole())));
        adminActionComboBox.setItems(allUsers.filtered(u -> !"OWNER".equals(u.getRole())));

        // Загрузка данных
        tasksTable.setItems(FXCollections.observableArrayList(jdbcDao.getAllTasksByMonth()));
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

        // Форматировщик для даты
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        // Создаем таблицу
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

        // Если createdAt также LocalDateTime, можно его тоже отформатировать
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
            row.setStyle(""); // Сбрасываем стиль для пустых строк
            return;
        }

        // Сбрасываем стиль перед применением новых правил
        row.setStyle("");

        boolean isClosed = "CLOSED".equalsIgnoreCase(task.getStatus());
        boolean isOpen = "OPEN".equalsIgnoreCase(task.getStatus());

        try {
            LocalDateTime deadline = task.getDeadline(); // Получаем уже как LocalDateTime
            LocalDateTime now = LocalDateTime.now();

            if (isOpen) {
                if (deadline.isBefore(now)) {
                    // Просроченные задачи - красный фон
                    row.setStyle("-fx-background-color: #ffdddd;");
                } else if (deadline.isBefore(now.plusDays(1))) {
                    // Задачи на сегодня - оранжевый фон
                    row.setStyle("-fx-background-color: #fff3cd;");
                } else if (deadline.isBefore(now.plusDays(3))) {
                    // Задачи на ближайшие 3 дня - желтый фон
                    row.setStyle("-fx-background-color: #ffffcc;");
                } else {
                    // Остальные открытые задачи - зеленый фон
                    row.setStyle("-fx-background-color: #ddffdd;");
                }
            } else if (isClosed) {
                // Закрытые задачи - серый фон
                row.setStyle("-fx-background-color: #f0f0f0;");
            }
        } catch (NullPointerException e) {
            System.err.println("Deadline is null for task: " + task.getId());
            if (isClosed) {
                row.setStyle("-fx-background-color: #f0f0f0;");
            }
        }
    }

    private Callback<TableColumn<Task, User>, TableCell<Task, User>> createUserComboBoxCell(boolean isCreatorColumn) {
        return column -> new TableCell<Task, User>() {
            private final ComboBox<User> comboBox = new ComboBox<>();
            private final FilteredList<User> filteredUsers = new FilteredList<>(allUsers);
            private User previousCreator;

            {
                // Настройка ComboBox
                comboBox.getStyleClass().add("table-combo-box");

                // Настройка отображения элементов в выпадающем списке
                comboBox.setCellFactory(lv -> new ListCell<User>() {
                    @Override
                    protected void updateItem(User user, boolean empty) {
                        super.updateItem(user, empty);
                        setText(empty || user == null ? "" : user.getUsername());
                    }
                });

                // Настройка отображения выбранного элемента
                comboBox.setButtonCell(new ListCell<User>() {
                    @Override
                    protected void updateItem(User user, boolean empty) {
                        super.updateItem(user, empty);
                        setText(empty || user == null ? "Выберите..." : user.getUsername());
                    }
                });

                // Обработчик изменения значения
                comboBox.setOnAction(event -> {
                    if (isEditing()) {
                        handleComboBoxChange();
                    }
                });

                // Обработчик потери фокуса ячейкой
                focusedProperty().addListener((obs, oldVal, newVal) -> {
                    if (!newVal && isEditing()) {
                        commitEdit(getItem());
                    }
                });

                // Обработчик потери фокуса ComboBox
                comboBox.focusedProperty().addListener((obs, oldVal, newVal) -> {
                    if (!newVal && isEditing()) {
                        commitEdit(comboBox.getValue());
                    }
                });

                // Обработчик клика вне ComboBox
                comboBox.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
                    if (isEditing() && !comboBox.isShowing()) {
                        commitEdit(comboBox.getValue());
                    }
                });
            }

            @Override
            public void startEdit() {
                if (!isEmpty()) {
                    super.startEdit();

                    // Настройка ComboBox перед редактированием
                    Task task = getTableView().getItems().get(getIndex());
                    previousCreator = task != null ? task.getCreator() : null;

                    // Фильтрация пользователей в зависимости от типа колонки
                    if (isCreatorColumn) {
                        filteredUsers.setPredicate(u -> true); // Все пользователи для колонки создателя
                    } else {
                        // Фильтрация исполнителей в зависимости от создателя задачи
                        if (task != null && task.getCreator() != null) {
                            String creatorRole = task.getCreator().getRole();
                            if ("OWNER".equalsIgnoreCase(creatorRole)) {
                                filteredUsers.setPredicate(u -> true); // Все пользователи для OWNER
                            } else {
                                filteredUsers.setPredicate(u ->
                                        getAvailableAssignees(task.getCreator()).contains(u));
                            }
                        }
                    }

                    comboBox.setItems(filteredUsers);
                    comboBox.setValue(getItem());

                    setText(null);
                    setGraphic(comboBox);
                    setStyle("-fx-border-color: #d0e3f0; -fx-background-color: white;");

                    // Автоматическое открытие выпадающего списка и установка фокуса
                    Platform.runLater(() -> {
                        comboBox.show();
                        comboBox.requestFocus();
                    });
                }
            }

            @Override
            public void cancelEdit() {
                super.cancelEdit();
                setText(getItem() != null ? getItem().getUsername() : "");
                setGraphic(null);
                setStyle("-fx-border-color: transparent; -fx-background-color: transparent;");
            }

            @Override
            protected void updateItem(User item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    if (isEditing()) {
                        comboBox.setValue(item);
                        setText(null);
                        setGraphic(comboBox);
                        setStyle("-fx-border-color: #d0e3f0; -fx-background-color: white;");
                    } else {
                        setText(item.getUsername());
                        setGraphic(null);
                        setStyle("-fx-border-color: transparent; -fx-background-color: transparent;");
                    }
                }
            }

            private void handleComboBoxChange() {
                User selected = comboBox.getValue();
                Task task = getTableView().getItems().get(getIndex());

                if (task == null) {
                    return;
                }

                if (selected == null) {
                    comboBox.setValue(getItem());
                    return;
                }

                if (isCreatorColumn) {
                    handleCreatorChange(task, selected);
                } else {
                    handleAssigneeChange(task, selected);
                }

                // Автоматическое завершение редактирования после выбора
                commitEdit(selected);
            }

            private void handleCreatorChange(Task task, User newCreator) {
                previousCreator = task.getCreator();
                task.setCreator(newCreator);
                task.markModified();

                // Обновление исполнителя при смене создателя
                updateAssigneeForNewCreator(task, newCreator);
            }

            private void handleAssigneeChange(Task task, User selected) {
                User creator = task.getCreator();

                // Валидация только для НЕ OWNER создателей
                if (creator != null && !"OWNER".equalsIgnoreCase(creator.getRole())) {
                    if (!isAssigneeValid(creator, selected)) {
                        showStyledAlert("Недопустимый выбор исполнителя!", Alert.AlertType.WARNING);
                        comboBox.setValue(task.getAssignee());
                        return;
                    }
                }

                task.setAssignee(selected);
                task.markModified();
            }

            private boolean isAssigneeValid(User creator, User assignee) {
                if ("ADMIN".equalsIgnoreCase(creator.getRole())) {
                    return assignee.getId() == creator.getId() ||
                            (assignee.getSuperiorId() == creator.getId() &&
                                    "PROGRAMMER".equalsIgnoreCase(assignee.getRole()));
                }

                if ("PROGRAMMER".equalsIgnoreCase(creator.getRole())) {
                    return assignee.getId() == creator.getId();
                }

                return true;
            }

            private void updateAssigneeForNewCreator(Task task, User newCreator) {
                List<User> availableAssignees = getAvailableAssignees(newCreator);

                if (availableAssignees.isEmpty()) {
                    task.setAssignee(null);
                    return;
                }

                User currentAssignee = task.getAssignee();
                boolean isValid = currentAssignee != null &&
                        availableAssignees.contains(currentAssignee);

                if (!isValid) {
                    User defaultAssignee = getDefaultAssignee(newCreator, availableAssignees);
                    task.setAssignee(defaultAssignee);
                    task.markModified();
                }

                getTableView().refresh();
            }

            private User getDefaultAssignee(User creator, List<User> available) {
                return "ADMIN".equalsIgnoreCase(creator.getRole()) ? creator : available.get(0);
            }

            private List<User> getAvailableAssignees(User creator) {
                List<User> assignees = new ArrayList<>();

                if (creator == null) return assignees;

                if ("OWNER".equalsIgnoreCase(creator.getRole())) {
                    assignees.addAll(allUsers);
                } else if ("ADMIN".equalsIgnoreCase(creator.getRole())) {
                    assignees.add(creator);
                    assignees.addAll(allUsers.filtered(u ->
                            u.getSuperiorId() == creator.getId() &&
                                    "PROGRAMMER".equalsIgnoreCase(u.getRole())));
                } else if ("PROGRAMMER".equalsIgnoreCase(creator.getRole())) {
                    assignees.add(creator);
                }

                return assignees;
            }
        };
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
                // Добавляем CSS для календаря
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
                if (!isEmpty()) {
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
                    // Преобразуем сохраненное значение к отображаемому формату
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

                // Фикс для кнопки календаря
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

        // 1. Создаем индикатор загрузки
        ProgressIndicator progress = new ProgressIndicator();
        progress.setMaxSize(30, 30);

        // 2. Заменяем кнопку на индикатор (анимация)
        StackPane stackPane = new StackPane(progress);
        stackPane.setPrefSize(btnSave.getWidth(), btnSave.getHeight());

        HBox buttonContainer = (HBox) btnSave.getParent();
        int btnIndex = buttonContainer.getChildren().indexOf(btnSave);
        buttonContainer.getChildren().set(btnIndex, stackPane);

        // 3. Сохраняем данные
        List<Task> tasksToSave = tasksTable.getItems().stream()
                .filter(task -> task.isModified() || task.isNew())
                .toList();

        List<User> usersToSave = allUsers.stream()
                .filter(u -> u.isNew() || u.isModified())
                .toList();

        Timeline saveTimeline = new Timeline();
        for (int i = 0; i < usersToSave.size(); i++) {
            User user = usersToSave.get(i);
            saveTimeline.getKeyFrames().add(
                    new KeyFrame(Duration.millis(50 * i),
                            e -> {
                                if (user.isNew()) {
                                    User savedUser = jdbcDao.createUser(user);
                                    user.setId(savedUser.getId()); // Обновляем ID
                                    user.setIsNew(false);
                                } else {
                                    jdbcDao.updateUser(user);
                                }
                                user.setModified(false);
                            }
                    )
            );
        }

        for (int i = 0; i < tasksToSave.size(); i++) {
            Task task = tasksToSave.get(i);
            saveTimeline.getKeyFrames().add(
                    new KeyFrame(Duration.millis(50 * i),
                            e -> {
                                if (task.isNew()) {
                                    // Вставляем новую задачу
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

        // 4. Восстанавливаем кнопку и показываем уведомление
        saveTimeline.getKeyFrames().add(
                new KeyFrame(Duration.millis(50 * tasksToSave.size() + 300),
                        e -> {
                            buttonContainer.getChildren().set(btnIndex, btnSave);
                            btnSave.setDisable(false);
                            tasksTable.refresh();
                            allUsers.setAll(jdbcDao.getAllUsers());
                            refreshUserDisplays();
                            showNotification(tasksTable.getScene());
                        }
                )
        );
        saveTimeline.play();
    }

    private void showNotification(Scene scene) {
        // 1. Создаем элементы UI
        Text text = new Text("Изменения успешно сохранены!");
        text.setFill(Color.WHITE);
        text.setFont(Font.font(14));

        Rectangle background = new Rectangle();
        background.setFill(Color.web("#4CAF50"));
        background.setArcHeight(10);
        background.setArcWidth(10);

        // 2. Быстрое вычисление размеров
        text.snapshot(null, null); // Форсируем рендеринг для получения размеров
        double padding = 20;
        background.setWidth(text.getLayoutBounds().getWidth() + padding * 2);
        background.setHeight(text.getLayoutBounds().getHeight() + padding);

        // 3. Позиционирование
        text.setLayoutX(padding);
        text.setLayoutY(text.getLayoutBounds().getHeight() + padding / 2 - 2);

        // 4. Создаем группу и контейнер
        StackPane container = new StackPane(new Group(background, text));
        container.setAlignment(Pos.TOP_CENTER);
        container.setPadding(new Insets(20, 0, 0, 1050));
        container.setMouseTransparent(true);

        // 5. Добавляем в сцену
        Pane rootPane = (Pane) scene.getRoot();
        rootPane.getChildren().add(container);

        // 6. Оптимизированная анимация (1 Timeline вместо 4 анимаций)
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
            tasksTable.refresh();
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

        // Поля формы
        TextField titleField = new TextField();
        titleField.getStyleClass().add("dialog-field");

        TextArea descriptionField = new TextArea();
        descriptionField.setWrapText(true);
        descriptionField.getStyleClass().add("dialog-field");
        descriptionField.setPrefRowCount(3);

        ComboBox<User> assigneeComboBox = new ComboBox<>();
        assigneeComboBox.getStyleClass().add("dialog-combo");
        assigneeComboBox.setItems(allUsers);

        ComboBox<User> adminComboBox = new ComboBox<>();
        adminComboBox.getStyleClass().add("dialog-combo");
        adminComboBox.setDisable(true);

        DatePicker deadlineDatePicker = new DatePicker();
        deadlineDatePicker.getStyleClass().add("dialog-datepicker");
        deadlineDatePicker.setValue(LocalDate.now());

        // Поле для ввода времени с валидацией
        TextField timeField = new TextField();
        timeField.getStyleClass().add("dialog-field");
        timeField.setPromptText("HH:mm");
        timeField.setText("18:00");

        // Валидатор для времени
        timeField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("^([0-1]?[0-9]|2[0-3]):[0-5][0-9]$")) {
                timeField.setStyle("-fx-text-fill: red;");
            } else {
                timeField.setStyle("");
            }
        });

        // Настройка отображения пользователей
        Callback<ListView<User>, ListCell<User>> userCellFactory = lv -> new ListCell<User>() {
            @Override
            protected void updateItem(User user, boolean empty) {
                super.updateItem(user, empty);
                setText(empty || user == null ? null : user.getUsername());
            }
        };

        assigneeComboBox.setCellFactory(userCellFactory);
        assigneeComboBox.setButtonCell(userCellFactory.call(null));
        adminComboBox.setCellFactory(userCellFactory);
        adminComboBox.setButtonCell(userCellFactory.call(null));

        // Обработчик изменения выбора исполнителя
        assigneeComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            adminComboBox.getItems().clear();

            if (newVal != null) {
                if ("PROGRAMMER".equals(newVal.getRole())) {
                    adminComboBox.setDisable(false);
                    // Добавляем текущего пользователя и его администратора (если есть)
                    ObservableList<User> allowedAdmins = FXCollections.observableArrayList();
                    allowedAdmins.add(allUsers.filtered(u -> u.getId() == newVal.getSuperiorId()).get(0));
                    if (!allowedAdmins.contains(currentUser)) {
                        allowedAdmins.add(currentUser);
                    }

                    if (currentUser.getSuperiorId() > 0) {
                        allUsers.stream()
                                .filter(u -> u.getId() == currentUser.getSuperiorId())
                                .findFirst()
                                .ifPresent(allowedAdmins::add);
                    }

                    adminComboBox.setItems(allowedAdmins);
                    adminComboBox.getSelectionModel().select(currentUser);
                } else {
                    adminComboBox.setDisable(true);
                    adminComboBox.getSelectionModel().clearSelection();
                }
            }
        });

        // Добавление элементов в сетку
        grid.add(new Label("Название:"), 0, 0);
        grid.add(titleField, 1, 0);
        grid.add(new Label("Описание:"), 0, 1);
        grid.add(descriptionField, 1, 1);
        grid.add(new Label("Исполнитель:"), 0, 2);
        grid.add(assigneeComboBox, 1, 2);
        grid.add(new Label("Администратор:"), 0, 3);
        grid.add(adminComboBox, 1, 3);
        grid.add(new Label("Дедлайн (дата):"), 0, 4);
        grid.add(deadlineDatePicker, 1, 4);
        grid.add(new Label("Дедлайн (время):"), 0, 5);
        grid.add(timeField, 1, 5);

        dialog.getDialogPane().setContent(grid);

        // Кнопки
        ButtonType createButton = new ButtonType("Создать", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButtonType = new ButtonType("Отмена", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(createButton, cancelButtonType);

        // Стилизация кнопок
        dialog.getDialogPane().lookupButton(createButton).getStyleClass().add("action-button");
        dialog.getDialogPane().lookupButton(cancelButtonType).getStyleClass().add("dialog-button");

        dialog.setResultConverter(buttonType -> {
            if (buttonType == createButton) {
                // Валидация данных
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

                if (assigneeComboBox.getValue() == null) {
                    showStyledAlert("Выберите исполнителя", Alert.AlertType.ERROR);
                    return null;
                }

                // Валидация времени
                if (!timeField.getText().matches("^([0-1]?[0-9]|2[0-3]):[0-5][0-9]$")) {
                    showStyledAlert("Введите корректное время в формате HH:mm", Alert.AlertType.ERROR);
                    return null;
                }

                // Парсинг времени
                LocalTime time;
                try {
                    time = LocalTime.parse(timeField.getText());
                } catch (DateTimeParseException e) {
                    showStyledAlert("Некорректный формат времени", Alert.AlertType.ERROR);
                    return null;
                }

                // Проверка даты и времени
                LocalDate date = deadlineDatePicker.getValue();
                LocalDateTime deadline = LocalDateTime.of(date, time);

                if (deadline.isBefore(LocalDateTime.now())) {
                    showStyledAlert("Дедлайн не может быть раньше текущего времени", Alert.AlertType.ERROR);
                    return null;
                }

                User assignee = assigneeComboBox.getValue();
                User admin;

                if ("ADMIN".equals(assignee.getRole()) || "OWNER".equals(assignee.getRole())) {
                    admin = currentUser;
                } else {
                    if (adminComboBox.getValue() == null) {
                        showStyledAlert("Для программиста необходимо выбрать администратора", Alert.AlertType.ERROR);
                        return null;
                    }
                    admin = adminComboBox.getValue();
                }

                return new Task(
                        0,
                        titleField.getText(),
                        descriptionField.getText(),
                        "OPEN",
                        LocalDateTime.now(),
                        admin,
                        assignee,
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

        // Добавляем стили
        alert.getDialogPane().getStylesheets().add(
                Objects.requireNonNull(getClass().getResource("css/style.css")).toExternalForm()
        );
        alert.getDialogPane().getStyleClass().add("custom-alert");

        // Применяем стиль для кнопки OK
        Button okButton = (Button) alert.getDialogPane().lookupButton(ButtonType.OK);
        if (okButton != null) {
            okButton.getStyleClass().add("action-button");
        }

        alert.showAndWait();
    }

    @FXML
    private void createNewUser(ActionEvent event) {
        // Первый диалог - ввод имени пользователя
        Dialog<String> nameDialog = new Dialog<>();
        nameDialog.initStyle(StageStyle.UTILITY);
        nameDialog.setTitle("Новый пользователь");
        nameDialog.getDialogPane().getStylesheets().add(
                Objects.requireNonNull(getClass().getResource("css/style.css")).toExternalForm()
        );
        nameDialog.getDialogPane().getStyleClass().add("custom-dialog");

        GridPane nameGrid = new GridPane();
        nameGrid.setHgap(10);
        nameGrid.setVgap(10);
        nameGrid.setPadding(new Insets(20));
        nameGrid.getStyleClass().add("dialog-grid");

        TextField usernameField = new TextField();
        usernameField.getStyleClass().add("dialog-field");
        usernameField.setPromptText("Введите логин");

        nameGrid.add(new Label("Логин:"), 0, 0);
        nameGrid.add(usernameField, 1, 0);

        nameDialog.getDialogPane().setContent(nameGrid);

        // Создаем кастомные кнопки
        ButtonType nextButtonType = new ButtonType("Далее", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButtonType = new ButtonType("Отмена", ButtonBar.ButtonData.CANCEL_CLOSE);
        nameDialog.getDialogPane().getButtonTypes().addAll(nextButtonType, cancelButtonType);

        // Применяем стиль к кнопкам
        Button nextButton = (Button) nameDialog.getDialogPane().lookupButton(nextButtonType);
        Button cancelButton = (Button) nameDialog.getDialogPane().lookupButton(cancelButtonType);
        nextButton.getStyleClass().addAll("dialog-button", "action-button");
        cancelButton.getStyleClass().add("dialog-button");

        nameDialog.setResultConverter(buttonType -> {
            if (buttonType == nextButtonType) {
                String username = usernameField.getText();
                boolean exists = allUsers.stream()
                        .anyMatch(u -> u.getUsername().equalsIgnoreCase(username));

                if (username.length() < 4 || username.length() > 255) {
                    showStyledAlert("Логин должен быть от 4 до 255 символов", Alert.AlertType.ERROR);
                    return null;
                }

                if (exists) {
                    showStyledAlert("Пользователь с таким именем уже существует", Alert.AlertType.ERROR);
                    return null;
                }

                return username;
            }
            return null;
        });

        Optional<String> nameResult = nameDialog.showAndWait();
        nameResult.ifPresent(username -> {
            // Второй диалог - выбор роли
            Dialog<String> roleDialog = new Dialog<>();
            roleDialog.setTitle("Выбор роли");
            roleDialog.initStyle(StageStyle.UTILITY);
            roleDialog.getDialogPane().getStylesheets().add(
                    Objects.requireNonNull(getClass().getResource("css/style.css")).toExternalForm()
            );
            roleDialog.getDialogPane().getStyleClass().add("custom-dialog");

            GridPane roleGrid = new GridPane();
            roleGrid.setHgap(10);
            roleGrid.setVgap(10);
            roleGrid.setPadding(new Insets(20));
            roleGrid.getStyleClass().add("dialog-grid");

            ComboBox<String> roleCombo = new ComboBox<>();
            roleCombo.getStyleClass().add("dialog-combo");
            roleCombo.getItems().addAll("ADMIN", "PROGRAMMER");
            roleCombo.setValue("PROGRAMMER");

            roleGrid.add(new Label("Роль для " + username + ":"), 0, 0);
            roleGrid.add(roleCombo, 1, 0);

            roleDialog.getDialogPane().setContent(roleGrid);

            ButtonType roleNextButtonType = new ButtonType("Далее", ButtonBar.ButtonData.OK_DONE);
            ButtonType roleCancelButtonType = new ButtonType("Отмена", ButtonBar.ButtonData.CANCEL_CLOSE);
            roleDialog.getDialogPane().getButtonTypes().addAll(roleNextButtonType, roleCancelButtonType);

            // Стилизация кнопок
            Button roleNextButton = (Button) roleDialog.getDialogPane().lookupButton(roleNextButtonType);
            Button roleCancelButton = (Button) roleDialog.getDialogPane().lookupButton(roleCancelButtonType);
            roleNextButton.getStyleClass().addAll("dialog-button", "action-button");
            roleCancelButton.getStyleClass().add("dialog-button");

            roleDialog.setResultConverter(buttonType -> {
                if (buttonType == roleNextButtonType) {
                    return roleCombo.getValue();
                }
                return null;
            });

            Optional<String> roleResult = roleDialog.showAndWait();
            roleResult.ifPresent(role -> {
                User newUser = new User(0, username, "", role, currentUser.getId());
                newUser.setIsNew(true);
                newUser.setModified(true);
                String password = PasswordManager.generatePassword();
                newUser.setPassword(PasswordManager.hashPassword(password));

                if ("PROGRAMMER".equals(role)) {
                    List<User> admins = allUsers.filtered(u -> "ADMIN".equals(u.getRole()));
                    if (admins.isEmpty()) {
                        showStyledAlert("Нет доступных администраторов", Alert.AlertType.ERROR);
                        return;
                    }

                    // Третий диалог - выбор администратора
                    Dialog<User> adminDialog = new Dialog<>();
                    adminDialog.setTitle("Выбор администратора");
                    adminDialog.getDialogPane().getStylesheets().add(
                            getClass().getResource("css/style.css").toExternalForm()
                    );
                    adminDialog.getDialogPane().getStyleClass().add("custom-dialog");

                    GridPane adminGrid = new GridPane();
                    adminGrid.setHgap(10);
                    adminGrid.setVgap(10);
                    adminGrid.setPadding(new Insets(20));
                    adminGrid.getStyleClass().add("dialog-grid");

                    ComboBox<User> adminCombo = new ComboBox<>();
                    adminCombo.getStyleClass().add("dialog-combo");
                    adminCombo.setItems(FXCollections.observableArrayList(admins));
                    adminCombo.setValue(admins.get(0));


                    adminGrid.add(new Label("Выберите администратора:"), 0, 0);
                    adminGrid.add(adminCombo, 1, 0);

                    adminDialog.getDialogPane().setContent(adminGrid);

                    ButtonType finishButtonType = new ButtonType("Готово", ButtonBar.ButtonData.OK_DONE);
                    ButtonType adminCancelButtonType = new ButtonType("Отмена", ButtonBar.ButtonData.CANCEL_CLOSE);
                    adminDialog.getDialogPane().getButtonTypes().addAll(finishButtonType, adminCancelButtonType);

                    // Стилизация кнопок
                    Button finishButton = (Button) adminDialog.getDialogPane().lookupButton(finishButtonType);
                    Button adminCancelButton = (Button) adminDialog.getDialogPane().lookupButton(adminCancelButtonType);
                    finishButton.getStyleClass().addAll("dialog-button", "action-button");
                    adminCancelButton.getStyleClass().add("dialog-button");

                    adminDialog.setResultConverter(buttonType -> {
                        if (buttonType == finishButtonType) {
                            return adminCombo.getValue();
                        }
                        return null;
                    });

                    adminDialog.showAndWait().ifPresent(admin -> {
                        newUser.setSuperiorId(admin.getId());
                        completeUserCreation(newUser, username, password);
                    });
                } else {
                    newUser.setSuperiorId(currentUser.getId());
                    completeUserCreation(newUser, username, password);
                }
            });
        });
    }

    private void completeUserCreation(User newUser, String username, String password) {
        allUsers.add(newUser);
        refreshUserDisplays();

        // Диалог с учетными данными
        Dialog<Void> credentialsDialog = new Dialog<>();
        credentialsDialog.setTitle("Учетные данные");
        credentialsDialog.initStyle(StageStyle.UTILITY);
        credentialsDialog.getDialogPane().getStylesheets().add(
                getClass().getResource("css/style.css").toExternalForm()
        );
        credentialsDialog.getDialogPane().getStyleClass().add("custom-dialog");

        // Основной контейнер
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));
        grid.getStyleClass().add("dialog-grid");

        // Поля для отображения данных
        TextField usernameField = new TextField(username);
        usernameField.setEditable(false);
        usernameField.getStyleClass().add("dialog-field");

        TextField passwordField = new TextField(password);
        passwordField.setEditable(false);
        passwordField.getStyleClass().add("dialog-field");

        // Кнопка копирования
        Button copyButton = new Button();
        copyButton.getStyleClass().addAll("dialog-button", "icon-button");
        try {
            ImageView copyIcon = new ImageView(new Image(Objects.requireNonNull(getClass().getResourceAsStream("icons/copy.png"))));
            copyIcon.setFitWidth(16);
            copyIcon.setFitHeight(16);
            copyButton.setGraphic(copyIcon);
            copyButton.setTooltip(new Tooltip("Скопировать пароль"));
        } catch (Exception e) {
            copyButton.setText("Копировать");
        }

        // Кнопка закрытия
        ButtonType closeButtonType = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
        credentialsDialog.getDialogPane().getButtonTypes().add(closeButtonType);

        // Стилизация стандартной кнопки
        Node closeButtonNode = credentialsDialog.getDialogPane().lookupButton(closeButtonType);
        closeButtonNode.getStyleClass().addAll("dialog-button", "action-button");

        // Обработчики событий
        copyButton.setOnAction(e -> {
            Clipboard clipboard = Clipboard.getSystemClipboard();
            ClipboardContent content = new ClipboardContent();
            content.putString(password);
            clipboard.setContent(content);
            showToast("Пароль скопирован");
        });

        // Расположение элементов
        HBox buttonBox = new HBox(10, copyButton);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);

        grid.add(new Label("Логин:"), 0, 0);
        grid.add(usernameField, 1, 0);
        grid.add(new Label("Пароль:"), 0, 1);
        grid.add(passwordField, 1, 1);
        grid.add(buttonBox, 0, 2, 2, 1);
        grid.getStyleClass().add("dialog-grid");

        credentialsDialog.getDialogPane().setContent(grid);
        credentialsDialog.setResultConverter(buttonType -> null);

        credentialsDialog.showAndWait();
    }

    // Метод для показа временного уведомления
    private void showToast(String message) {
        Stage toast = new Stage();
        toast.initStyle(StageStyle.TRANSPARENT);

        Label label = new Label(message);
        label.setStyle("-fx-background-color: rgba(0,0,0,0.7); -fx-text-fill: white; -fx-padding: 10px; -fx-background-radius: 5;");

        Scene scene = new Scene(label);
        scene.setFill(Color.TRANSPARENT);
        toast.setScene(scene);

        // Позиционирование по центру экрана
        Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
        toast.setX((screenBounds.getWidth() - label.getWidth()) / 2);
        toast.setY(screenBounds.getHeight() + 1000);

        toast.show();

        // Автоматическое закрытие через 2 секунды
        new Timeline(new KeyFrame(
                Duration.seconds(2),
                ae -> toast.close()
        )).play();
    }

    private void refreshUserDisplays() {
        ObservableList<User> admins = allUsers.filtered(u ->
                "ADMIN".equalsIgnoreCase(u.getRole())
        );
        ObservableList<User> programmers = allUsers.filtered(u ->
                "PROGRAMMER".equalsIgnoreCase(u.getRole())
        );

        adminComboBox.setItems(admins);
        assigneeComboBox.setItems(programmers);

        tasksTable.refresh();
    }

    @FXML
    private void assignDeveloperToAdmin(ActionEvent event) {
        User admin = adminComboBox.getValue();
        User programmer = assigneeComboBox.getValue();

        if (admin == null || programmer == null) {
            showStyledAlert("Выберите администратора и программиста", Alert.AlertType.ERROR);
            return;
        }

        // Проверяем, действительно ли изменился администратор
        if (programmer.getSuperiorId() == admin.getId()) {
            showStyledAlert("Этот программист уже назначен выбранному администратору", Alert.AlertType.INFORMATION);
            return;
        }

        // 1. Обновляем программиста в ObservableList и помечаем как измененного
        programmer.setSuperiorId(admin.getId());
        programmer.setModified(true); // Помечаем пользователя как измененного

        // 2. Обновляем задачи программиста в ObservableList
        for (Task task : tasksTable.getItems()) {
            if (task.getAssigneeId() == programmer.getId() && task.getCreatorId() != admin.getId() && task.getCreatorId() != programmer.getId()) {
                task.setCreator(admin);
                task.markModified(); // Помечаем задачу как измененную
            }
        }

        tasksTable.refresh();
    }

    @FXML
    public void assignAdmin(ActionEvent event) {
        User selectedUser = adminActionComboBox.getSelectionModel().getSelectedItem();

        if (selectedUser == null) {
            showStyledAlert("Пожалуйста, выберите пользователя из списка", Alert.AlertType.WARNING);
            return;
        }

        if (selectedUser.getRole().equals("ADMIN")) {
            showStyledAlert("Текущий пользователь уже является администратором", Alert.AlertType.ERROR);
            return;
        }

        // Меняем роль пользователя
        selectedUser.setRole("ADMIN");
        selectedUser.setSuperiorId(currentUser.getId());
        selectedUser.setModified(true);
        System.out.println(selectedUser);
        // Обновляем задачи где пользователь был исполнителем
        List<Task> modifiedTasks = tasksTable.getItems().stream()
                .filter(task -> selectedUser.equals(task.getAssignee()))
                .peek(task -> {
                    task.setCreator(currentUser);
                    task.markModified();
                }).toList();


        if (!modifiedTasks.isEmpty()) {
            tasksTable.refresh();
        }
        refreshData();

        showStyledAlert(String.format("Пользователь %s назначен администратором. Обновлено %d задач",
                selectedUser.getUsername(), modifiedTasks.size()), Alert.AlertType.INFORMATION);
    }

    @FXML
    public void revokeAdmin(ActionEvent event) {
        User adminToRevoke = adminActionComboBox.getSelectionModel().getSelectedItem();

        if (adminToRevoke == null) {
            showStyledAlert("Выберите администратора для снятия", Alert.AlertType.WARNING);
            return;
        }

        if (!adminToRevoke.getRole().equals("ADMIN")) {
            showStyledAlert("Текущий пользователь не является администратором", Alert.AlertType.ERROR);
            return;
        }

        // Проверяем есть ли подчиненные
        List<User> subordinates = allUsers.filtered(u ->
                u.getSuperiorId() == adminToRevoke.getId() &&
                        "PROGRAMMER".equalsIgnoreCase(u.getRole())
        );

        User newAdminForSubordinates = null;
        if (!subordinates.isEmpty()) {
            // Диалог для выбора нового администратора для подчиненных
            Dialog<User> subordinatesDialog = createAdminSelectionDialog(
                    "Переназначение подчиненных",
                    "У администратора есть подчиненные. Выберите нового руководителя:",
                    adminToRevoke
            );

            Optional<User> result = subordinatesDialog.showAndWait();
            if (result.isEmpty() || result.get() == null) return;

            newAdminForSubordinates = result.get();

            // Обновляем подчиненных
            subordinates.forEach(user -> {
                user.setSuperiorId(result.get().getId());
                user.setModified(true);
            });
        }

        // Диалог для выбора администратора для бывшего администратора
        Dialog<User> exAdminDialog = createAdminSelectionDialog(
                "Назначение руководителя",
                "Выберите администратора для бывшего администратора " + adminToRevoke.getUsername() + ":",
                adminToRevoke
        );

        Optional<User> exAdminResult = exAdminDialog.showAndWait();
        if (exAdminResult.isEmpty() || exAdminResult.get() == null) return;

        User newAdminForExAdmin = exAdminResult.get();
        // Обновляем бывшего администратора
        adminToRevoke.setSuperiorId(newAdminForExAdmin.getId());
        adminToRevoke.setRole("PROGRAMMER");
        adminToRevoke.setModified(true);

        // Обновляем задачи где он был создателем
        List<Task> tasks = tasksTable.getItems().stream().toList();

        for (Task task : tasks) {
            if (task.getCreator().equals(adminToRevoke)) {
                task.setCreator(newAdminForSubordinates);
                task.markModified();
            }
            if (task.getAssignee().equals(adminToRevoke)) {
                task.setCreator(newAdminForExAdmin);
                task.markModified();
            }
        }

        tasksTable.refresh();
        refreshData();

        String message = "Администратор " + adminToRevoke.getUsername() + " снят.";

        if (newAdminForSubordinates != null) {
            message += "\nПодчиненные переназначены администратору: " + newAdminForSubordinates.getUsername();
        }

        message += "\nБывший администратор подчинен: " + newAdminForExAdmin.getUsername();

        showStyledAlert(message, Alert.AlertType.INFORMATION);
    }

    private Dialog<User> createAdminSelectionDialog(String title, String header, User userToExclude) {
        Dialog<User> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.setHeaderText(header);

        // 1. Добавляем стили для всего диалога
        dialog.getDialogPane().getStylesheets().add(
                getClass().getResource("css/style.css").toExternalForm()
        );
        dialog.getDialogPane().getStyleClass().add("custom-dialog");
        dialog.initStyle(StageStyle.UTILITY);

        // 2. Настройка комбобокса
        ComboBox<User> adminCombo = new ComboBox<>();
        adminCombo.getStyleClass().add("dialog-combo");
        adminCombo.setItems(allUsers.filtered(u ->
                (u.getRole().equals("ADMIN") || u.getRole().equals("OWNER")) &&
                        !u.equals(userToExclude)
        ));

        // 3. Настройка сетки
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 10, 10, 10));
        grid.add(new Label("Администратор:"), 0, 0);
        grid.add(adminCombo, 1, 0);
        grid.getStyleClass().add("dialog-grid");

        // 4. Добавляем контент
        dialog.getDialogPane().setContent(grid);

        // 5. Настройка кнопок
        ButtonType confirmButton = new ButtonType("Подтвердить", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButton = new ButtonType("Отмена", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(confirmButton, cancelButton);

        // 6. Применяем стили к кнопкам
        Button confirmBtn = (Button) dialog.getDialogPane().lookupButton(confirmButton);
        Button cancelBtn = (Button) dialog.getDialogPane().lookupButton(cancelButton);
        confirmBtn.getStyleClass().addAll("action-button", "dialog-button");
        cancelBtn.getStyleClass().addAll("cancel-button", "dialog-button");


        dialog.setResultConverter(buttonType -> {
            if (buttonType == confirmButton) {
                return adminCombo.getSelectionModel().getSelectedItem();
            }
            return null;
        });

        return dialog;
    }

    private void refreshData() {
        adminComboBox.setItems(allUsers.filtered(u -> "ADMIN".equals(u.getRole())));
        assigneeComboBox.setItems(allUsers.filtered(u -> "PROGRAMMER".equals(u.getRole())));
    }

    @FXML
    private void showArchivedTasks() {
        Stage archiveStage = new Stage();
        archiveStage.setTitle("Архивные задачи (CLOSED > 1 месяца)");

        // Основной контейнер
        VBox root = new VBox(10);
        root.setPadding(new Insets(15));
        root.getStyleClass().add("root");

        // Заголовок
        Label titleLabel = new Label("Архивные задачи");
        titleLabel.getStyleClass().add("section-label");

        // Настройка таблицы
        archiveTasksTable.getStyleClass().add("minimal-table");

        // Настройка ScrollPane
        ScrollPane scrollPane = new ScrollPane(archiveTasksTable);
        scrollPane.getStyleClass().add("modern-scroll");
        scrollPane.setFitToWidth(true);  // Растягиваем по ширине
        scrollPane.setFitToHeight(true); // Растягиваем по высоте

        // Привязка размеров
        scrollPane.setPrefViewportWidth(800);
        scrollPane.setPrefViewportHeight(600);
        scrollPane.prefWidthProperty().bind(root.widthProperty().subtract(30)); // 30 = padding*2
        scrollPane.prefHeightProperty().bind(root.heightProperty().subtract(70)); // Учитываем заголовок и padding

        // Сборка интерфейса
        root.getChildren().addAll(titleLabel, scrollPane);

        // Настройка сцены
        Scene scene = new Scene(root, 800, 600);
        scene.getStylesheets().add(Objects.requireNonNull(getClass().getResource("css/style.css")).toExternalForm());

        archiveStage.setScene(scene);
        archiveStage.initModality(Modality.NONE);
        archiveStage.show();
    }
}