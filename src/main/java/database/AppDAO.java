package database;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import model.Collaborator;
import model.CollaboratorCategory;
import model.CollaboratorSubtask;
import model.GeneralSubtask;
import model.Project;
import model.SubTask;
import model.SubTaskStatus;
import model.Tag;
import model.Task;
import model.TaskStatus;
import service.Projects;
import service.Tasks;

//single persistence class that loads/saves projects, collaborators, tasks, subtasks, and tags
public final class AppDAO {

    private final Connection conn;

//---------------------------------CONSTRUCTORS---------------------------------

    public AppDAO(Connection conn) {
        this.conn = conn;
    }

//---------------------------------LOAD ALL INTO MEMORY---------------------------------

    public void loadAll(Projects projects, Tasks tasks) throws IOException {
        try {
            projects.replaceState(new ArrayList<>());
            tasks.replaceState(new ArrayList<>(), 1);

            loadProjectsInto(projects);
            Map<Long, Collaborator> collabById = loadCollaboratorsInto(projects);

            Map<Integer, Task> taskById = new HashMap<>();
            int maxId = 0;
            try (Statement st = this.conn.createStatement();
                    ResultSet rs = st.executeQuery(
                            "SELECT id, title, description, priority, status, due_date, project_name, creation_time "
                                    + "FROM task ORDER BY id")) {
                while (rs.next()) {
                    int id = rs.getInt("id");
                    maxId = Math.max(maxId, id);
                    String dueStr = rs.getString("due_date");
                    LocalDate due = dueStr == null || dueStr.isEmpty() ? null : LocalDate.parse(dueStr);
                    TaskStatus status = TaskStatus.valueOf(rs.getString("status"));
                    LocalDateTime created = LocalDateTime.parse(rs.getString("creation_time"));
                    Task t = new Task(id, rs.getString("title"), rs.getString("description"),
                            rs.getString("priority"), due, status, created);
                    String pname = rs.getString("project_name");
                    if (pname != null && !pname.isEmpty()) {
                        Project p = projects.findByName(pname);
                        if (p != null) {
                            p.addTask(t);
                        }
                    }
                    taskById.put(id, t);
                }
            }

            try (Statement st = this.conn.createStatement();
                    ResultSet rs = st.executeQuery(
                            "SELECT task_id, sort_order, kind, title, status, collaborator_id FROM subtask "
                                    + "ORDER BY task_id, sort_order")) {
                while (rs.next()) {
                    Task task = taskById.get(rs.getInt("task_id"));
                    if (task == null) {
                        continue;
                    }
                    String kind = rs.getString("kind");
                    String title = rs.getString("title");
                    SubTaskStatus stEnum = SubTaskStatus.valueOf(rs.getString("status"));
                    if ("G".equals(kind)) {
                        GeneralSubtask g = new GeneralSubtask(title);
                        g.setStatus(stEnum);
                        task.attachLoadedSubtask(g);
                    } else if ("C".equals(kind)) {
                        long cid = rs.getLong("collaborator_id");
                        Collaborator col = collabById.get(cid);
                        if (col == null) {
                            continue;
                        }
                        CollaboratorSubtask cs = new CollaboratorSubtask(title, col);
                        cs.setStatus(stEnum);
                        task.attachLoadedSubtask(cs);
                        if (stEnum == SubTaskStatus.OPEN) {
                            col.assignedTask(cs);
                        }
                    }
                }
            }

            try (Statement st = this.conn.createStatement();
                    ResultSet rs = st.executeQuery("SELECT task_id, tag FROM task_tag ORDER BY task_id, tag")) {
                while (rs.next()) {
                    Task task = taskById.get(rs.getInt("task_id"));
                    if (task != null) {
                        task.attachLoadedTagKeyword(rs.getString("tag"));
                    }
                }
            }

            List<Task> ordered = new ArrayList<>(taskById.values());
            ordered.sort(Comparator.comparingInt(Task::getId));
            tasks.replaceState(ordered, maxId > 0 ? maxId + 1 : 1);
        } catch (SQLException e) {
            throw new IOException("Failed to load database", e);
        }
    }

//---------------------------------SAVE FULL SNAPSHOT (live replace of DB contents)---------------------------------

