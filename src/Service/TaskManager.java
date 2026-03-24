package Service;

import model.Collaborator;
import model.Project;
import model.SearchCriteria;
import model.Task;
import java.util.Comparator;
import java.util.List;
import model.TaskStatus;
import java.time.LocalDate;
import java.util.ArrayList;
 

public class TaskManager {

    private List<Task> tasks;
    private List<Project> projects;
    private List<Collaborator> collaborators;

    //constr.
    public TaskManager() {
        this.tasks = new ArrayList<>();
        this.projects = new ArrayList<>();
        this.collaborators = new ArrayList<>();
    }

    //create, add a task
    public Task createTask(String title, String description, String priority, LocalDate dueDate,
                           Project project, List<String> tags, String recurrencePattern,
                           Collaborator collaborator) {

        //validation
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Task title can't be empty.");
        }

        if (dueDate == null) {
            throw new IllegalArgumentException("Due date can't be null.");
        }

        //uniqueness of title&due date
        for (Task currentTask : this.tasks) {
            if (currentTask.getTitle().equalsIgnoreCase(title)
                    && currentTask.getDueDate().equals(dueDate)) {
                throw new IllegalArgumentException("A task with the same due date and title already exists.");
            }
        }

        //check collaborator limit (if assigned)
        if (collaborator != null && !collaborator.canAcceptMoreTasks()) {
            throw new IllegalArgumentException("Collaborator limit is reached.");
        }

        Task newTask = new Task(title, description, priority, dueDate);

        if (project != null) {
            newTask.setProject(project);
            if (!this.projects.contains(project)) {
                this.projects.add(project);
            }
        }

        if (tags != null) {
            for (String currentTag : tags) {
                newTask.addTag(currentTag);
            }
        }

        if (recurrencePattern != null) {
            newTask.setRecurrencePattern(recurrencePattern);
        }

        if (collaborator != null) {
            newTask.setCollaborator(collaborator);
            newTask.setSubtask("Complete assigned collaborator work");
            collaborator.incrementOpenTaskCount();

            if (!this.collaborators.contains(collaborator)) {
                this.collaborators.add(collaborator);
            }
        }

        this.tasks.add(newTask);
        return newTask;
    }

    //return all tasks
    public List<Task> getAllTasks() {
        return new ArrayList<>(this.tasks);
    }

    //search tasks (criteria)
    public List<Task> searchTasks(SearchCriteria criteria) {
        List<Task> matchingTasks = new ArrayList<>();

        //if no criteria, return all tasks that are open
        if (criteria == null || criteria.isEmpty()) {
            for (Task currentTask : this.tasks) {
                if (currentTask.getStatus() == TaskStatus.OPEN) {
                    matchingTasks.add(currentTask);
                }
            }
       matchingTasks.sort(Comparator.comparing(Task::getDueDate));
            return matchingTasks;
        }

        for (Task currentTask : this.tasks) {
            boolean matches = true;

            //filter by title keyword
            if (criteria.getTitleKeyword() != null &&
                    !criteria.getTitleKeyword().trim().isEmpty()) {
                if (currentTask.getTitle() == null ||
                        !currentTask.getTitle().toLowerCase().contains(criteria.getTitleKeyword().toLowerCase())) {
                    matches = false;
                }
            }

            //filter by status
            if (matches && criteria.getStatus() != null) {
                if (currentTask.getStatus() != criteria.getStatus()) {
                    matches = false;
                }
            }

            //filter by start date
            if (matches && criteria.getStartDate() != null) {
                if (currentTask.getDueDate().isBefore(criteria.getStartDate())) {
                    matches = false;
                } }

            //filter by end date
            if (matches && criteria.getEndDate() != null) {
                if (currentTask.getDueDate().isAfter(criteria.getEndDate())) {
                    matches = false;
                }
            }
            //filter by day of week
            if (matches && criteria.getDayOfWeek() != null &&
                    !criteria.getDayOfWeek().trim().isEmpty()) {
                String taskDay = currentTask.getDueDate().getDayOfWeek().toString();
                if (!taskDay.equalsIgnoreCase(criteria.getDayOfWeek())) {
                    matches = false;
                }
            }
            if (matches) {
                matchingTasks.add(currentTask);
            }
        }

        matchingTasks.sort(Comparator.comparing(Task::getDueDate));
        return matchingTasks;
    }

    //task as completed
    public void completeTask(Task task) {
        if (task == null) {
            throw new IllegalArgumentException("Task can't be null.");
        }

        task.setStatus(TaskStatus.COMPLETED);

        if (task.getCollaborator() != null) {
            task.getCollaborator().decrementOpenTaskCount();
        }
    }

    //cancel a task
    public void cancelTask(Task task) {
        if (task == null) {
            throw new IllegalArgumentException("Task can't be null.");
        }

        task.setStatus(TaskStatus.CANCELLED);

        if (task.getCollaborator() != null) {
            task.getCollaborator().decrementOpenTaskCount();
        }
    }

    //add project manually
    public void addProject(Project project) {
        if (project != null && !this.projects.contains(project)) {
            this.projects.add(project);
        }
    }

    //add collaborator manually
    public void addCollaborator(Collaborator collaborator) {
        if (collaborator != null && !this.collaborators.contains(collaborator)) {
            this.collaborators.add(collaborator);
        }
    }

    //find project by name
    public Project findProjectByName(String projectName) {
        for (Project currentProject : this.projects) {
            if (currentProject.getName().equalsIgnoreCase(projectName)) {
                return currentProject;
            }
        }
        return null;
    }

    //finding collaborator by name
    public Collaborator findCollaboratorByName(String collaboratorName) {
        for (Collaborator currentCollaborator : this.collaborators) {
            if (currentCollaborator.getName().equalsIgnoreCase(collaboratorName)) {
                return currentCollaborator;
            }
        }
        return null;  }

    //getting all projects
    public List<Project> getAllProjects() {
        return new ArrayList<>(this.projects);
    }
    //getting collaborators
    public List<Collaborator> getAllCollaborators() {
        return new ArrayList<>(this.collaborators);
    }
}