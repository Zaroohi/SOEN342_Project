package model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Task {

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

    public Task(int id, String title, String description, String priorityLevel, LocalDate dueDate) {
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Task title cannot be empty.");
        }
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

    private void addSubtaskInternal(SubTask subtask) {
        if (subtask != null && !this.subtasks.contains(subtask)) {
            this.subtasks.add(subtask);
            subtask.bindParentTask(this);
            log(ActionType.SUBTASK_ADDED, "Subtask added: " + subtask.getTitle());
        }
    }

    public void addSubtask(SubTask subtask) {
        addSubtaskInternal(subtask);
    }

    public void addGeneralSubtask(String title) {
        addSubtask(new GeneralSubtask(title));
    }

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

    public CollaboratorSubtask findCollaboratorSubtaskFor(Collaborator collaborator) {
        if (collaborator == null) {
            return null;
        }
        for (SubTask s : this.subtasks) {
            if (s instanceof CollaboratorSubtask) {
                CollaboratorSubtask cs = (CollaboratorSubtask) s;
                if (collaborator.equals(cs.getCollaborator())) {
                    return cs;
                }
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return "Task{id=" + this.id + ", title='" + this.title + "', description='" + this.description
                + "', priority='" + this.priorityLevel + "', dueDate=" + this.dueDate
                + ", status=" + this.status + ", project=" + this.project
                + ", recurring=" + isRecurring() + ", subtasks=" + getSubtaskProgress()
                + ", tags=" + this.tags + "}";
    }
}
