package service;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import database.DatabaseConnection;
import database.TaskDAO;
import model.Collaborator;
import model.CollaboratorSubtask;
import model.Project;
import model.RecurrencePattern;
import model.SearchCriteria;
import model.SubTask;
import model.SubTaskStatus;
import model.Task;
import model.TaskStatus;

//in-memory tasks + rules; copies everything to SQLite after each change (see beginDefer/endDefer for multi-step only)
public class Tasks {

    private final List<Task> tasks;
    private final Projects projects;
    private final Collaborators collaborators;
    private final DatabaseConnection databaseConnection;
    private int deferSaveDepth;
    private int nextId;

//---------------------------------CONSTRUCTORS---------------------------------

    public Tasks(Projects projects, Collaborators collaborators, DatabaseConnection databaseConnection) {
        this.tasks = new ArrayList<>();
        this.projects = projects;
        this.collaborators = collaborators;
        this.databaseConnection = databaseConnection;
        this.deferSaveDepth = 0;
        this.nextId = 1;
    }

//---------------------------------DATABASE (one method + optional defer)---------------------------------

    //writes memory to SQLite (projects, collaborators, tasks, subtasks, tags)
    private void writeToDatabase() {
        try (Connection c = this.databaseConnection.getConnection()) {
            new TaskDAO(c).saveAll(this.projects, this);
        } catch (IOException | SQLException e) {
            throw new RuntimeException("Could not save to database: " + e.getMessage(), e);
        }
    }

    //call after mutating unless inside beginDefer/endDefer
    private void saveAfterChange() {
        if (this.deferSaveDepth == 0) {
            writeToDatabase();
        }
    }

    //same package: CSV import defers until the whole file is parsed
    void beginDefer() {
        this.deferSaveDepth++;
    }

    void endDefer() {
        this.deferSaveDepth--;
        if (this.deferSaveDepth < 0) {
            this.deferSaveDepth = 0;
        }
        if (this.deferSaveDepth == 0) {
            writeToDatabase();
        }
    }

    //exit menu / shutdown: always save even if someone forgot to balance defer
    public void saveToDisk() {
        this.deferSaveDepth = 0;
        writeToDatabase();
    }

//---------------------------------LOOKUP---------------------------------

    public Task findById(int id) {
        for (Task t : this.tasks) {
            if (t.getId() == id) {
                return t;
            }
        }
        return null;
    }

//---------------------------------TASK MUTATIONS---------------------------------

    public void save(Task task) {
        if (task == null) {
            throw new IllegalArgumentException("Task cannot be null.");
        }
        if (!this.tasks.contains(task)) {
            this.tasks.add(task);
        }
        Project p = task.getProject();
        if (p != null) {
            this.projects.save(p);
            if (!p.getTasks().contains(task)) {
                p.addTask(task);
            }
        }
        saveAfterChange();
    }

    public void deleteId(int id) {
        Task task = findById(id);
        if (task == null) {
            return;
        }
        releaseCollaboratorAssignments(task);
        Project p = task.getProject();
        if (p != null) {
            p.removeTask(task);
        }
        this.tasks.remove(task);
        saveAfterChange();
    }

//---------------------------------QUERIES (read-only, no DB write)---------------------------------

    public List<Task> getAllTasks() {
        return new ArrayList<>(this.tasks);
    }

    public List<Task> search(SearchCriteria criteria) {
        List<Task> matching = new ArrayList<>();
        if (criteria == null || criteria.isEmpty()) {
            for (Task t : this.tasks) {
                if (t.getStatus() == TaskStatus.OPEN) {
                    matching.add(t);
                }
            }
            matching.sort(Comparator.comparing(Task::getDueDate, Comparator.nullsLast(Comparator.naturalOrder())));
            return matching;
        }

        for (Task current : this.tasks) {
            if (matches(current, criteria)) {
                matching.add(current);
            }
        }
        matching.sort(Comparator.comparing(Task::getDueDate, Comparator.nullsLast(Comparator.naturalOrder())));
        return matching;
    }

