package model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class Project {

    private String name;
    private String description;
    private final List<Task> tasks;
    private final List<Collaborator> collaborators;

    public Project(String name, String description) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Project name cannot be empty.");
        }
        this.name = name.trim();
        this.description = description == null ? "" : description;
        this.tasks = new ArrayList<>();
        this.collaborators = new ArrayList<>();
    }

    public String getName() {
        return this.name;
    }

    public String getDescription() {
        return this.description;
    }

    public List<Task> getTasks() {
        return Collections.unmodifiableList(this.tasks);
    }

    void addTaskInternal(Task task) {
        if (task != null && !this.tasks.contains(task)) {
            this.tasks.add(task);
        }
    }

    void removeTaskInternal(Task task) {
        this.tasks.remove(task);
    }

    public void addTask(Task task) {
        if (task == null) {
            throw new IllegalArgumentException("Task cannot be null.");
        }
        addTaskInternal(task);
        task.setProject(this);
    }

    public void removeTask(Task task) {
        if (task == null) {
            return;
        }
        removeTaskInternal(task);
        if (task.getProject() == this) {
            task.setProject(null);
        }
    }

    public List<Collaborator> getCollaborators() {
        return Collections.unmodifiableList(this.collaborators);
    }

    public Collaborator findCollaboratorByName(String collaboratorName) {
        if (collaboratorName == null || collaboratorName.trim().isEmpty()) {
            return null;
        }
        for (Collaborator c : this.collaborators) {
            if (c.getName().equalsIgnoreCase(collaboratorName.trim())) {
                return c;
            }
        }
        return null;
    }

    public Collaborator addCollaborator(Collaborator collaborator) {
        if (collaborator == null) {
            throw new IllegalArgumentException("Collaborator cannot be null.");
        }
        Collaborator existing = findCollaboratorByName(collaborator.getName());
        if (existing != null) {
            return existing;
        }
        this.collaborators.add(collaborator);
        return collaborator;
    }

    public void removeCollaborator(Collaborator collaborator) {
        this.collaborators.remove(collaborator);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Project project = (Project) o;
        return this.name.equalsIgnoreCase(project.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.name.toLowerCase());
    }

    @Override
    public String toString() {
        return "Project{name='" + this.name + "', description='" + this.description + "'}";
    }
}
