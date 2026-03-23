Use case: Create Task
Level: User goal
Primary actor: User 
Stakeholders: 
- User: wants to create a task with details. Have it stored and searchable. 

Preconditions:
- The system is fully running and is ready to accept commands. 
Trigger: 
- The user wants to create a new task. 

Success Scenario: 
1. User initiates "Create Task".
2. System requests the task details: title, description, priority,due date, project, tags (due date, project, tags and description are optional).
3. User provides the task details and submits.
4. System validates that title is provided. System validates that priority is provided.
5. System creates a new task.
6. System sets Task.creationDate to the current date/time.
7. System sets Task.status to open.
8. System associates the task with the provided project.
9. System associates the task with the provided tags.
10. System records a task activity entry: "task created" (with a timestamp).
11. System confirms creation and returns the new taskId.

Postcondition:
- New taks exists in the system with its stores details.
- Task.status is open. 
- The new task is associated with the selected project and tags (if these two are provided). 
- A new Activiy Entry exists for this task. 

Extensions (failure): 
(happens at step 4)
4a. Missing or empty title.
4a1. System detects the title is empty or missing.
4a2. System displays an error message: "Title is needed!".
4a3. System does not create a new task.
4a4. End of use case (fails).

Failure posconditions: 
- No task is created. 
- No Activity Entry is recorded. 