    private boolean matches(Task currentTask, SearchCriteria criteria) {
        boolean matches = true;
        if (criteria.getTitleKeyword() != null && !criteria.getTitleKeyword().trim().isEmpty()) {
            String kw = criteria.getTitleKeyword().toLowerCase();
            String title = currentTask.getTitle() != null ? currentTask.getTitle().toLowerCase() : "";
            String desc = currentTask.getDescription() != null ? currentTask.getDescription().toLowerCase() : "";
            if (!title.contains(kw) && !desc.contains(kw)) {
                matches = false;
            }
        }
        if (matches && criteria.getStatus() != null) {
            if (currentTask.getStatus() != criteria.getStatus()) {
                matches = false;
            }
        }
        if (matches && criteria.getStartDate() != null) {
            if (currentTask.getDueDate() == null || currentTask.getDueDate().isBefore(criteria.getStartDate())) {
                matches = false;
            }
        }
        if (matches && criteria.getEndDate() != null) {
            if (currentTask.getDueDate() == null || currentTask.getDueDate().isAfter(criteria.getEndDate())) {
                matches = false;
            }
        }
        if (matches && criteria.getDayOfWeek() != null && !criteria.getDayOfWeek().trim().isEmpty()) {
            if (currentTask.getDueDate() == null) {
                matches = false;
            } else {
                String taskDay = currentTask.getDueDate().getDayOfWeek().toString();
                if (!taskDay.equalsIgnoreCase(criteria.getDayOfWeek().trim())) {
                    matches = false;
                }
            }
        }
        return matches;
    }

//---------------------------------CREATE TASKS---------------------------------

    public List<Task> createTaskSeries(String title, String description, String priorityLevel,
            LocalDate singleDueDate, Project project, List<String> tagKeywords,
            Collaborator collaborator, String collaboratorSubtaskTitle, String extraGeneralSubtaskTitle,
            RecurrencePattern pattern) {

        if (pattern != null) {
            List<LocalDate> dates = pattern.generateOccurrences();
            if (dates.isEmpty()) {
                throw new IllegalArgumentException("Recurrence pattern produced no occurrences.");
            }
            this.beginDefer();
            try {
                List<Task> created = new ArrayList<>();
                for (int i = 0; i < dates.size(); i++) {
                    LocalDate due = dates.get(i);
                    Collaborator c = (i == 0) ? collaborator : null;
                    String collabTitle = (i == 0) ? collaboratorSubtaskTitle : null;
                    String generalExtra = (i == 0) ? extraGeneralSubtaskTitle : null;
                    created.add(createSingleTask(title, description, priorityLevel, due, project, tagKeywords,
                            c, collabTitle, generalExtra, null));
                }
                return created;
            } finally {
                this.endDefer();
            }
        }
        return Collections.singletonList(createSingleTask(title, description, priorityLevel, singleDueDate,
                project, tagKeywords, collaborator, collaboratorSubtaskTitle, extraGeneralSubtaskTitle, null));
    }

    public Task createSingleTask(String title, String description, String priorityLevel,
            LocalDate dueDate, Project project, List<String> tagKeywords,
            Collaborator collaborator, String collaboratorSubtaskTitle, String extraGeneralSubtaskTitle,
            RecurrencePattern attachPattern) {

        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Task title can't be empty.");
        }
        assertTitleDueDateUnique(title, dueDate);

        this.collaborators.validateForOpenAssignment(collaborator, project);

        Task task = new Task(this.nextId++, title, description, priorityLevel, dueDate);
        if (attachPattern != null) {
            task.setRecurrencePattern(attachPattern);
        }

        if (tagKeywords != null) {
            for (String kw : tagKeywords) {
                if (kw != null && !kw.trim().isEmpty()) {
                    task.addTagKeyword(kw.trim());
                }
            }
        }

        if (project != null) {
            this.projects.save(project);
            project.addTask(task);
        }

        if (collaborator != null) {
            String stTitle = (collaboratorSubtaskTitle != null && !collaboratorSubtaskTitle.trim().isEmpty())
                    ? collaboratorSubtaskTitle.trim()
                    : "Collaborated work";
            CollaboratorSubtask cst = new CollaboratorSubtask(stTitle, collaborator);
            task.addSubtask(cst);
            collaborator.assignedTask(cst);
        }

