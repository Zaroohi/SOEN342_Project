package model;

public enum CollaboratorCategory {
	SENIOR(2), INTERMEDIATE(5), JUNIOR(10);
	
	private final int maxOpenTasks; 
	
	 CollaboratorCategory(int maxOpenTasks) {
            if (maxOpenTasks <= 0) {
                throw new IllegalArgumentException("Collaborator open-task limit must be a positive integer.");
            }
	        this.maxOpenTasks = maxOpenTasks;
	    }

	    //getter
	    public int getMaxOpenTasks() {
	        return this.maxOpenTasks;
	    }
	}

