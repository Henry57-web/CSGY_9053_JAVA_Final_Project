package com.habit.ui;

import com.habit.dao.HabitDao;
import com.habit.model.Habit;
import com.habit.service.ReminderService;
import com.habit.util.DatabaseHelper;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;

public class MainApp extends Application {

    private HabitDao habitDao = new HabitDao();
    private ReminderService reminderService = new ReminderService();
    private ObservableList<Habit> habitData = FXCollections.observableArrayList();

    @Override
    public void start(Stage primaryStage) {
        DatabaseHelper.initDatabase();
        reminderService.startReminders();

        TabPane mainTabPane = new TabPane();

        Tab listTab = new Tab("Habits & Tracking");
        listTab.setClosable(false);
        listTab.setContent(createHabitManagementView());

        Tab chartTab = new Tab("Analytics & Charts");
        chartTab.setClosable(false);
        chartTab.setContent(createAnalyticsView());

        chartTab.setOnSelectionChanged(e -> {
            if (chartTab.isSelected()) chartTab.setContent(createAnalyticsView());
        });

        mainTabPane.getTabs().addAll(listTab, chartTab);

        Scene scene = new Scene(mainTabPane, 800, 600);
        primaryStage.setTitle("Habit Tracker Pro (Java 8)");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private BorderPane createHabitManagementView() {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(15));

        // --- List View & Context Menu ---
        ListView<Habit> listView = new ListView<>();
        listView.setItems(habitData);
        loadData();

        ContextMenu contextMenu = new ContextMenu();
        MenuItem editItem = new MenuItem("Edit Habit");
        MenuItem deleteItem = new MenuItem("Delete Habit");
        MenuItem historyItem = new MenuItem("Check-in Past Date...");

        editItem.setOnAction(e -> showEditDialog(listView.getSelectionModel().getSelectedItem()));
        deleteItem.setOnAction(e -> deleteHabit(listView.getSelectionModel().getSelectedItem()));
        historyItem.setOnAction(e -> showPastDateDialog(listView.getSelectionModel().getSelectedItem()));

        contextMenu.getItems().addAll(editItem, historyItem, deleteItem);
        listView.setContextMenu(contextMenu);

        // --- Bottom Control Bar ---
        TextField nameInput = new TextField();
        nameInput.setPromptText("Habit Name");
        ComboBox<String> freqBox = new ComboBox<>();
        freqBox.getItems().addAll("Daily", "Weekly", "Monthly");
        freqBox.setValue("Daily");

        Button addButton = new Button("Add Habit");
        Button checkInButton = new Button("Check In Today ✅");
        checkInButton.setStyle("-fx-background-color: #90ee90; -fx-font-weight: bold;");

        HBox inputBar = new HBox(10, nameInput, freqBox, addButton, checkInButton);
        inputBar.setPadding(new Insets(10, 0, 0, 0));

        // --- TOP BAR: Data Management ---
        Button loadButton = new Button("Load Sample Data");
        loadButton.setTooltip(new Tooltip("Add 10 sample habits with history"));

        Button clearButton = new Button("Clear All Data");
        clearButton.setStyle("-fx-text-fill: red;");
        clearButton.setTooltip(new Tooltip("Delete all habits and history"));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        loadButton.setOnAction(e -> generateDemoData());
        clearButton.setOnAction(e -> clearAllData());

        HBox topBar = new HBox(10, loadButton, spacer, clearButton);
        topBar.setPadding(new Insets(0, 0, 10, 0));

        root.setTop(topBar);
        root.setCenter(listView);
        root.setBottom(inputBar);

        // Input Events
        addButton.setOnAction(e -> {
            String name = nameInput.getText();
            String freq = freqBox.getValue();
            if (name != null && !name.trim().isEmpty()) {
                habitDao.addHabit(name, freq);
                loadData();
                nameInput.clear();
            } else {
                showAlert(Alert.AlertType.WARNING, "Invalid Input", "Please enter a habit name.");
            }
        });

        // 核心逻辑修改：处理 Check-In 的返回值
        checkInButton.setOnAction(e -> {
            Habit selected = listView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                // 传入 Habit 对象，获取状态码
                int result = habitDao.checkIn(selected, LocalDate.now().toString());
                handleCheckInResult(result, selected.getName());
            } else {
                System.out.println("Check-in IGNORED: No habit selected.");
                showAlert(Alert.AlertType.WARNING, "Selection Needed", "Please select a habit to check in.");
            }
        });

