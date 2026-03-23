package model;

public enum CollaboratorCategory {
	SENIOR(2), INTERMEDIATE(5), JUNIOR(10);
	
	private final int maxOpenTasks; 
	
	 CollaboratorCategory(int maxOpenTasks) {
	        this.maxOpenTasks = maxOpenTasks;
	    }

	    //getter
	    public int getMaxOpenTasks() {
	        return this.maxOpenTasks;
	    }
	}

