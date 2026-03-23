package Service;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import model.Collaborator;
import model.CollaboratorCategory;
import model.Project;

 
public class CsvImporter {

    private TaskManager taskManager;

    //constructor
    public CsvImporter(TaskManager taskManager) {
        this.taskManager = taskManager;
    }

    //import tasks from csv file
    public void importFromCSV(String filePath) throws IOException {
        BufferedReader bufferedReader = new BufferedReader(new FileReader(filePath));
        String currentLine;
        boolean isFirstLine = true;

        while ((currentLine = bufferedReader.readLine()) != null) {
            if (currentLine.trim().isEmpty()) {
                continue;
            }

            //skipping header row
            if (isFirstLine) {
                isFirstLine = false;
                if (currentLine.toLowerCase().contains("taskname")) {
                    continue;
                }
            }

            String[] values = currentLine.split(",", -1);

        
            if (values.length < 10) {
                continue;
            }

            String taskTitle = values[0].trim();
            String taskDescription = values[1].trim();
            String subtask = values[2].trim();
            String taskStatus = values[3].trim(); //not used yett
            String taskPriority = values[4].trim();
            String dueDateText = values[5].trim();
            String projectName = values[6].trim();
            String projectDescription = values[7].trim();
            String collaboratorName = values[8].trim();
            String collaboratorCategoryText = values[9].trim();

            if (taskTitle.isEmpty() || dueDateText.isEmpty()) {
                continue;
            }

            LocalDate dueDate = LocalDate.parse(dueDateText);

            Project project = null;
            if (!projectName.isEmpty()) {
                project = this.taskManager.findProjectByName(projectName);

                if (project == null) {
                    project = new Project(projectName, projectDescription);
                    this.taskManager.addProject(project);
                }
            }

            Collaborator collaborator = null;
            if (!collaboratorName.isEmpty() && !collaboratorCategoryText.isEmpty()) {
                collaborator = this.taskManager.findCollaboratorByName(collaboratorName);

                if (collaborator == null) {
                    CollaboratorCategory collaboratorCategory =
                            CollaboratorCategory.valueOf(collaboratorCategoryText.toUpperCase());

                    collaborator = new Collaborator(collaboratorName, collaboratorCategory);
                    this.taskManager.addCollaborator(collaborator);
                }
            }

            List<String> tags = new ArrayList<>();

            this.taskManager.createTask(
                    taskTitle,
                    taskDescription,
                    taskPriority,
                    dueDate,
                    project,
                    tags,
                    "",
                    collaborator
            );

            //set imported subtask (if exists)
            if (!subtask.isEmpty()) {
                this.taskManager.getAllTasks()
                        .get(this.taskManager.getAllTasks().size() - 1)
                        .setSubtask(subtask);   }
            //set imported status (if we need i)
            if (!taskStatus.isEmpty()) {
                String normalizedStatus = taskStatus.toUpperCase();

                if (normalizedStatus.equals("COMPLETED")) {
                    this.taskManager.completeTask(
                            this.taskManager.getAllTasks().get(this.taskManager.getAllTasks().size() - 1)
                    );
                }
                else if (normalizedStatus.equals("CANCELLED")) {
                    this.taskManager.cancelTask(
                            this.taskManager.getAllTasks().get(this.taskManager.getAllTasks().size() - 1)
                    );
                }
            }  }

        bufferedReader.close();
    }
}