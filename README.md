# Habit Tracker Pro

### 1. Project Overview
**Habit Tracker Pro** is a comprehensive desktop application designed to help users build and maintain positive habits through consistent tracking and data visualization.

The application allows users to create habits with specific frequencies (Daily, Weekly, Monthly), record their progress via check-ins, and analyze their consistency through interactive charts. It combines personal productivity principles with robust software engineering practices using **Java 8** and **JavaFX**.

---

### 2. Key Features
*   **Habit Management (CRUD):** Users can Create, Read, Update (Edit name/frequency), and Delete habits.
*   **Smart Check-in System:**
    *   Supports **Daily**, **Weekly**, and **Monthly** frequency logic.
    *   Prevents duplicate check-ins based on the habit's frequency rules.
    *   **"Past Date Check-in"**: Allows users to backdate check-ins for missed days via a calendar interface.
*   **Data Visualization:**
    *   **Dashboard:** Displays Weekly (7 days), Monthly (30 days), and All-Time statistics.
    *   **Interactive Charts:** Includes Bar Charts (check-in counts) and Pie Charts (distribution of focus).
*   **Sample Data Generator:** A built-in feature to instantly populate the database with realistic sample habits and 2 months of historical data for testing purposes.
*   **Background Reminders:** A background service that runs independently of the UI thread to prompt users to complete tasks.
*   **Data Persistence:** All data is stored locally in an SQLite database, ensuring data is not lost when the application closes.

---

### 3. Advanced Java Concepts Implemented
This project demonstrates proficiency in several advanced Java concepts:

*   **JavaFX GUI:** Complex UI layout using `TabPane`, `BorderPane`, `TableView`, and custom CSS styling. Implementation of Event Handling for buttons and context menus.
*   **JDBC & SQLite Database:**
    *   Uses **JDBC** for database connectivity.
    *   Implements the **DAO (Data Access Object)** design pattern to separate data logic from business logic.
    *   **Atomic Transactions:** Uses `conn.setAutoCommit(false)` and `commit()`/`rollback()` to ensure data consistency during deletion and insertion operations.
*   **Multithreading & Concurrency:**
    *   Uses `ScheduledExecutorService` to run a background reminder thread without blocking the main JavaFX UI thread.
*   **Java Collections & Streams:** Utilizes `ArrayList`, `HashMap`, and Java 8 Stream API for data processing.

---

### 4. Project Structure (How it Works)
The project follows a modular **MVC-like architecture**:

*   `com.habit.model`: Defines the `Habit` entity class.
*   `com.habit.dao`: Handles all SQL operations (`HabitDao`), implementing CRUD and transaction logic.
*   `com.habit.service`: Contains background services like `ReminderService` for threading logic.
*   `com.habit.ui`: Manages the JavaFX interface (`MainApp`), charts, and user interactions.
*   `com.habit.util`: Manages database connection and initialization (`DatabaseHelper`).

---

### 5. How to Run the Project

#### Prerequisites
*   **JDK 1.8 (Java 8)** is required.
*   **Maven** (for dependency management).
*   **IntelliJ IDEA** (Recommended IDE).

#### Steps to Run
1.  **Open the Project:**
    *   Open the folder in IntelliJ IDEA.
    *   Ensure the project is recognized as a Maven project (look for `pom.xml`).

2.  **Load Dependencies:**
    *   If the code shows errors initially, look for the Maven tab on the right side of IDEA and click the **"Reload All Maven Projects"** (refresh icon) button. This will download the `sqlite-jdbc` driver.

3.  **Run the Application:**
    *   Navigate to `src/main/java/com/habit/ui/MainApp.java`.
    *   Right-click inside the file and select **Run 'MainApp.main()'**.

#### Quick Evaluation Guide (For Grader)
Once the application launches:
1.  **Populate Data:** Click the **"Load Sample Data"** button at the top left. This will instantly generate 10 habits with mixed frequencies and 60 days of check-in history. You can also add a habit manually by input the habit name at the bar and choose habit type then add habit.
2.  **View Analytics:** Click the **"Analytics & Charts"** tab to view the visualized data (Bar & Pie charts).
3.  **Interact:**
    *   Select a habit and click **"Check In Today"**.
    *   **Right-click** any habit in the list to access **Edit**, **Delete**, or **Check-in Past Date** options.
    *   Click **"Clear All Data"** (top right) to reset the database.

---

### 6. Troubleshooting
*   **"No suitable driver found"**: Ensure Maven has finished downloading the dependencies. Re-import the Maven project.
*   **Database Lock**: If the database file (`habit_tracker.db`) is open in another program (like DB Browser for SQLite), the app might fail to write data. Close other DB viewers before running.

---