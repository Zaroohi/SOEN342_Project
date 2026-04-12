package ui;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Scanner;
import database.DatabaseConnection;
import database.AppDAO;
import model.ActivityEntry;
import model.Collaborator;
import model.CollaboratorCategory;
import model.CollaboratorSubtask;
import model.Project;
import model.RecurrenceFrequency;
import model.RecurrencePattern;
import model.SearchCriteria;
import model.SubTask;
import model.Task;
import model.TaskStatus;
import service.Collaborators;
import service.CsvExporter;
import service.CsvImporter;
import service.IcsCalendarExporter;
import service.Projects;
import service.Tasks;
import util.DateUtil;

//CLI menu; Tasks saves to SQLite after changes; exit/shutdown calls saveToDisk()
public class Console {

    private final Scanner scanner;
    private final DatabaseConnection databaseConnection;
    private final Projects projects;
    private final Collaborators collaborators;
    private final Tasks tasks;
    private final CsvImporter csvImporter;
    private final CsvExporter csvExporter;
    private final IcsCalendarExporter icsCalendarExporter;
    private List<Task> lastSearchResults;

//---------------------------------CONSTRUCTORS---------------------------------

    public Console() {
        this(new DatabaseConnection());
    }

    Console(DatabaseConnection databaseConnection) {
        this.scanner = new Scanner(System.in);
        this.databaseConnection = databaseConnection;
        this.projects = new Projects();
        this.collaborators = new Collaborators(this.projects);
        this.tasks = new Tasks(this.projects, this.collaborators, this.databaseConnection);
        try (Connection c = this.databaseConnection.getConnection()) {
            new AppDAO(c).loadAll(this.projects, this.tasks);
        } catch (SQLException | IOException e) {
            System.err.println("Could not load database: " + e.getMessage());
        }
        this.csvImporter = new CsvImporter(this.tasks, this.projects, this.collaborators);
        this.csvExporter = new CsvExporter(this.tasks);
        this.icsCalendarExporter = new IcsCalendarExporter();
        this.lastSearchResults = new ArrayList<>();
        Runtime.getRuntime().addShutdownHook(new Thread(this::persistQuietly));
    }

//---------------------------------PERSIST TO SQLITE---------------------------------

    private void persistQuietly() {
        try {
            this.tasks.saveToDisk();
        } catch (Exception ignored) {
            // best effort on shutdown
        }
    }

    private void persist() throws IOException {
        try {
            this.tasks.saveToDisk();
        } catch (RuntimeException e) {
            throw new IOException(e.getMessage(), e);
        }
    }

//---------------------------------MENU LOOP---------------------------------

    public void display(String message) {
        System.out.println(message);
    }

    public void run() {
        boolean running = true;
        while (running) {
            printMenu();
            System.out.print(" Please, enter your choice: ");
            String choice = this.scanner.nextLine();
            running = handleMenuChoice(choice);
            System.out.println();
        }
        this.scanner.close();
    }

    private boolean handleMenuChoice(String choice) {
        switch (choice) {
            case "1":
                createTask();
                return true;
            case "2":
                createRecurringTask();
                return true;
            case "3":
                updateTask();
                return true;
            case "4":
                changeTaskStatus("complete");
                return true;
            case "5":
                changeTaskStatus("cancel");
                return true;
            case "6":
                changeTaskStatus("reopen");
                return true;
            case "7":
                viewAllTasks();
                return true;
            case "8":
                searchTasks();
                return true;
            case "9":
                viewTaskHistory();
                return true;
            case "10":
                addGeneralSubtask();
                return true;
            case "11":
                addCollaboratorToTask();
                return true;
            case "12":
                changeSubtaskStatus("complete");
                return true;
            case "13":
                changeSubtaskStatus("cancel");
                return true;
            case "14":
                changeSubtaskStatus("reopen");
                return true;
            case "15":
                createProject();
                return true;
            case "16":
                viewAllProjects();
                return true;
            case "17":
                assignTaskToProject();
                return true;
            case "18":
                addCollaboratorToProject();
                return true;
            case "19":
                importTasks();
                return true;
            case "20":
                exportTasks();
                return true;
            case "21":
                exportSingleTaskToIcal();
                return true;
            case "22":
                exportProjectTasksToIcal();
                return true;
            case "23":
                exportFilteredTasksToIcal();
                return true;
            case "24": 
            	listOverloadedCollaborators();
            	return true;
            case "25":
                try {
                    persist();
                } catch (IOException e) {
                    display("Could not save database: " + e.getMessage());
                }
                display("Exiting.");
                return false;
            default:
                display("Choice is invalid. Please try again.");
                return true;
        }
    }

