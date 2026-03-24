import java.util.Scanner;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import model.SearchCriteria;
import model.Task;
import model.TaskStatus;
import Service.CsvExporter;
import Service.CsvImporter;
import Service.TaskManager;
import util.DateUtil;
 
public class Main {

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        TaskManager taskManager = new TaskManager();
        CsvImporter csvImporter = new CsvImporter(taskManager);
        CsvExporter csvExporter = new CsvExporter(taskManager);

        boolean isRunning = true;

        while (isRunning) {
            printMenu();
            System.out.print(" Please, enter your choice: ");
            String choice = scanner.nextLine();

            switch (choice) {
                case "1":
                    importTasks(scanner, csvImporter);
                    break;
                case "2":
                    searchTasks(scanner, taskManager);
                    break;
                case "3":
                    viewAllTasks(taskManager);
                    break;
                case "4":
                    exportTasks(scanner, csvExporter);
                    break;
                case "5":
                    isRunning = false;
                    System.out.println("Exiting.");
                    break;
                default:
                    System.out.println("Choice is invalid. Please try again.");
            }

            System.out.println();
        }

        scanner.close();
    }

    //displaying menu
    private static void printMenu() {
        System.out.println("------------- Task Management PoC -------------  ");
        System.out.println("1. Import tasks from CSV");
        System.out.println("2. Search tasks");
        System.out.println("3. View all tasks");
        System.out.println("4. Export tasks to CSV");
        System.out.println("5. Exit program");
        System.out.println("--------------------------------------------------- ");
    }

    //import tasks from csv
    private static void importTasks(Scanner scanner, CsvImporter csvImporter) {
        System.out.print("Enter CSV file path to import: ");
        String filePath = scanner.nextLine();

        try {
            csvImporter.importFromCSV(filePath);
            System.out.println("Tasks imported successfully.");
        } catch (IOException e) {
            System.out.println("Error importing file: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("Import failed: " + e.getMessage());
        }
    }

    //search tasks using optional criteria
    private static void searchTasks(Scanner scanner, TaskManager taskManager) {
        SearchCriteria searchCriteria = new SearchCriteria();

        System.out.print("Enter title keyword (or blank): ");
        String titleKeyword = scanner.nextLine().trim();
        if (!titleKeyword.isEmpty()) {
            searchCriteria.setTitleKeyword(titleKeyword);
        }

        System.out.print("Enter status (OPEN, COMPLETED, CANCELLED) or leave blank: ");
        String statusText = scanner.nextLine().trim();
        if (!statusText.isEmpty()) {
            try {
                searchCriteria.setStatus(TaskStatus.valueOf(statusText.toUpperCase()));
            } catch (IllegalArgumentException e) {
                System.out.println("Entered status is invalid. Ignoring status filter.");
            }
        }

        System.out.print("Enter start date (yyyy-MM-dd) or leave it blank: ");
        String startDateText = scanner.nextLine().trim();
        if (!startDateText.isEmpty()) {
            try {
                LocalDate startDate = DateUtil.parseDate(startDateText);
                searchCriteria.setStartDate(startDate);
            } catch (IllegalArgumentException e) {
                System.out.println("Start date is invalid. Ignoring start date filter.");
            }
        }

        System.out.print("Enter end date (yyyy-MM-dd) or leave blank: ");
        String endDateText = scanner.nextLine().trim();
        if (!endDateText.isEmpty()) {
            try {
                LocalDate endDate = DateUtil.parseDate(endDateText);
                searchCriteria.setEndDate(endDate);
            } catch (IllegalArgumentException e) {
                System.out.println("End date is invalid. Ignoring end date filter.");
            }
        }
        System.out.print("Enter day of week (MONDAY, TUESDAY, etc.) or leave blank: ");
        String dayOfWeek = scanner.nextLine().trim();
        if (!dayOfWeek.isEmpty()) {
            searchCriteria.setDayOfWeek(dayOfWeek);
        }
        List<Task> matchingTasks = taskManager.searchTasks(searchCriteria);
        if (matchingTasks.isEmpty()) {
            System.out.println("No matching tasks are found.");
        } else {
            System.out.println("Matching tasks are:");
            for (Task currentTask : matchingTasks) {
                System.out.println(currentTask);
            }
        }
    }

    //view all tasks in the system
    private static void viewAllTasks(TaskManager taskManager) {
        List<Task> allTasks = taskManager.getAllTasks();

        if (allTasks.isEmpty()) {
            System.out.println("No tasks are available.");
        } else {
            System.out.println("All tasks:");
            for (Task currentTask : allTasks) {
                System.out.println(currentTask);
            }
        }
    }
    //exporting tasks to csv
    private static void exportTasks(Scanner scanner, CsvExporter csvExporter) {
        System.out.print("Enter CSV file path to export: ");
        String filePath = scanner.nextLine();

        try {
            csvExporter.exportToCSV(filePath);
            System.out.println("Tasks exported successfully.");
        } catch (IOException e) {
            System.out.println("Error exporting file: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("Export failed: " + e.getMessage());
        }
    }
}