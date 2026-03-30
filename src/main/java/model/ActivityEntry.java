package model;

import java.time.LocalDateTime;

public class ActivityEntry {

    private final ActionType type;
    private final String description;
    private final LocalDateTime at;

//---------------------------------CONSTRUCTORS---------------------------------

    public ActivityEntry(ActionType type, String description) {
        this.type = type;
        this.description = description == null ? "" : description;
        this.at = LocalDateTime.now();
    }

//---------------------------------GETTERS AND SETTERS---------------------------------

    public ActionType getType() {
        return this.type;
    }

    public String getDescription() {
        return this.description;
    }

    public LocalDateTime getAt() {
        return this.at;
    }
}
