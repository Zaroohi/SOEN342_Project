package model;


public class Collaborator {

    private String name;
    private CollaboratorCategory category;
    private int openTaskCount;

    //constr
    public Collaborator(String name, CollaboratorCategory category) {
        this.name = name;
        this.category = category;
        this.openTaskCount = 0;
    }

    //getter name
    public String getName() {
        return this.name;
    }

    //sette name
    public void setName(String name) {
        this.name = name;
    }

    //getter category
    public CollaboratorCategory getCategory() {
        return this.category;
    }

    //setter category
    public void setCategory(CollaboratorCategory category) {
        this.category = category;
    }

    //getter open task count
    public int getOpenTaskCount() {
        return this.openTaskCount;
    }

    //increasing count
    public void incrementOpenTaskCount() {
        this.openTaskCount++;
    }

    //decreasing count
    public void decrementOpenTaskCount() {
        if (this.openTaskCount > 0) {
            this.openTaskCount--;
        }
    }

    //check if collaborator can take more tasks
    public boolean canAcceptMoreTasks() {
        return this.openTaskCount < this.category.getMaxOpenTasks();
    }

    @Override
    public String toString() {
        return "Collaborator{name='" + this.name + "', category=" + this.category +
               ", openTaskCount=" + this.openTaskCount + "}";
    }
}