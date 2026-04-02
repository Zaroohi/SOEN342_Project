package database;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

//opens SQLite (default data/taskmanager.db) and ensures tables exist before each connection is returned
public final class DatabaseConnection {

    public static final String DEFAULT_RELATIVE_PATH = "data/taskmanager.db";

    private final String jdbcUrl;

//---------------------------------CONSTRUCTORS---------------------------------

    public DatabaseConnection() {
        this(DEFAULT_RELATIVE_PATH);
    }

    public DatabaseConnection(String relativePath) {
        File f = new File(relativePath);
        File parent = f.getParentFile();
        if (parent != null) {
            parent.mkdirs();
        }
        this.jdbcUrl = "jdbc:sqlite:" + f.getAbsolutePath();
    }

//---------------------------------CONNECTION---------------------------------

    public Connection getConnection() throws SQLException {
        Connection c = DriverManager.getConnection(this.jdbcUrl);
        try (Statement st = c.createStatement()) {
            st.executeUpdate("PRAGMA foreign_keys = ON");
        }
        ensureSchema(c);
        return c;
    }

//---------------------------------SCHEMA---------------------------------

    static void ensureSchema(Connection c) throws SQLException {
        try (Statement st = c.createStatement()) {
            st.executeUpdate("CREATE TABLE IF NOT EXISTS project ("
                    + "name TEXT PRIMARY KEY COLLATE NOCASE NOT NULL, "
                    + "description TEXT NOT NULL DEFAULT '')");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS collaborator ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "project_name TEXT NOT NULL REFERENCES project(name), "
                    + "name TEXT NOT NULL, "
                    + "category TEXT NOT NULL, "
                    + "UNIQUE(project_name, name))");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS task ("
                    + "id INTEGER PRIMARY KEY, "
                    + "title TEXT NOT NULL, "
                    + "description TEXT NOT NULL, "
                    + "priority TEXT NOT NULL, "
                    + "status TEXT NOT NULL, "
                    + "due_date TEXT, "
                    + "project_name TEXT REFERENCES project(name), "
                    + "creation_time TEXT NOT NULL)");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS subtask ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "task_id INTEGER NOT NULL REFERENCES task(id) ON DELETE CASCADE, "
                    + "sort_order INTEGER NOT NULL, "
                    + "kind TEXT NOT NULL, "
                    + "title TEXT NOT NULL, "
                    + "status TEXT NOT NULL, "
                    + "collaborator_id INTEGER REFERENCES collaborator(id))");
            st.executeUpdate("CREATE TABLE IF NOT EXISTS task_tag ("
                    + "task_id INTEGER NOT NULL REFERENCES task(id) ON DELETE CASCADE, "
                    + "tag TEXT NOT NULL)");
        }
    }
}
