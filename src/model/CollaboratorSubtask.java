package model;

public class CollaboratorSubtask extends SubTask {

    private Collaborator assignedTo;

    public CollaboratorSubtask(String title, Collaborator assignedTo) {
        super(title);
        if (assignedTo == null) {
            throw new IllegalArgumentException("Collaborator subtask requires a collaborator.");
        }
        this.assignedTo = assignedTo;
    }

    public Collaborator getCollaborator() {
        return this.assignedTo;
    }

    public void setCollaborator(Collaborator assignedTo) {
        if (assignedTo == null) {
            throw new IllegalArgumentException("Collaborator cannot be null.");
        }
        this.assignedTo = assignedTo;
    }

    @Override
    public String toString() {
        return "CollaboratorSubtask{title='" + getTitle() + "', status=" + getStatus()
                + ", assignedTo=" + this.assignedTo + "}";
    }
}
