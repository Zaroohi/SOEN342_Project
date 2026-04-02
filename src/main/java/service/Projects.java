package service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import model.Project;

//all projects in memory — SQLite is updated when Tasks saves
public class Projects {

    private final List<Project> projects;

//---------------------------------CONSTRUCTORS---------------------------------

    public Projects() {
        this.projects = new ArrayList<>();
    }

//---------------------------------LOOKUP---------------------------------

    public Project findByName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return null;
        }
        for (Project p : this.projects) {
            if (p.getName().equalsIgnoreCase(name.trim())) {
                return p;
            }
        }
        return null;
    }

//---------------------------------MUTATIONS---------------------------------

    public void save(Project project) {
        if (project == null) {
            throw new IllegalArgumentException("Project cannot be null.");
        }
        Project existing = findByName(project.getName());
        if (existing != null && existing != project) {
            throw new IllegalArgumentException("Another project with the same name already exists.");
        }
        if (!this.projects.contains(project)) {
            this.projects.add(project);
        }
    }

    public void deleteName(String name) {
        Project p = findByName(name);
        if (p == null) {
            return;
        }
        for (model.Task t : new ArrayList<>(p.getTasks())) {
            p.removeTask(t);
        }
        this.projects.remove(p);
    }

    public List<Project> getAllProjects() {
        return Collections.unmodifiableList(this.projects);
    }

//---------------------------------LOAD FROM DATABASE---------------------------------

    public void replaceState(List<Project> loaded) {
        this.projects.clear();
        if (loaded != null) {
            this.projects.addAll(loaded);
        }
    }
}
