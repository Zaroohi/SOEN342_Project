package database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import model.Project;
import service.Projects;

public final class ProjectDAO {

    private final Connection conn;

    public ProjectDAO(Connection conn) {
        this.conn = conn;
    }

    public void deleteAll() throws SQLException {
        try (Statement st = this.conn.createStatement()) {
            st.executeUpdate("DELETE FROM project");
        }
    }

    public void insertAll(Iterable<Project> projects) throws SQLException {
        for (Project p : projects) {
            try (PreparedStatement ps = this.conn.prepareStatement(
                    "INSERT INTO project (name, description) VALUES (?,?)")) {
                ps.setString(1, p.getName());
                ps.setString(2, p.getDescription() == null ? "" : p.getDescription());
                ps.executeUpdate();
            }
        }
    }

    public List<Project> loadAllProjects() throws SQLException {
        List<Project> list = new ArrayList<>();
        try (Statement st = this.conn.createStatement();
                ResultSet rs = st.executeQuery("SELECT name, description FROM project ORDER BY name")) {
            while (rs.next()) {
                list.add(new Project(rs.getString("name"), rs.getString("description")));
            }
        }
        return list;
    }

    /** Loads projects into the given {@link Projects} registry (replaces current list). */
    public void loadInto(Projects projects) throws SQLException {
        projects.replaceState(loadAllProjects());
    }
}