    public void saveAll(Projects projects, Tasks tasks) throws IOException {
        try {
            this.conn.setAutoCommit(false);
            try (Statement st = this.conn.createStatement()) {
                st.executeUpdate("DELETE FROM task_tag");
                st.executeUpdate("DELETE FROM subtask");
                st.executeUpdate("DELETE FROM task");
                st.executeUpdate("DELETE FROM collaborator");
                st.executeUpdate("DELETE FROM project");
            }
            insertAllProjects(projects.getAllProjects());
            IdentityHashMap<Collaborator, Long> collabIds = insertAllCollaborators(projects.getAllProjects());

            for (Task t : tasks.getAllTasks()) {
                try (PreparedStatement ps = this.conn.prepareStatement(
                        "INSERT INTO task (id, title, description, priority, status, due_date, project_name, "
                                + "creation_time) VALUES (?,?,?,?,?,?,?,?)")) {
                    ps.setInt(1, t.getId());
                    ps.setString(2, t.getTitle());
                    ps.setString(3, t.getDescription());
                    ps.setString(4, t.getPriorityLevel());
                    ps.setString(5, t.getStatus().name());
                    ps.setString(6, t.getDueDate() == null ? null : t.getDueDate().toString());
                    Project p = t.getProject();
                    ps.setString(7, p == null ? null : p.getName());
                    ps.setString(8, t.getCreationDate().toString());
                    ps.executeUpdate();
                }
            }

            for (Task t : tasks.getAllTasks()) {
                int order = 0;
                for (SubTask s : t.getSubtasks()) {
                    Long collabRowId = null;
                    if (s instanceof CollaboratorSubtask) {
                        collabRowId = collabIds.get(((CollaboratorSubtask) s).getCollaborator());
                    }
                    try (PreparedStatement ps = this.conn.prepareStatement(
                            "INSERT INTO subtask (task_id, sort_order, kind, title, status, collaborator_id) "
                                    + "VALUES (?,?,?,?,?,?)")) {
                        ps.setInt(1, t.getId());
                        ps.setInt(2, order++);
                        ps.setString(3, s instanceof CollaboratorSubtask ? "C" : "G");
                        ps.setString(4, s.getTitle());
                        ps.setString(5, s.getStatus().name());
                        if (collabRowId == null) {
                            ps.setObject(6, null);
                        } else {
                            ps.setLong(6, collabRowId);
                        }
                        ps.executeUpdate();
                    }
                }
                for (Tag tag : t.getTags()) {
                    try (PreparedStatement ps = this.conn.prepareStatement(
                            "INSERT INTO task_tag (task_id, tag) VALUES (?,?)")) {
                        ps.setInt(1, t.getId());
                        ps.setString(2, tag.getKeyword());
                        ps.executeUpdate();
                    }
                }
            }

            this.conn.commit();
        } catch (SQLException e) {
            try {
                this.conn.rollback();
            } catch (SQLException ignored) {
                // ignore
            }
            throw new IOException("Failed to save database", e);
        } finally {
            try {
                this.conn.setAutoCommit(true);
            } catch (SQLException ignored) {
                // ignore
            }
        }
    }

//---------------------------------PROJECT SQL---------------------------------

    private void loadProjectsInto(Projects projects) throws SQLException {
        List<Project> loaded = new ArrayList<>();
        try (Statement st = this.conn.createStatement();
                ResultSet rs = st.executeQuery("SELECT name, description FROM project ORDER BY name")) {
            while (rs.next()) {
                loaded.add(new Project(rs.getString("name"), rs.getString("description")));
            }
        }
        projects.replaceState(loaded);
    }

    private void insertAllProjects(Iterable<Project> projects) throws SQLException {
        for (Project p : projects) {
            try (PreparedStatement ps = this.conn.prepareStatement(
                    "INSERT INTO project (name, description) VALUES (?,?)")) {
                ps.setString(1, p.getName());
                ps.setString(2, p.getDescription() == null ? "" : p.getDescription());
                ps.executeUpdate();
            }
        }
    }

//---------------------------------COLLABORATOR SQL---------------------------------

    private Map<Long, Collaborator> loadCollaboratorsInto(Projects projects) throws SQLException {
        Map<Long, Collaborator> byId = new HashMap<>();
        try (Statement st = this.conn.createStatement();
                ResultSet rs = st.executeQuery(
                        "SELECT id, project_name, name, category FROM collaborator ORDER BY id")) {
            while (rs.next()) {
                long id = rs.getLong("id");
                Project p = projects.findByName(rs.getString("project_name"));
                if (p == null) {
                    continue;
                }
                CollaboratorCategory cat = CollaboratorCategory.valueOf(rs.getString("category"));
                Collaborator col = new Collaborator(p, rs.getString("name"), cat);
                p.addCollaborator(col);
                byId.put(id, col);
            }
        }
        return byId;
    }

    private IdentityHashMap<Collaborator, Long> insertAllCollaborators(Iterable<Project> projects) throws SQLException {
        IdentityHashMap<Collaborator, Long> ids = new IdentityHashMap<>();
        for (Project p : projects) {
            for (Collaborator col : p.getCollaborators()) {
                try (PreparedStatement ps = this.conn.prepareStatement(
                        "INSERT INTO collaborator (project_name, name, category) VALUES (?,?,?)",
                        Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, p.getName());
                    ps.setString(2, col.getName());
                    ps.setString(3, col.getCategory().name());
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        if (keys.next()) {
                            ids.put(col, keys.getLong(1));
                        }
                    }
                }
            }
        }
        return ids;
    }
}
