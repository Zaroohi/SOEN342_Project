package ui;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Scanner;
import model.SearchCriteria;
import model.Task;
import model.TaskStatus;
import Service.CsvExport;
import Service.CsvImport;
import Service.Projects;
import Service.Tasks;
import util.DateUtil;

public class Console {

    private final Scanner scanner;
    private final Tasks tasks;
    private final CsvImport csvImport;
    private final CsvExport csvExport;

    public Console() {
        this.scanner = new Scanner(System.in);
        Projects projects = new Projects();
        this.tasks = new Tasks(projects);
        this.csvImport = new CsvImport(this.tasks, projects);
        this.csvExport = new CsvExport(this.tasks);
    }

    public void display(String message) {
        System.out.println(message);
    }

    public void run() {
        boolean running = true;
        while (running) {
            printMenu();
            System.out.print(" Please, enter your choice: ");
            String choice = this.scanner.nextLine();
            switch (choice) {
                case "1":
                    importTasks();
                    break;
                case "2":
                    searchTasks();
                    break;
                case "3":
                    viewAllTasks();
                    break;
                case "4":
                    exportTasks();
                    break;
                case "5":
                    running = false;
                    display("Exiting.");
                    break;
                default:
                    display("Choice is invalid. Please try again.");
            }
            System.out.println();
        }
        this.scanner.close();
    }

    private void printMenu() {
        display("------------- Task Management PoC -------------  ");
        display("1. Import tasks from CSV");
        display("2. Search tasks");
        display("3. View all tasks");
        display("4. Export tasks to CSV");
        display("5. Exit program");
        display("--------------------------------------------------- ");
    }

    private void importTasks() {
        System.out.print("Enter CSV file path to import: ");
        String filePath = this.scanner.nextLine();
        try {
            this.csvImport.importFromCsv(filePath);
            display("Tasks imported successfully.");
        } catch (IOException e) {
            display("Error importing file: " + e.getMessage());
        } catch (Exception e) {
            display("Import failed: " + e.getMessage());
        }
    }

    private void searchTasks() {
        SearchCriteria criteria = new SearchCriteria();
        System.out.print("Enter title keyword (or blank): ");
        String titleKeyword = this.scanner.nextLine().trim();
        if (!titleKeyword.isEmpty()) {
            criteria.setTitleKeyword(titleKeyword);
        }
        System.out.print("Enter status (OPEN, COMPLETED, CANCELLED) or leave blank: ");
        String statusText = this.scanner.nextLine().trim();
        if (!statusText.isEmpty()) {
            try {
                criteria.setStatus(TaskStatus.valueOf(statusText.toUpperCase()));
            } catch (IllegalArgumentException e) {
                display("Entered status is invalid. Ignoring status filter.");
            }
        }
        System.out.print("Enter start date (yyyy-MM-dd) or leave it blank: ");
        String startDateText = this.scanner.nextLine().trim();
        if (!startDateText.isEmpty()) {
            try {
                LocalDate startDate = DateUtil.parseDate(startDateText);
                criteria.setStartDate(startDate);
            } catch (IllegalArgumentException e) {
                display("Start date is invalid. Ignoring start date filter.");
            }
        }
        System.out.print("Enter end date (yyyy-MM-dd) or leave blank: ");
        String endDateText = this.scanner.nextLine().trim();
        if (!endDateText.isEmpty()) {
            try {
                LocalDate endDate = DateUtil.parseDate(endDateText);
                criteria.setEndDate(endDate);
            } catch (IllegalArgumentException e) {
                display("End date is invalid. Ignoring end date filter.");
            }
        }
        System.out.print("Enter day of week (MONDAY, TUESDAY, etc.) or leave blank: ");
        String dayOfWeek = this.scanner.nextLine().trim();
        if (!dayOfWeek.isEmpty()) {
            criteria.setDayOfWeek(dayOfWeek);
        }
        List<Task> matchingTasks = this.tasks.search(criteria);
        if (matchingTasks.isEmpty()) {
            display("No matching tasks are found.");
        } else {
            display("Matching tasks are:");
            for (Task t : matchingTasks) {
                display(t.toString());
            }
        }
    }

    private void viewAllTasks() {
        List<Task> allTasks = this.tasks.getAllTasks();
        if (allTasks.isEmpty()) {
            display("No tasks are available.");
        } else {
            display("All tasks:");
            for (Task t : allTasks) {
                display(t.toString());
            }
        }
    }

    private void exportTasks() {
        System.out.print("Enter CSV file path to export: ");
        String filePath = this.scanner.nextLine();
        try {
            this.csvExport.exportToCsv(filePath);
            display("Tasks exported successfully.");
        } catch (IOException e) {
            display("Error exporting file: " + e.getMessage());
        } catch (Exception e) {
            display("Export failed: " + e.getMessage());
        }
    }
}
