package model;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;


public class Task {

    private String title;
    private String description;
    private String priority;
    private Project project;
    private List<String> tags;
    private Collaborator collaborator;
    private String subtask;
    private String recurrencePattern;

    private LocalDate dueDate;
    private TaskStatus status;
     
    //constr. 
    public Task(String title, String description, String priority, LocalDate dueDate) {
        this.title = title;
        this.description = description;
        this.priority = priority;
        this.dueDate = dueDate;
        this.status = TaskStatus.OPEN;
        this.project = null;
        this.tags = new ArrayList<>();
        this.collaborator = null;
        this.subtask = "";
        this.recurrencePattern = ""; }

    //getter title
    public String getTitle() {
        return this.title;
    }

    //setter title
    public void setTitle(String title) {
        this.title = title; }

    //getter description
    public String getDescription() {
        return this.description;
    }

    //setter description
    public void setDescription(String description) {
        this.description = description;
    }

    //getter priority
    public String getPriority() {
        return this.priority;
    }

    //setter priority
    public void setPriority(String priority) {
        this.priority = priority;
    }

    //gette due date
    public LocalDate getDueDate() {
        return this.dueDate;
    }

    //setter due date
    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    //getter status
    public TaskStatus getStatus() {
        return this.status;
    }

    //setter status
    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    //getter project
    public Project getProject() {
        return this.project;
    }

    //setter project
    public void setProject(Project project) {
        this.project = project;
    }

    //getter tags
    public List<String> getTags() {
        return this.tags;
    }

    //add one tag
    public void addTag(String tag) {
        this.tags.add(tag);
    }

    //remove one tag
    public void removeTag(String tag) {
        this.tags.remove(tag);
    }

    //getter for collaborator
    public Collaborator getCollaborator() {
        return this.collaborator;
    }

    //setter collaborator
    public void setCollaborator(Collaborator collaborator) {
        this.collaborator = collaborator;
    }

    //getter subtask
    public String getSubtask() {
        return this.subtask; }

    //setter subtask
    public void setSubtask(String subtask) {
        this.subtask = subtask;
    }
    //getter recurrence pattern
    public String getRecurrencePattern() {
        return this.recurrencePattern;
    }
    //setter recurrence pattern
    public void setRecurrencePattern(String recurrencePattern) {
        this.recurrencePattern = recurrencePattern;
    }
    //check if task open
    public boolean isOpen() {
        return this.status == TaskStatus.OPEN;
    }

    @Override
    public String toString() {
        return "Task: title='" + this.title + "', description='" + this.description +
                "', priority='" + this.priority + "', dueDate=" + this.dueDate +
                ", status=" + this.status + ", project=" + this.project +
                ", tags=" + this.tags + ", collaborator=" + this.collaborator +
                ", subtask='" + this.subtask + "', recurrencePattern='" + this.recurrencePattern;
    }
}