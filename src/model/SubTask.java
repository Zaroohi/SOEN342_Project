package model;

public abstract class SubTask {

    private String title;
    private SubTaskStatus status;
    private Task parentTask;

    protected SubTask(String title) {
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Subtask title cannot be empty.");
        }
        this.title = title.trim();
        this.status = SubTaskStatus.OPEN;
    }

    public String getTitle() {
        return this.title;
    }

    public void setTitle(String title) {
        if (title == null || title.trim().isEmpty()) {
            throw new IllegalArgumentException("Subtask title cannot be empty.");
        }
        this.title = title.trim();
    }

    public SubTaskStatus getStatus() {
        return this.status;
    }

    public void setStatus(SubTaskStatus status) {
        this.status = status;
    }

    public Task getParentTask() {
        return this.parentTask;
    }

    void bindParentTask(Task parent) {
        this.parentTask = parent;
    }

    public boolean isOpen() {
        return this.status == SubTaskStatus.OPEN;
    }

    public void complete() {
        this.status = SubTaskStatus.COMPLETED;
    }

    public void cancel() {
        this.status = SubTaskStatus.CANCELLED;
    }

    public void reopen() {
        if (this.status != SubTaskStatus.OPEN) {
            this.status = SubTaskStatus.OPEN;
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{title='" + this.title + "', status=" + this.status + "}";
    }
}
