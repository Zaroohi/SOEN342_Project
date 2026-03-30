package service;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import model.Collaborator;
import model.CollaboratorSubtask;
import model.Project;
import model.SubTask;
import model.Task;

//writes current in-memory tasks to a CSV file only — does not read or write SQLite (DB is unchanged by export)
public class CsvExporter {

    public static final String HEADER =
            "TaskName,Description,Subtask,Status,Priority,DueDate,ProjectName,ProjectDescription,Collaborator,CollaboratorCategory";

    private final Tasks tasks;

//---------------------------------CONSTRUCTORS---------------------------------

    public CsvExporter(Tasks tasks) {
        this.tasks = tasks;
    }

//---------------------------------EXPORT---------------------------------

    public void exportToCsv(String filePath) throws IOException {
        PrintWriter writer = new PrintWriter(new FileWriter(filePath));
        writer.println(HEADER);
        for (Task t : this.tasks.getAllTasks()) {
            Project p = t.getProject();
            String projectName = p != null ? escapeCsv(p.getName()) : "";
            String projectDesc = p != null ? escapeCsv(p.getDescription()) : "";
            CollaboratorSubtask collabSub = firstCollaboratorSubtask(t);
            String collabName = "";
            String collabCat = "";
            if (collabSub != null) {
                Collaborator c = collabSub.getCollaborator();
                collabName = escapeCsv(c.getName());
                collabCat = c.getCategory().name();
            }
            writer.println(
                    escapeCsv(t.getTitle()) + ","
                            + escapeCsv(t.getDescription()) + ","
                            + escapeCsv(t.subtasksSummaryForCsv()) + ","
                            + t.getStatus().name() + ","
                            + escapeCsv(t.getPriorityLevel()) + ","
                            + (t.getDueDate() != null ? t.getDueDate().toString() : "") + ","
                            + projectName + ","
                            + projectDesc + ","
                            + collabName + ","
                            + collabCat
            );
        }
        writer.close();
    }

//---------------------------------HELPERS---------------------------------

    private static CollaboratorSubtask firstCollaboratorSubtask(Task t) {
        for (SubTask s : t.getSubtasks()) {
            if (s instanceof CollaboratorSubtask) {
                return (CollaboratorSubtask) s;
            }
        }
        return null;
    }

    private static String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