    private void printMenu() {
        display("");
        display("================ Personal Task Manager ================");
        display("");
        display("TASKS");
        display("  1) Create task");
        display("  2) Create recurring task");
        display("  3) Update task");
        display("  4) Complete task");
        display("  5) Cancel task");
        display("  6) Reopen task");
        display("  7) View all tasks");
        display("  8) Search tasks");
        display("  9) View task activity history");
        display("");
        display("SUBTASKS");
        display(" 10) Add general subtask");
        display(" 11) Add collaborator subtask");
        display(" 12) Complete subtask");
        display(" 13) Cancel subtask");
        display(" 14) Reopen subtask");
        display("");
        display("PROJECTS");
        display(" 15) Create project");
        display(" 16) View all projects");
        display(" 17) Assign task to project");
        display("");
        display("COLLABORATORS");
        display(" 18) Add collaborator to project");
        display("");
        display("IMPORT / EXPORT");
        display(" 19) Import tasks from CSV");
        display(" 20) Export tasks to CSV");
        display(" 21) Export one task to .ics file");
        display(" 22) Export project tasks to .ics file");
        display(" 23) Export filtered tasks to .ics file");
        display(" 24) List overloaded collaborators");
        display("");
        display("SYSTEM");
        display(" 25) Exit");
        display("=======================================================");
    }

//---------------------------------MENU ACTIONS---------------------------------

    private void importTasks() {
        System.out.print("Enter CSV file path to import: ");
        String filePath = this.scanner.nextLine();
        try {
            this.csvImporter.importFromCsv(filePath);
            display("Tasks imported successfully.");
        } catch (IOException e) {
            display("Error importing file: " + e.getMessage());
        } catch (RuntimeException e) {
            display("Import failed: " + e.getMessage());
        } catch (Exception e) {
            display("Import failed: " + e.getMessage());
        }
    }

    private void createTask() {
        try {
            Task t = this.tasks.createSingleTask(
                    prompt("Title"),
                    prompt("Description"),
                    prompt("Priority"),
                    readOptionalDate("Due date (yyyy-MM-dd, blank for none)"),
                    readOptionalProject("Project name (blank for none)"),
                    readTagList(),
                    null,
                    null,
                    null,
                    null);
            display("Task created with id: " + t.getId());
        } catch (Exception e) {
            display("Create task failed: " + e.getMessage());
        }
    }

    private void createRecurringTask() {
        try {
            String title = prompt("Title");
            String description = prompt("Description");
            String priority = prompt("Priority");
            Project project = readOptionalProject("Project name (blank for none)");
            List<String> tags = readTagList();
            RecurrencePattern pattern = readRecurrencePattern();
            List<Task> created = this.tasks.createTaskSeries(
                    title, description, priority, null, project, tags, null, null, null, pattern);
            display("Recurring series created (" + created.size() + " tasks).");
        } catch (Exception e) {
            display("Create recurring task failed: " + e.getMessage());
        }
    }

