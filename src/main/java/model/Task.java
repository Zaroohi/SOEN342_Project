package model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Task {

    public static final int MAX_SUBTASKS = 20;

    private final int id;
    private String title;
    private String description;
    private final LocalDateTime creationDate;
    private String priorityLevel;
    private TaskStatus status;
    private LocalDate dueDate;
    private RecurrencePattern recurrencePattern;
    private Project project;
    private final List<SubTask> subtasks;
    private final List<Tag> tags;
    private final List<ActivityEntry> activityHistory;

//---------------------------------CONSTRUCTORS---------------------------------

    //creates a new task, sets status to OPEN and log a CREATED entry (when created from the menu)
    public Task(int id, String title, String description, String priorityLevel, LocalDate dueDate) {
        requireNonEmptyTitle(title);
        this.id = id;
        this.title = title.trim();
        this.description = description == null ? "" : description;
        this.priorityLevel = priorityLevel == null ? "" : priorityLevel;
        this.dueDate = dueDate;
        this.status = TaskStatus.OPEN;
        this.creationDate = LocalDateTime.now();
        this.recurrencePattern = null;
        this.project = null;
        this.subtasks = new ArrayList<>();
        this.tags = new ArrayList<>();
        this.activityHistory = new ArrayList<>();
        log(ActionType.CREATED, "Task created");
    }

    //loads a task from the database, uses saved status and creation time
    public Task(int id, String title, String description, String priorityLevel, LocalDate dueDate,
            TaskStatus status, LocalDateTime creationDate) {
        requireNonEmptyTitle(title);
        this.id = id;
        this.title = title.trim();
        this.description = description == null ? "" : description;
        this.priorityLevel = priorityLevel == null ? "" : priorityLevel;
        this.dueDate = dueDate;
        this.status = status != null ? status : TaskStatus.OPEN;
        this.creationDate = creationDate != null ? creationDate : LocalDateTime.now();
        this.recurrencePattern = null;
        this.project = null;
        this.subtasks = new ArrayList<>();
        this.tags = new ArrayList<>();
        this.activityHistory = new ArrayList<>();
    }

    //helper method to check if the title is not empty
    private static void requireNonEmptyTitle(String title) {
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Task title cannot be empty.");
        }
    }

//---------------------------------LOADING FROM DATABASE---------------------------------

    //from the database, adds a subtask to the task and ignores the activity log
    public void attachLoadedSubtask(SubTask subtask) {
        if (subtask != null && !this.subtasks.contains(subtask)) {
            ensureSubtaskCapacity();
            this.subtasks.add(subtask);
            subtask.bindParentTask(this);
        }
    }

    //from the database, adds a tag to the task and ignores the activity log
    public void attachLoadedTagKeyword(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return;
        }
        Tag tag = new Tag(keyword.trim());
        if (!this.tags.contains(tag)) {
            this.tags.add(tag);
        }
    }

//---------------------------------GETTERS AND SETTERS---------------------------------

    public int getId() {
        return this.id;
    }

    public String getTitle() {
        return this.title;
    }

    public void setTitle(String title) {
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Task title cannot be empty.");
        }
        this.title = title.trim();
        log(ActionType.UPDATED, "Title updated");
    }

    public String getDescription() {
        return this.description;
    }

    public void setDescription(String description) {
        this.description = description == null ? "" : description;
        log(ActionType.UPDATED, "Description updated");
    }

    public LocalDateTime getCreationDate() {
        return this.creationDate;
    }

    public String getPriorityLevel() {
        return this.priorityLevel;
    }

    public void setPriorityLevel(String priorityLevel) {
        this.priorityLevel = priorityLevel == null ? "" : priorityLevel;
        log(ActionType.UPDATED, "Priority updated");
    }

    public TaskStatus getStatus() {
        return this.status;
    }

    public void setStatus(TaskStatus status) {
        if (status != null) {
            this.status = status;
            log(ActionType.UPDATED, "Status set to " + status);
        }
    }

    public LocalDate getDueDate() {
        return this.dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
        log(ActionType.UPDATED, "Due date updated");
    }

    public boolean isRecurring() {
        return this.recurrencePattern != null;
    }

    public RecurrencePattern getRecurrencePattern() {
        return this.recurrencePattern;
    }

    public void setRecurrencePattern(RecurrencePattern recurrencePattern) {
        this.recurrencePattern = recurrencePattern;
        log(ActionType.UPDATED, recurrencePattern == null ? "Recurrence cleared" : "Recurrence pattern set");
    }

    public Project getProject() {
        return this.project;
    }

    void setProject(Project project) {
        this.project = project;
    }

    public List<SubTask> getSubtasks() {
        return Collections.unmodifiableList(this.subtasks);
    }

