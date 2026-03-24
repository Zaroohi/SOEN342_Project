package Service;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import model.Collaborator;
import model.Project;
import model.Task;

 

public class CsvExporter {

    private TaskManager taskManager;

    //constructor
    public CsvExporter(TaskManager taskManager) {
        this.taskManager = taskManager;
    }

    //export all tasks to csv file
    public void exportToCSV(String filePath) throws IOException {
        PrintWriter printWriter = new PrintWriter(new FileWriter(filePath));

        printWriter.println("TaskName,Description,Subtask,Status,Priority,DueDate,ProjectName,ProjectDescription,Collaborator,CollaboratorCategory");

        List<Task> allTasks = this.taskManager.getAllTasks();

        for (Task currentTask : allTasks) {
            String taskName = escapeCsv(currentTask.getTitle());
            String description = escapeCsv(currentTask.getDescription());
            String subtask = escapeCsv(currentTask.getSubtask());
            String status = currentTask.getStatus().toString();
            String priority = escapeCsv(currentTask.getPriority());
            String dueDate = currentTask.getDueDate().toString();

            Project project = currentTask.getProject();
            String projectName = "";
            String projectDescription = "";

            if (project != null) {
                projectName = escapeCsv(project.getName());
                projectDescription = escapeCsv(project.getDescription());
            }

            Collaborator collaborator = currentTask.getCollaborator();
            String collaboratorName = "";
            String collaboratorCategory = "";

            if (collaborator != null) {
                collaboratorName = escapeCsv(collaborator.getName());
                collaboratorCategory = collaborator.getCategory().toString();
            }

            printWriter.println(
                    taskName + "," +
                    description + "," +
                    subtask + "," +
                    status + "," +
                    priority + "," +
                    dueDate + "," +
                    projectName + "," +
                    projectDescription + "," +
                    collaboratorName + "," +
                    collaboratorCategory
            );
        }

        printWriter.close();
    }
    //escape csv values containing " or ,
    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"")) {
            value = value.replace("\"", "\"\"");
            return "\"" + value + "\"";
        }

        return value;
    }
}