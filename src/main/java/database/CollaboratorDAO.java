package database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import model.Collaborator;
import model.CollaboratorCategory;
import model.Project;
import service.Projects;

//SQL for the collaborator table (ties to project name; row ids used by collaborator subtasks)
public final class CollaboratorDAO {

    private final Connection conn;

//---------------------------------CONSTRUCTORS---------------------------------

    public CollaboratorDAO(Connection conn) {
        this.conn = conn;
    }

//---------------------------------WRITE---------------------------------

    public void deleteAll() throws SQLException {
        try (Statement st = this.conn.createStatement()) {
            st.executeUpdate("DELETE FROM collaborator");
        }
    }

    //inserts every collaborator on every project; returns JDBC row id per Collaborator instance for subtask FKs
    public IdentityHashMap<Collaborator, Long> insertAll(Iterable<Project> projects) throws SQLException {
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

//---------------------------------READ---------------------------------

    //attaches Collaborator objects to projects already in memory; returns id -> collaborator for loading subtasks
    public Map<Long, Collaborator> loadInto(Projects projects) throws SQLException {
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
}
