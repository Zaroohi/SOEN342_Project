# SOEN342_Project
Software Requirements and Deployment (Concordia University, Winter 2026)
Team repository for an OO software system.


Team leader: Zaruhi Grigoryan

Team members: 
- Zaruhi Grigoryan - 40299515 - Zaroohi
- Jakson Rabinovitch - 40285726 - Jaksonrab
- Hala Abdulsamad - 40296853 - hala842
## Link to our demo video 
https://www.youtube.com/watch?v=bT7qi8riV2I
## Running the application

### Prerequisites
- Java 17+ installed
- Internet connection on first run (Maven Wrapper downloads Maven/dependencies)

### Run from project root

Recommended:

```sh
cd /Users/jakson/SOEN342_Project-1
./run.sh
```

Direct Maven Wrapper command (same result):

```sh
cd /Users/jakson/SOEN342_Project-1
./mvnw -q compile exec:java
```

### What starts
- Entry point: `src/main/java/Main.java`
- Console UI: `ui.Console`

### SQLite database location
- File: `data/taskmanager.db`
- The app creates the file/tables automatically if they do not exist.

### Optional: open DB in DB Browser for SQLite (macOS)

```sh
open -a "DB Browser for SQLite" /Users/jakson/SOEN342_Project-1/data/taskmanager.db
```
