package service;

import model.Collaborator;
import model.CollaboratorCategory;
import model.Project;

//find-or-create collaborators on a project — SQLite updates when Tasks saves
public final class Collaborators {

    private final Projects projects;

//---------------------------------CONSTRUCTORS---------------------------------

    public Collaborators(Projects projects) {
        if (projects == null) {
            throw new IllegalArgumentException("Projects cannot be null.");
        }
        this.projects = projects;
    }

    public Projects getProjects() {
        return this.projects;
    }

//---------------------------------REGISTRATION---------------------------------

    //returns existing collaborator or creates one (DB save happens when Tasks saves)
    public Collaborator getOrCreate(Project project, String name, CollaboratorCategory category) {
        if (project == null) {
            throw new IllegalArgumentException("Project cannot be null.");
        }
        if (name == null || name.trim().isEmpty()) {
            return null;
        }
        if (category == null) {
            throw new IllegalArgumentException("Category is required when creating a collaborator.");
        }
        this.projects.save(project);
        Collaborator existing = project.findCollaboratorByName(name);
        if (existing != null) {
            return existing;
        }
        Collaborator created = new Collaborator(project, name, category);
        project.addCollaborator(created);
        return created;
    }

    public Collaborator findByName(Project project, String name) {
        if (project == null || name == null || name.trim().isEmpty()) {
            return null;
        }
        return project.findCollaboratorByName(name);
    }

//---------------------------------VALIDATION FOR TASKS---------------------------------

    public void validateForOpenAssignment(Collaborator collaborator, Project project) {
        if (collaborator == null) {
            return;
        }
        if (project == null || collaborator.getProject() != project) {
            throw new IllegalArgumentException("Collaborator must belong to the task's project.");
        }
        if (!collaborator.canAcceptTask()) {
            throw new IllegalArgumentException("Collaborator limit is reached.");
        }
    }
}