        return root;
    }

    // 统一处理打卡结果（弹窗 + 后台打印）
    private void handleCheckInResult(int result, String habitName) {
        if (result == 0) {
            // 成功
            System.out.println("Check-in SUCCESS for: " + habitName);
            showAlert(Alert.AlertType.INFORMATION, "Success", "Good job! Check-in recorded.");
            loadData();
        }
        else if (result == 1) {
            // Daily 重复
            System.out.println("Check-in FAILED: Already done today -> " + habitName);
            showAlert(Alert.AlertType.WARNING, "Already Checked In", "You have already completed '" + habitName + "' today!");
        }
        else if (result == 2) {
            // Weekly 重复
            System.out.println("Check-in FAILED: Already done this week -> " + habitName);
            showAlert(Alert.AlertType.WARNING, "Weekly Limit Reached", "You have already completed this WEEKLY habit this week!");
        }
        else if (result == 3) {
            // Monthly 重复
            System.out.println("Check-in FAILED: Already done this month -> " + habitName);
            showAlert(Alert.AlertType.WARNING, "Monthly Limit Reached", "You have already completed this MONTHLY habit this month!");
        }
        else {
            // 数据库错误
            System.out.println("Check-in ERROR: Database issue.");
            showAlert(Alert.AlertType.ERROR, "Error", "Could not save check-in.");
        }
    }

    private TabPane createAnalyticsView() {
        TabPane statsPane = new TabPane();
        Tab weekTab = new Tab("Weekly (7 Days)");
        weekTab.setClosable(false);
        weekTab.setContent(createDashboard(7, "Weekly Check-ins", "Weekly Distribution"));

        Tab monthTab = new Tab("Monthly (30 Days)");
        monthTab.setClosable(false);
        monthTab.setContent(createDashboard(30, "Monthly Check-ins", "Monthly Distribution"));

        Tab allTab = new Tab("All Time");
        allTab.setClosable(false);
        allTab.setContent(createDashboard(-1, "Total Consistency", "Overall Focus"));

        statsPane.getTabs().addAll(weekTab, monthTab, allTab);
        return statsPane;
    }

    private BorderPane createDashboard(int days, String barTitle, String pieTitle) {
        BorderPane dashboard = new BorderPane();
        BorderPane barPane = createBarChart(days, barTitle);
        PieChart pieChart = createPieChart(days, pieTitle);

        VBox layout = new VBox(10);
        layout.setPadding(new Insets(10));
        barPane.prefHeightProperty().bind(layout.heightProperty().divide(2));
        pieChart.prefHeightProperty().bind(layout.heightProperty().divide(2));
        layout.getChildren().addAll(barPane, pieChart);

        dashboard.setCenter(layout);
        return dashboard;
    }

    private BorderPane createBarChart(int days, String title) {
        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Habit");
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Count");

        BarChart<String, Number> barChart = new BarChart<>(xAxis, yAxis);
        barChart.setTitle(title);
        barChart.setLegendVisible(false);

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        Map<String, Integer> stats = habitDao.getHabitStats(days);
        for (Map.Entry<String, Integer> entry : stats.entrySet()) {
            series.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
        }
        barChart.getData().add(series);
        return new BorderPane(barChart);
    }

    private PieChart createPieChart(int days, String title) {
        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
        Map<String, Integer> stats = habitDao.getHabitStats(days);
        for (Map.Entry<String, Integer> entry : stats.entrySet()) {
            if (entry.getValue() > 0) {
                pieData.add(new PieChart.Data(entry.getKey(), entry.getValue()));
            }
        }
        PieChart chart = new PieChart(pieData);
        chart.setTitle(title);
        chart.setLabelsVisible(true);
        chart.setLegendVisible(true);
        return chart;
    }

    private void showEditDialog(Habit habit) {
        if (habit == null) return;
        Dialog<Habit> dialog = new Dialog<>();
        dialog.setTitle("Edit Habit");
        dialog.setHeaderText("Update details for: " + habit.getName());

        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButtonType = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, cancelButtonType);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField nameField = new TextField(habit.getName());
        ComboBox<String> freqField = new ComboBox<>();
        freqField.getItems().addAll("Daily", "Weekly", "Monthly");
        freqField.setValue(habit.getFrequency());

        grid.add(new Label("Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Frequency:"), 0, 1);
        grid.add(freqField, 1, 1);
        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                habit.setName(nameField.getText());
                habit.setFrequency(freqField.getValue());
                return habit;
            }
            return null;
        });

        Optional<Habit> result = dialog.showAndWait();
        result.ifPresent(updatedHabit -> {
            habitDao.updateHabit(updatedHabit);
            loadData();
        });
    }

    private void showPastDateDialog(Habit habit) {
        if (habit == null) return;
        Dialog<LocalDate> dialog = new Dialog<>();
        dialog.setTitle("Past Check-in");
        dialog.setHeaderText("Add a missed check-in for: " + habit.getName());

        ButtonType confirmButtonType = new ButtonType("Confirm", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(confirmButtonType, ButtonType.CANCEL);

        DatePicker datePicker = new DatePicker(LocalDate.now().minusDays(1));
        VBox content = new VBox(10, new Label("Select Date:"), datePicker);
        content.setPadding(new Insets(20));
        dialog.getDialogPane().setContent(content);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == confirmButtonType) return datePicker.getValue();
            return null;
        });

        Optional<LocalDate> result = dialog.showAndWait();
        result.ifPresent(date -> {
            // 【修复点】：这里传入整个 habit 对象，而不是 habit.getId()
            int code = habitDao.checkIn(habit, date.toString());
            handleCheckInResult(code, habit.getName());
        });
    }

    private void deleteHabit(Habit habit) {
        if (habit == null) return;
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Delete Habit");
        alert.setHeaderText("Delete " + habit.getName() + "?");
        alert.setContentText("Check-in history will be lost.");
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            habitDao.deleteHabit(habit.getId());
            loadData();
        }
    }

    private void clearAllData() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Clear All Data");
        alert.setHeaderText("Delete EVERYTHING?");
        alert.setContentText("This will remove ALL habits and ALL check-in history. This cannot be undone.");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            habitDao.deleteAllData();
            loadData();
            showAlert(Alert.AlertType.INFORMATION, "Reset Complete", "All data has been cleared.");
        }
    }

    private void loadData() {
        habitData.clear();
        habitData.addAll(habitDao.getAllHabits());
    }

    private void generateDemoData() {
        String[] sampleHabits = {
                "Morning Jog", "Read 30 Mins", "Code Java", "Drink 2L Water", "Meditation",
                "Grocery Shopping", "Laundry", "Call Parents",
                "Pay Bills", "Check Savings"
        };
        String[] freqs = {
                "Daily", "Daily", "Daily", "Daily", "Daily",
                "Weekly", "Weekly", "Weekly",
                "Monthly", "Monthly"
        };

        for (int i = 0; i < sampleHabits.length; i++) {
            habitDao.addHabit(sampleHabits[i], freqs[i]);
        }
        loadData();

        java.util.Random random = new java.util.Random();
        LocalDate today = LocalDate.now();

        for (Habit habit : habitData) {
            boolean isSample = false;
            for(String s : sampleHabits) if(s.equals(habit.getName())) isSample = true;

            if (isSample) {
                for (int day = 0; day < 60; day++) {
                    double chance = random.nextDouble();
                    boolean shouldCheckIn = false;
                    if (habit.getFrequency().equals("Daily") && chance > 0.3) shouldCheckIn = true;
                    else if (habit.getFrequency().equals("Weekly") && chance > 0.85) shouldCheckIn = true;
                    else if (habit.getFrequency().equals("Monthly") && chance > 0.97) shouldCheckIn = true;

                    if (shouldCheckIn) {
                        // 【修复点】：这里传入整个 habit 对象，而不是 habit.getId()
                        habitDao.checkIn(habit, today.minusDays(day).toString());
                    }
                }
            }
        }
        showAlert(Alert.AlertType.INFORMATION, "Data Loaded", "Added 10 sample habits with 2 months of history!");
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    @Override
    public void stop() throws Exception {
        super.stop();
        reminderService.stop();
    }

    public static void main(String[] args) {
        java.util.Locale.setDefault(java.util.Locale.ENGLISH);
        launch(args);
    }
}