        if (extraGeneralSubtaskTitle != null && !extraGeneralSubtaskTitle.trim().isEmpty()) {
            task.addGeneralSubtask(extraGeneralSubtaskTitle.trim());
        }

        this.tasks.add(task);
        saveAfterChange();
        return task;
    }

    private void assertTitleDueDateUnique(String title, LocalDate dueDate) {
        for (Task current : this.tasks) {
            if (current.getTitle().equalsIgnoreCase(title.trim())
                    && Objects.equals(current.getDueDate(), dueDate)) {
                throw new IllegalArgumentException("A task with the same title and due date already exists.");
            }
        }
    }

//---------------------------------TASK STATUS---------------------------------

    public void completeTask(Task task) {
        if (task == null) {
            throw new IllegalArgumentException("Task can't be null.");
        }
        this.beginDefer();
        try {
            task.complete();
            for (SubTask s : new ArrayList<>(task.getSubtasks())) {
                if (s instanceof CollaboratorSubtask && s.isOpen()) {
                    applySubtaskComplete(s);
                }
            }
        } finally {
            this.endDefer();
        }
    }

    public void cancelTask(Task task) {
        if (task == null) {
            throw new IllegalArgumentException("Task can't be null.");
        }
        this.beginDefer();
        try {
            task.cancel();
            for (SubTask s : new ArrayList<>(task.getSubtasks())) {
                if (s instanceof CollaboratorSubtask && s.isOpen()) {
                    applySubtaskCancel(s);
                }
            }
        } finally {
            this.endDefer();
        }
    }

    public void reopenTask(Task task) {
        if (task == null) {
            throw new IllegalArgumentException("Task can't be null.");
        }
        task.reopen();
        saveAfterChange();
    }

//---------------------------------SUBTASK STATUS---------------------------------

    public void completeSubtask(SubTask subtask) {
        if (subtask == null) {
            throw new IllegalArgumentException("Subtask can't be null.");
        }
        applySubtaskComplete(subtask);
        saveAfterChange();
    }

    public void cancelSubtask(SubTask subtask) {
        if (subtask == null) {
            throw new IllegalArgumentException("Subtask can't be null.");
        }
        applySubtaskCancel(subtask);
        saveAfterChange();
    }

    private static void applySubtaskComplete(SubTask subtask) {
        subtask.complete();
        if (subtask instanceof CollaboratorSubtask) {
            ((CollaboratorSubtask) subtask).getCollaborator().releaseAssignment((CollaboratorSubtask) subtask);
        }
    }

    private static void applySubtaskCancel(SubTask subtask) {
        subtask.cancel();
        if (subtask instanceof CollaboratorSubtask) {
            ((CollaboratorSubtask) subtask).getCollaborator().releaseAssignment((CollaboratorSubtask) subtask);
        }
    }

    public void reopenSubtask(SubTask subtask) {
        if (subtask == null) {
            throw new IllegalArgumentException("Subtask can't be null.");
        }
        if (subtask instanceof CollaboratorSubtask) {
            CollaboratorSubtask cs = (CollaboratorSubtask) subtask;
            if (!cs.getCollaborator().canAcceptTask()) {
                throw new IllegalStateException("Collaborator cannot accept another open assignment.");
            }
            cs.getCollaborator().assignedTask(cs);
        }
        subtask.reopen();
        saveAfterChange();
    }

    private void releaseCollaboratorAssignments(Task task) {
        for (SubTask s : task.getSubtasks()) {
            if (s instanceof CollaboratorSubtask && s.getStatus() == SubTaskStatus.OPEN) {
                ((CollaboratorSubtask) s).getCollaborator().releaseAssignment((CollaboratorSubtask) s);
            }
        }
    }

//---------------------------------LOAD FROM DATABASE (no SQLite write here)---------------------------------

    public void replaceState(List<Task> loaded, int nextIdValue) {
        this.tasks.clear();
        if (loaded != null) {
            this.tasks.addAll(loaded);
        }
        this.nextId = nextIdValue;
    }
}
