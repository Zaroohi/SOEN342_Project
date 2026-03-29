package model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class Collaborator {

    private final Project project;
    private String name;
    private CollaboratorCategory category;
    private final List<CollaboratorSubtask> openAssignments;

    public Collaborator(Project project, String name, CollaboratorCategory category) {
        if (project == null) {
            throw new IllegalArgumentException("Collaborator must belong to a project.");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Collaborator name cannot be empty.");
        }
        if (category == null) {
            throw new IllegalArgumentException("Category is required.");
        }
        this.project = project;
        this.name = name.trim();
        this.category = category;
        this.openAssignments = new ArrayList<>();
    }

    public Project getProject() {
        return this.project;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Collaborator name cannot be empty.");
        }
        this.name = name.trim();
    }

    public CollaboratorCategory getCategory() {
        return this.category;
    }

    public void setCategory(CollaboratorCategory category) {
        if (category == null) {
            throw new IllegalArgumentException("Category is required.");
        }
        this.category = category;
    }

    public int getOpenTasksLimit() {
        return this.category.getMaxOpenTasks();
    }

    public int getActiveOpenTasks() {
        return this.openAssignments.size();
    }

    public boolean canAcceptTask() {
        return getActiveOpenTasks() < getOpenTasksLimit();
    }

    public void trackOpenAssignment(CollaboratorSubtask subtask) {
        if (subtask != null && !this.openAssignments.contains(subtask)) {
            this.openAssignments.add(subtask);
        }
    }

    public void releaseAssignment(CollaboratorSubtask subtask) {
        this.openAssignments.remove(subtask);
    }

    public void assignedTask(CollaboratorSubtask subtask) {
        if (subtask == null) {
            throw new IllegalArgumentException("Subtask cannot be null.");
        }
        if (!canAcceptTask()) {
            throw new IllegalStateException("Collaborator open task limit reached.");
        }
        trackOpenAssignment(subtask);
    }

    public List<Task> getOpenTasks() {
        List<Task> parents = new ArrayList<>();
        for (CollaboratorSubtask sub : this.openAssignments) {
            Task parent = sub.getParentTask();
            if (parent != null && !parents.contains(parent)) {
                parents.add(parent);
            }
        }
        return Collections.unmodifiableList(parents);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Collaborator that = (Collaborator) o;
        return this.project.equals(that.project) && this.name.equalsIgnoreCase(that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.project, this.name.toLowerCase());
    }

    @Override
    public String toString() {
        return "Collaborator{name='" + this.name + "', category=" + this.category
                + ", activeOpenTasks=" + getActiveOpenTasks() + "/" + getOpenTasksLimit() + "}";
    }
}