    private void updateTask() {
        Task task = findTaskByPrompt();
        if (task == null) {
            return;
        }
        try {
            String title = prompt("New title (blank keep current)");
            if (!title.trim().isEmpty()) {
                task.setTitle(title);
            }
            String description = prompt("New description (blank keep current)");
            if (!description.trim().isEmpty()) {
                task.setDescription(description);
            }
            String priority = prompt("New priority (blank keep current)");
            if (!priority.trim().isEmpty()) {
                task.setPriorityLevel(priority);
            }

            String dueText = prompt("New due date yyyy-MM-dd / 'none' to clear / blank keep current");
            if (!dueText.trim().isEmpty()) {
                if ("none".equalsIgnoreCase(dueText.trim())) {
                    task.setDueDate(null);
                } else {
                    task.setDueDate(DateUtil.parseDate(dueText.trim()));
                }
            }

            String statusText = prompt("New status OPEN|COMPLETED|CANCELLED (blank keep current)");
            if (!statusText.trim().isEmpty()) {
                TaskStatus status = TaskStatus.valueOf(statusText.trim().toUpperCase());
                if (status == TaskStatus.COMPLETED) {
                    this.tasks.completeTask(task);
                } else if (status == TaskStatus.CANCELLED) {
                    this.tasks.cancelTask(task);
                } else {
                    this.tasks.reopenTask(task);
                }
            }

            String projectName = prompt("Project name to assign (blank keep, 'none' remove)");
            if (!projectName.trim().isEmpty()) {
                Project old = task.getProject();
                if (old != null) {
                    old.removeTask(task);
                }
                if (!"none".equalsIgnoreCase(projectName.trim())) {
                    Project p = this.projects.findByName(projectName);
                    if (p == null) {
                        throw new IllegalArgumentException("Project does not exist.");
                    }
                    p.addTask(task);
                }
            }

            String tagsText = prompt("Tags comma-separated (blank keep current, 'clear' removes all)");
            if (!tagsText.trim().isEmpty()) {
                if ("clear".equalsIgnoreCase(tagsText.trim())) {
                    for (model.Tag tag : new ArrayList<>(task.getTags())) {
                        task.removeTag(tag);
                    }
                } else {
                    for (model.Tag tag : new ArrayList<>(task.getTags())) {
                        task.removeTag(tag);
                    }
                    for (String kw : splitCsv(tagsText)) {
                        if (!kw.isEmpty()) {
                            task.addTagKeyword(kw);
                        }
                    }
                }
            }

            this.tasks.save(task);
            display("Task updated.");
        } catch (Exception e) {
            display("Update failed: " + e.getMessage());
        }
    }

