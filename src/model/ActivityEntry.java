package model;

import java.time.LocalDateTime;

public class ActivityEntry {

    private final LocalDateTime timestamp;
    private final String actionDescription;
    private final ActionType actionType;

    public ActivityEntry(ActionType actionType, String actionDescription) {
        this.timestamp = LocalDateTime.now();
        this.actionType = actionType;
        this.actionDescription = actionDescription;
    }

    public LocalDateTime getTimestamp() {
        return this.timestamp;
    }

    public String getActionDescription() {
        return this.actionDescription;
    }

    public ActionType getActionType() {
        return this.actionType;
    }

    @Override
    public String toString() {
        return this.timestamp + " [" + this.actionType + "] " + this.actionDescription;
    }
}
