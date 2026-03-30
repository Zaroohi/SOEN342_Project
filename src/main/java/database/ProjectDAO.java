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

//SQL for the project table (names + descriptions)
public final class ProjectDAO {

    private final Connection conn;

//---------------------------------CONSTRUCTORS---------------------------------

    public ProjectDAO(Connection conn) {
        this.conn = conn;
    }

//---------------------------------WRITE---------------------------------

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

//---------------------------------READ---------------------------------

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

    //replaces the in-memory Projects registry from the project table
    public void loadInto(Projects projects) throws SQLException {
        projects.replaceState(loadAllProjects());
    }
}