//---------------------------------SUBTASK MANAGEMENT---------------------------------

    //helper method to add a subtask to the task and log the action
    private void addSubtaskInternal(SubTask subtask) {
        if (subtask != null && !this.subtasks.contains(subtask)) {
            ensureSubtaskCapacity();
            this.subtasks.add(subtask);
            subtask.bindParentTask(this);
            log(ActionType.SUBTASK_ADDED, "Subtask added: " + subtask.getTitle());
        }
    }

    private void ensureSubtaskCapacity() {
        if (this.subtasks.size() >= MAX_SUBTASKS) {
            throw new IllegalStateException("A task cannot have more than " + MAX_SUBTASKS + " subtasks.");
        }
    }
    //adds a subtask to the task and logs the action
    public void addSubtask(SubTask subtask) {
        addSubtaskInternal(subtask);
    }

    public void addGeneralSubtask(String title) {
        addSubtask(new GeneralSubtask(title));
    }

//---------------------------------TAG MANAGEMENT---------------------------------

    public List<Tag> getTags() {
        return Collections.unmodifiableList(this.tags);
    }

    public void addTag(Tag tag) {
        if (tag != null && !this.tags.contains(tag)) {
            this.tags.add(tag);
            log(ActionType.TAG_ADDED, "Tag added: " + tag.getKeyword());
        }
    }

    public void addTagKeyword(String keyword) {
        addTag(new Tag(keyword));
    }

    public void removeTag(Tag tag) {
        if (this.tags.remove(tag)) {
            log(ActionType.TAG_REMOVED, "Tag removed: " + tag.getKeyword());
        }
    }

//---------------------------------ACTIVITY HISTORY---------------------------------

    public List<ActivityEntry> getActivityHistory() {
        return Collections.unmodifiableList(this.activityHistory);
    }

    public String getSubtaskProgress() {
        if (this.subtasks.isEmpty()) {
            return "0/0";
        }
        int done = 0;
        for (SubTask s : this.subtasks) {
            if (s.getStatus() == SubTaskStatus.COMPLETED) {
                done++;
            }
        }
        return done + "/" + this.subtasks.size();
    }

    public void complete() {
        this.status = TaskStatus.COMPLETED;
        log(ActionType.COMPLETED, "Task completed");
    }

    public void cancel() {
        this.status = TaskStatus.CANCELLED;
        log(ActionType.CANCELLED, "Task cancelled");
    }

    public void reopen() {
        if (this.status != TaskStatus.OPEN) {
            this.status = TaskStatus.OPEN;
            log(ActionType.UPDATED, "Task reopened");
        }
    }

    public boolean isOpen() {
        return this.status == TaskStatus.OPEN;
    }

    private void log(ActionType type, String description) {
        this.activityHistory.add(new ActivityEntry(type, description));
    }
//---------------------------------UTILITY METHODS---------------------------------

    public String subtasksSummaryForCsv() {
        if (this.subtasks.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < this.subtasks.size(); i++) {
            if (i > 0) {
                sb.append("; ");
            }
            sb.append(this.subtasks.get(i).getTitle());
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return "Task:id=" + this.id + ", title='" + this.title + "', description='" + this.description
                + "', priority='" + this.priorityLevel + "', dueDate=" + this.dueDate
                + ", status=" + this.status + ", project=" + this.project
                + ", recurring=" + isRecurring() + ", subtasks=" + getSubtaskProgress()
                + ", tags=" + this.tags ;
    }
}
