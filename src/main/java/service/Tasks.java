package service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import model.Collaborator;
import model.CollaboratorSubtask;
import model.Project;
import model.RecurrencePattern;
import model.SearchCriteria;
import model.SubTask;
import model.SubTaskStatus;
import model.Task;
import model.TaskStatus;

public class Tasks {

    private final List<Task> tasks;
    private final Projects projects;
    private int nextId;

    public Tasks(Projects projects) {
        this.tasks = new ArrayList<>();
        this.projects = projects;
        this.nextId = 1;
    }

    public Task findById(int id) {
        for (Task t : this.tasks) {
            if (t.getId() == id) {
                return t;
            }
        }
        return null;
    }

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
    }

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

    public List<Task> createTaskSeries(String title, String description, String priorityLevel,
            LocalDate singleDueDate, Project project, List<String> tagKeywords,
            Collaborator collaborator, String collaboratorSubtaskTitle, String extraGeneralSubtaskTitle,
            RecurrencePattern pattern) {

        if (pattern != null) {
            List<LocalDate> dates = pattern.generateOccurrences();
            if (dates.isEmpty()) {
                throw new IllegalArgumentException("Recurrence pattern produced no occurrences.");
            }
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

        if (collaborator != null) {
            if (project == null || collaborator.getProject() != project) {
                throw new IllegalArgumentException("Collaborator must belong to the task's project.");
            }
            if (!collaborator.canAcceptTask()) {
                throw new IllegalArgumentException("Collaborator limit is reached.");
            }
        }

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

    public void completeTask(Task task) {
        if (task == null) {
            throw new IllegalArgumentException("Task can't be null.");
        }
        task.complete();
        for (SubTask s : new ArrayList<>(task.getSubtasks())) {
            if (s instanceof CollaboratorSubtask && s.isOpen()) {
                completeSubtask(s);
            }
        }
    }

    public void cancelTask(Task task) {
        if (task == null) {
            throw new IllegalArgumentException("Task can't be null.");
        }
        task.cancel();
        for (SubTask s : new ArrayList<>(task.getSubtasks())) {
            if (s instanceof CollaboratorSubtask && s.isOpen()) {
                cancelSubtask(s);
            }
        }
    }

    public void reopenTask(Task task) {
        if (task == null) {
            throw new IllegalArgumentException("Task can't be null.");
        }
        task.reopen();
    }

    public void completeSubtask(SubTask subtask) {
        if (subtask == null) {
            throw new IllegalArgumentException("Subtask can't be null.");
        }
        subtask.complete();
        if (subtask instanceof CollaboratorSubtask) {
            ((CollaboratorSubtask) subtask).getCollaborator().releaseAssignment((CollaboratorSubtask) subtask);
        }
    }

    public void cancelSubtask(SubTask subtask) {
        if (subtask == null) {
            throw new IllegalArgumentException("Subtask can't be null.");
        }
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
    }

    private void releaseCollaboratorAssignments(Task task) {
        for (SubTask s : task.getSubtasks()) {
            if (s instanceof CollaboratorSubtask && s.getStatus() == SubTaskStatus.OPEN) {
                ((CollaboratorSubtask) s).getCollaborator().releaseAssignment((CollaboratorSubtask) s);
            }
        }
    }

    public void replaceState(List<Task> loaded, int nextIdValue) {
        this.tasks.clear();
        if (loaded != null) {
            this.tasks.addAll(loaded);
        }
        this.nextId = nextIdValue;
    }
}
