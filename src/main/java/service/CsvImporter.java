package service;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import model.Collaborator;
import model.CollaboratorCategory;
import model.Project;
import model.Task;

//reads CSV; Tasks defers one DB write until the file is done
public class CsvImporter {

    private final Tasks tasks;
    private final Projects projects;
    private final Collaborators collaborators;

//---------------------------------CONSTRUCTORS---------------------------------

    public CsvImporter(Tasks tasks, Projects projects, Collaborators collaborators) {
        this.tasks = tasks;
        this.projects = projects;
        this.collaborators = collaborators;
    }

//---------------------------------IMPORT---------------------------------

    public void importFromCsv(String filePath) throws IOException {
        this.tasks.beginDefer();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            importFromOpenReader(reader);
        } finally {
            this.tasks.endDefer();
        }
    }

    private void importFromOpenReader(BufferedReader reader) throws IOException {
        String line;
        boolean first = true;
        while ((line = reader.readLine()) != null) {
            if (line.trim().isEmpty()) {
                continue;
            }
            if (first) {
                first = false;
                if (line.toLowerCase().contains("taskname")) {
                    continue;
                }
            }
            String[] values = line.split(",", -1);
            if (values.length < 10) {
                continue;
            }
            String taskTitle = values[0].trim();
            String taskDescription = values[1].trim();
            String subtaskText = values[2].trim();
            String taskStatus = values[3].trim();
            String taskPriority = values[4].trim();
            String dueDateText = values[5].trim();
            String projectName = values[6].trim();
            String projectDescription = values[7].trim();
            String collaboratorName = values[8].trim();
            String collaboratorCategoryText = values[9].trim();

            if (taskTitle.isEmpty()) {
                continue;
            }

            LocalDate dueDate = null;
            if (!dueDateText.isEmpty()) {
                dueDate = LocalDate.parse(dueDateText);
            }

            Project project = null;
            if (!projectName.isEmpty()) {
                project = this.projects.findByName(projectName);
                if (project == null) {
                    project = new Project(projectName, projectDescription);
                    this.projects.save(project);
                }
            }

            Collaborator collaborator = null;
            if (project != null && !collaboratorName.isEmpty() && !collaboratorCategoryText.isEmpty()) {
                CollaboratorCategory cat = CollaboratorCategory.valueOf(collaboratorCategoryText.toUpperCase());
                collaborator = this.collaborators.getOrCreate(project, collaboratorName, cat);
            }

            List<String> tags = new ArrayList<>();
            String collabSubTitle = collaborator != null
                    ? (subtaskText.isEmpty() ? null : subtaskText)
                    : null;
            String generalSubTitle = collaborator == null && !subtaskText.isEmpty() ? subtaskText : null;

            Task task = this.tasks.createSingleTask(taskTitle, taskDescription, taskPriority, dueDate, project, tags,
                    collaborator, collabSubTitle, generalSubTitle, null);

            if (!taskStatus.isEmpty()) {
                String normalized = taskStatus.toUpperCase();
                if ("COMPLETED".equals(normalized)) {
                    this.tasks.completeTask(task);
                } else if ("CANCELLED".equals(normalized)) {
                    this.tasks.cancelTask(task);
                }
            }
        }
    }
}
