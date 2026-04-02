package service;

import java.io.FileOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import model.Project;
import model.Task;
import model.TaskStatus;
import net.fortuna.ical4j.data.CalendarOutputter;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Date;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.property.CalScale;
import net.fortuna.ical4j.model.property.Description;
import net.fortuna.ical4j.model.property.DtStamp;
import net.fortuna.ical4j.model.property.Priority;
import net.fortuna.ical4j.model.property.ProdId;
import net.fortuna.ical4j.model.property.Status;
import net.fortuna.ical4j.model.property.Uid;
import net.fortuna.ical4j.model.property.Version;
import net.fortuna.ical4j.validate.ValidationException;

public final class IcsCalendarExporter {

    private static final DateTimeFormatter ICS_DAY = DateTimeFormatter.BASIC_ISO_DATE;
    private final IcalGateway ical;

//---------------------------------CONSTRUCTORS---------------------------------

    public IcsCalendarExporter() {
        this.ical = new Ical4jGateway();
    }

//---------------------------------EXPORT---------------------------------

    public int exportToIcs(String filePath, List<Task> tasks) throws IOException {
        return this.ical.writeCalendar(ensureIcsFilePath(filePath), tasks);
    }

    public static String ensureIcsFilePath(String filePath) throws IOException {
        if (filePath == null) {
            throw new IOException("File path is required.");
        }
        String p = filePath.trim();
        if (p.isEmpty()) {
            throw new IOException("File path is required.");
        }
        if (!p.toLowerCase().endsWith(".ics")) {
            p = p + ".ics";
        }
        return p;
    }

    // Gateway contract for calendar serialization.
    private interface IcalGateway {
        int writeCalendar(String filePath, List<Task> tasks) throws IOException;
    }

    // Concrete gateway backed by iCal4j.
    private static final class Ical4jGateway implements IcalGateway {

        @Override
        //writes the calendar to the file
        public int writeCalendar(String filePath, List<Task> tasks) throws IOException {
            Calendar calendar = new Calendar();
            calendar.getProperties().add(new ProdId("-//SOEN342//TaskManager//EN"));
            calendar.getProperties().add(CalScale.GREGORIAN);
            calendar.getProperties().add(Version.VERSION_2_0);

            int exported = 0;
            if (tasks != null) {
                for (Task task : tasks) {
                    if (task == null || task.getDueDate() == null) {
                        continue;
                    }
                    try {
                        calendar.getComponents().add(toVEvent(task));
                    } catch (ValidationException e) {
                        throw new IOException(e.getMessage(), e);
                    }
                    exported++;
                }
            }

            try (FileOutputStream out = new FileOutputStream(filePath)) {
                new CalendarOutputter(false).output(calendar, out);
            } catch (ValidationException e) {
                throw new IOException(e.getMessage(), e);
            }
            return exported;
        }

        // converts one task to a calendar event
        private static VEvent toVEvent(Task task) throws IOException {
            LocalDate due = task.getDueDate();
            try {
                Date start = new Date(due.format(ICS_DAY));
                Date end = new Date(due.plusDays(1).format(ICS_DAY));
                VEvent event = new VEvent(start, end, task.getTitle());
                // VEvent with ical4j Date start/end is already date-only; do not add
                // VALUE=DATE again or clients see DTSTART;VALUE=DATE;VALUE=DATE (invalid).

                String uid = task.getId() + "-" + start + "-" + UUID.randomUUID() + "@soen342.local";
                event.getProperties().add(new Uid(uid));
                if (event.getDateStamp() == null) {
                    event.getProperties().add(new DtStamp());
                }
                event.getProperties().add(new Description(descriptionFor(task)));
                event.getProperties().add(new Priority(priorityLevel(task.getPriorityLevel())));
                event.getProperties().add(veventStatus(task.getStatus()));
                return event;
            } catch (ParseException e) {
                throw new IOException(e.getMessage(), e);
            }
        }

        // creates a description for the task including the description, status, priority, project, and subtask summary
        private static String descriptionFor(Task task) {
            StringBuilder sb = new StringBuilder();
            sb.append("Description: ").append(task.getDescription() == null ? "" : task.getDescription());
            sb.append("\nStatus: ").append(task.getStatus());
            sb.append("\nPriority: ").append(task.getPriorityLevel() == null ? "" : task.getPriorityLevel());
            Project p = task.getProject();
            sb.append("\nProject: ").append(p == null ? "" : p.getName());
            String subs = task.subtasksSummaryForCsv();
            if (subs != null && !subs.isEmpty()) {
                sb.append("\nSubtasks: ").append(subs);
            }
            return sb.toString();
        }

        // converts the priority level to a numeric level
        private static int priorityLevel(String priorityLevel) {
            if (priorityLevel == null || priorityLevel.trim().isEmpty()) {
                return Priority.UNDEFINED.getLevel();
            }
            String p = priorityLevel.trim().toLowerCase();
            if (p.contains("high") || p.equals("1")) {
                return Priority.HIGH.getLevel();
            }
            if (p.contains("medium") || p.equals("5")) {
                return Priority.MEDIUM.getLevel();
            }
            if (p.contains("low") || p.equals("9")) {
                return Priority.LOW.getLevel();
            }
            return Priority.UNDEFINED.getLevel();
        }

        // converts the task status to a calendar event status
        private static Status veventStatus(TaskStatus status) {
            if (status == TaskStatus.COMPLETED) {
                return Status.VEVENT_CONFIRMED;
            }
            if (status == TaskStatus.CANCELLED) {
                return Status.VEVENT_CANCELLED;
            }
            return Status.VEVENT_TENTATIVE;
        }
    }
}