    private void changeTaskStatus(String action) {
        Task task = findTaskByPrompt();
        if (task == null) {
            return;
        }
        try {
            if ("complete".equals(action)) {
                this.tasks.completeTask(task);
            } else if ("cancel".equals(action)) {
                this.tasks.cancelTask(task);
            } else {
                this.tasks.reopenTask(task);
            }
            display("Task updated: " + task.getStatus());
        } catch (Exception e) {
            display("Task status update failed: " + e.getMessage());
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
        System.out.print("Enter priority (exact match) or leave blank: ");
        String priority = this.scanner.nextLine().trim();
        if (!priority.isEmpty()) {
            criteria.setPriority(priority);
        }
        System.out.print("Enter project name or leave blank: ");
        String projectName = this.scanner.nextLine().trim();
        if (!projectName.isEmpty()) {
            criteria.setProjectName(projectName);
        }
        System.out.print("Enter tag keyword or leave blank: ");
        String tagKeyword = this.scanner.nextLine().trim();
        if (!tagKeyword.isEmpty()) {
            criteria.setTagKeyword(tagKeyword);
        }
        List<Task> matchingTasks = this.tasks.search(criteria);
        this.lastSearchResults = new ArrayList<>(matchingTasks);
        if (matchingTasks.isEmpty()) {
            display("No matching tasks are found.");
        } else {
            display("Matching tasks are:");
            for (Task t : matchingTasks) {
                display(t.toString());
            }
            String export = prompt("Export these search results now? (y/N)");
            if ("y".equalsIgnoreCase(export.trim())) {
                exportTasks(true);
            }
        }
    }

    private void viewTaskHistory() {
        Task task = findTaskByPrompt();
        if (task == null) {
            return;
        }
        List<ActivityEntry> history = task.getActivityHistory();
        if (history.isEmpty()) {
            display("No activity history.");
            return;
        }
        display("Activity history for task " + task.getId() + ":");
        for (ActivityEntry entry : history) {
            display(entry.getAt() + " | " + entry.getType() + " | " + entry.getDescription());
        }
    }

    private void addGeneralSubtask() {
        Task task = findTaskByPrompt();
        if (task == null) {
            return;
        }
        try {
            String title = prompt("Subtask title");
            task.addGeneralSubtask(title);
            this.tasks.save(task);
            display("General subtask added.");
        } catch (Exception e) {
            display("Add subtask failed: " + e.getMessage());
        }
    }

    private void addCollaboratorToTask() {
        Task task = findTaskByPrompt();
        if (task == null) {
            return;
        }
        if (task.getProject() == null) {
            display("Task has no project. Assign it to a project first.");
            return;
        }
        try {
            String name = prompt("Collaborator name");
            Collaborator collaborator = this.collaborators.findByName(task.getProject(), name);
            if (collaborator == null) {
                display("Collaborator not found on project.");
                return;
            }
            this.collaborators.validateForOpenAssignment(collaborator, task.getProject());
            String subtaskTitle = prompt("Collaborator subtask title");
            CollaboratorSubtask cs = new CollaboratorSubtask(subtaskTitle, collaborator);
            task.addSubtask(cs);
            collaborator.assignedTask(cs);
            this.tasks.save(task);
            display("Collaborator subtask added.");
        } catch (Exception e) {
            display("Add collaborator subtask failed: " + e.getMessage());
        }
    }

    private void changeSubtaskStatus(String action) {
        Task task = findTaskByPrompt();
        if (task == null) {
            return;
        }
        if (task.getSubtasks().isEmpty()) {
            display("Task has no subtasks.");
            return;
        }
        for (int i = 0; i < task.getSubtasks().size(); i++) {
            SubTask s = task.getSubtasks().get(i);
            display((i + 1) + ". " + s.toString());
        }
        try {
            int idx = Integer.parseInt(prompt("Choose subtask number")) - 1;
            if (idx < 0 || idx >= task.getSubtasks().size()) {
                display("Invalid subtask number.");
                return;
            }
            SubTask subtask = task.getSubtasks().get(idx);
            if ("complete".equals(action)) {
                this.tasks.completeSubtask(subtask);
            } else if ("cancel".equals(action)) {
                this.tasks.cancelSubtask(subtask);
            } else {
                this.tasks.reopenSubtask(subtask);
            }
            display("Subtask updated: " + subtask.getStatus());
        } catch (Exception e) {
            display("Subtask status update failed: " + e.getMessage());
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

    private void createProject() {
        try {
            String name = prompt("Project name");
            String description = prompt("Project description");
            this.tasks.createProject(name, description);
            display("Project created.");
        } catch (Exception e) {
            display("Create project failed: " + e.getMessage());
        }
    }

    private void viewAllProjects() {
        List<Project> all = this.projects.getAllProjects();
        if (all.isEmpty()) {
            display("No projects are available.");
            return;
        }
        for (Project p : all) {
            display(p.toString());
        }
    }

    private void assignTaskToProject() {
        Task task = findTaskByPrompt();
        if (task == null) {
            return;
        }
        try {
            String projectName = prompt("Project name");
            Project p = this.projects.findByName(projectName);
            if (p == null) {
                display("Project not found.");
                return;
            }
            Project old = task.getProject();
            if (old != null) {
                old.removeTask(task);
            }
            p.addTask(task);
            this.tasks.save(task);
            display("Task assigned to project.");
        } catch (Exception e) {
            display("Assign failed: " + e.getMessage());
        }
    }

    private void addCollaboratorToProject() {
        try {
            String projectName = prompt("Project name");
            Project project = this.projects.findByName(projectName);
            if (project == null) {
                display("Project not found.");
                return;
            }
            String name = prompt("Collaborator name");
            String categoryText = prompt("Category SENIOR|INTERMEDIATE|JUNIOR");
            CollaboratorCategory category = CollaboratorCategory.valueOf(categoryText.trim().toUpperCase());
            this.tasks.addCollaboratorToProject(project, name, category);
            display("Collaborator added to project.");
        } catch (Exception e) {
            display("Add collaborator failed: " + e.getMessage());
        }
    }

    private void exportTasks() {
        exportTasks(false);
    }

    private void exportTasks(boolean searchOnly) {
        System.out.print("Enter CSV file path to export: ");
        String filePath = this.scanner.nextLine();
        try {
            if (searchOnly) {
                this.csvExporter.exportToCsv(filePath, this.lastSearchResults);
                display("Search results exported successfully.");
                return;
            }
            String mode = prompt("Export all tasks or last search results? (all/search)");
            if ("search".equalsIgnoreCase(mode.trim())) {
                this.csvExporter.exportToCsv(filePath, this.lastSearchResults);
                display("Search results exported successfully.");
            } else {
                this.csvExporter.exportToCsv(filePath);
                display("Tasks exported successfully.");
            }
        } catch (IOException e) {
            display("Error exporting file: " + e.getMessage());
        } catch (Exception e) {
            display("Export failed: " + e.getMessage());
        }
    }

    private void exportSingleTaskToIcal() {
        Task task = findTaskByPrompt();
        if (task == null) {
            return;
        }
        String rawPath = prompt("Output path (.ics added if missing, e.g. mytasks or out/mytasks.ics)");
        try {
            String path = IcsCalendarExporter.ensureIcsFilePath(rawPath);
            int n = this.icsCalendarExporter.exportToIcs(path, Collections.singletonList(task));
            display("Wrote " + n + " event(s) to " + path + " (tasks without a due date are skipped).");
        } catch (IOException e) {
            display("iCal export failed: " + e.getMessage());
        }
    }

    private void exportProjectTasksToIcal() {
        String projectName = prompt("Project name");
        Project project = this.projects.findByName(projectName);
        if (project == null) {
            display("Project not found.");
            return;
        }
        String rawPath = prompt("Output path (.ics added if missing, e.g. mytasks or out/mytasks.ics)");
        try {
            String path = IcsCalendarExporter.ensureIcsFilePath(rawPath);
            int n = this.icsCalendarExporter.exportToIcs(path, new ArrayList<>(project.getTasks()));
            display("Wrote " + n + " event(s) to " + path + " (tasks without a due date are skipped).");
        } catch (IOException e) {
            display("iCal export failed: " + e.getMessage());
        }
    }

    private void exportFilteredTasksToIcal() {
        if (this.lastSearchResults.isEmpty()) {
            display("No filtered list available. Use Search tasks (8) first.");
            return;
        }
        String rawPath = prompt("Output path (.ics added if missing, e.g. mytasks or out/mytasks.ics)");
        try {
            String path = IcsCalendarExporter.ensureIcsFilePath(rawPath);
            int n = this.icsCalendarExporter.exportToIcs(path, this.lastSearchResults);
            display("Wrote " + n + " event(s) to " + path + " (tasks without a due date are skipped).");
        } catch (IOException e) {
            display("iCal export failed: " + e.getMessage());
        }
    }

    private Task findTaskByPrompt() {
        try {
            int id = Integer.parseInt(prompt("Task id"));
            Task task = this.tasks.findById(id);
            if (task == null) {
                display("Task not found.");
            }
            return task;
        } catch (NumberFormatException e) {
            display("Task id must be a number.");
            return null;
        }
    }

    private String prompt(String label) {
        System.out.print(label + ": ");
        return this.scanner.nextLine();
    }

    private LocalDate readOptionalDate(String label) {
        String txt = prompt(label).trim();
        if (txt.isEmpty()) {
            return null;
        }
        return DateUtil.parseDate(txt);
    }

    private Project readOptionalProject(String label) {
        String name = prompt(label).trim();
        if (name.isEmpty()) {
            return null;
        }
        Project existing = this.projects.findByName(name);
        if (existing != null) {
            return existing;
        }
        String desc = prompt("Project doesn't exist. Description for new project");
        Project created = this.tasks.createProject(name, desc);
        return created;
    }

    private List<String> readTagList() {
        String tagsRaw = prompt("Tags comma-separated (blank for none)");
        return splitCsv(tagsRaw);
    }

    private List<String> splitCsv(String text) {
        List<String> out = new ArrayList<>();
        if (text == null || text.trim().isEmpty()) {
            return out;
        }
        String[] parts = text.split(",");
        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                out.add(trimmed);
            }
        }
        return out;
    }
    private void listOverloadedCollaborators() {
        System.out.println("DEBUG: entered option 24");

        System.out.println("\nOverloaded collaborators:");

        boolean found = false;
        List<Collaborator> printed = new ArrayList<>();

        for (Project project : projects.getAllProjects()) {
            System.out.println("DEBUG project: " + project.getName());

            for (Collaborator collaborator : project.getCollaborators()) {
                System.out.println("DEBUG collaborator: " + collaborator.getName()
                    + " active=" + collaborator.getActiveOpenTasks()
                    + " limit=" + collaborator.getOpenTasksLimit());

                if (collaborator.getActiveOpenTasks() >= collaborator.getOpenTasksLimit()
                        && !printed.contains(collaborator)) {

                    found = true;
                    printed.add(collaborator);

                    System.out.println(
                        "- " + collaborator.getName()
                        + " | Project: " + project.getName()
                        + " | Category: " + collaborator.getCategory()
                        + " | Open tasks: " + collaborator.getActiveOpenTasks()
                        + "/" + collaborator.getOpenTasksLimit()
                    );
                }
            }
        }

        if (!found) {
            System.out.println("No overloaded collaborators.");
        }
    }

    private RecurrencePattern readRecurrencePattern() {
        String freqText = prompt("Frequency DAILY|WEEKLY|MONTHLY");
        RecurrenceFrequency frequency = RecurrenceFrequency.valueOf(freqText.trim().toUpperCase());
        int interval = Integer.parseInt(prompt("Interval (>=1)"));
        LocalDate start = DateUtil.parseDate(prompt("Start date yyyy-MM-dd"));
        LocalDate end = DateUtil.parseDate(prompt("End date yyyy-MM-dd"));
        RecurrencePattern pattern = new RecurrencePattern(frequency, interval, start, end);
        if (frequency == RecurrenceFrequency.WEEKLY) {
            String days = prompt("Days (MONDAY,TUESDAY...) blank=use start-date weekday cadence");
            if (!days.trim().isEmpty()) {
                EnumSet<DayOfWeek> set = EnumSet.noneOf(DayOfWeek.class);
                for (String token : days.split(",")) {
                    set.add(DayOfWeek.valueOf(token.trim().toUpperCase()));
                }
                pattern.setWeekDays(set);
            }
        }
        if (frequency == RecurrenceFrequency.MONTHLY) {
            String dom = prompt("Day of month 1..31 (blank uses start date's day)");
            if (!dom.trim().isEmpty()) {
                pattern.setDayOfMonth(Integer.parseInt(dom.trim()));
            }
        }
        return pattern;
    }
}
