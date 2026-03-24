package model;


public class Project {

    private String name;
    private String description;

    //constr.
    public Project(String name, String description) {
        this.name = name;
        this.description = description;
    }

    //getter name
    public String getName() {
        return this.name;
    }

    //setter name
    public void setName(String name) {
        this.name = name;
    }

    //getter description
    public String getDescription() {
        return this.description;
    }

    //setter description
    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public String toString() {
        return "Project{name='" + this.name + "', description='" + this.description + "'}";
    }
}