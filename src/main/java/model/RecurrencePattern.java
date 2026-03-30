package model;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public class RecurrencePattern {

    private RecurrenceFrequency frequency;
    private int interval;
    private LocalDate startDate;
    private LocalDate endDate;
    private Set<DayOfWeek> weekDays;
    private int dayOfMonth;

//---------------------------------CONSTRUCTORS---------------------------------

    public RecurrencePattern(RecurrenceFrequency frequency, int interval,
            LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Start and end date are required.");
        }
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("End date cannot be before start date.");
        }
        if (interval < 1) {
            throw new IllegalArgumentException("Interval must be at least 1.");
        }
        this.frequency = requireRecurrenceFrequency(frequency);
        this.interval = interval;
        this.startDate = startDate;
        this.endDate = endDate;
        this.weekDays = EnumSet.noneOf(DayOfWeek.class);
        this.dayOfMonth = startDate.getDayOfMonth();
    }

    //helper method to check if the frequency is not null and is a valid frequency from the enum
    private static RecurrenceFrequency requireRecurrenceFrequency(RecurrenceFrequency frequency) {
        if (frequency == null) {
            throw new IllegalArgumentException(
                    "Frequency must be a RecurrenceFrequency: " + Arrays.toString(RecurrenceFrequency.values()));
        }
        return frequency;
    }

//---------------------------------GETTERS AND SETTERS---------------------------------

    public RecurrenceFrequency getFrequency() {
        return this.frequency;
    }

    public void setFrequency(RecurrenceFrequency frequency) {
        this.frequency = requireRecurrenceFrequency(frequency);
    }

    public int getInterval() {
        return this.interval;
    }

    public void setInterval(int interval) {
        if (interval < 1) {
            throw new IllegalArgumentException("Interval must be at least 1.");
        }
        this.interval = interval;
    }

    public LocalDate getStartDate() {
        return this.startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return this.endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public Set<DayOfWeek> getWeekDays() {
        return Collections.unmodifiableSet(this.weekDays);
    }

    public void setWeekDays(Set<DayOfWeek> days) {
        this.weekDays = days == null ? EnumSet.noneOf(DayOfWeek.class) : EnumSet.copyOf(days);
    }

    public int getDayOfMonth() {
        return this.dayOfMonth;
    }

    public void setDayOfMonth(int dayOfMonth) {
        if (dayOfMonth < 1 || dayOfMonth > 31) {
            throw new IllegalArgumentException("Day of month must be 1..31.");
        }
        this.dayOfMonth = dayOfMonth;
    }

//---------------------------------UTILITY METHODS---------------------------------

    //generates the occurrences of the recurrence pattern
    public List<LocalDate> generateOccurrences() {
        List<LocalDate> dates = new ArrayList<>();
        if (this.startDate == null || this.endDate == null) {
            return dates;
        }
        if (this.frequency == null) {
            throw new IllegalStateException("Frequency is required.");
        }
        switch (this.frequency) {
            case DAILY:
                for (LocalDate d = this.startDate; !d.isAfter(this.endDate); d = d.plusDays(this.interval)) {
                    dates.add(d);
                }
                break;
            case WEEKLY:
                if (this.weekDays == null || this.weekDays.isEmpty()) {
                    for (LocalDate d = this.startDate; !d.isAfter(this.endDate); d = d.plusWeeks(this.interval)) {
                        dates.add(d);
                    }
                } else {
                    LocalDate d = this.startDate;
                    while (!d.isAfter(this.endDate)) {
                        if (this.weekDays.contains(d.getDayOfWeek())) {
                            dates.add(d);
                        }
                        d = d.plusDays(1);
                    }
                }
                break;
            case MONTHLY:
                LocalDate monthCursor = LocalDate.of(this.startDate.getYear(), this.startDate.getMonth(), 1);
                while (true) {
                    LocalDate occ = clampDayOfMonth(monthCursor, this.dayOfMonth);
                    if (occ.isAfter(this.endDate)) {
                        break;
                    }
                    if (!occ.isBefore(this.startDate)) {
                        dates.add(occ);
                    }
                    monthCursor = monthCursor.plusMonths(this.interval);
                    if (monthCursor.getYear() > this.endDate.getYear() + 1) {
                        break;
                    }
                }
                break;
            default:
                throw new IllegalStateException("Unhandled RecurrenceFrequency: " + this.frequency
                        + " (expected one of " + Arrays.toString(RecurrenceFrequency.values()) + ")");
        }
        return dates;
    }

    //finds the next occurrence after a given date
    public LocalDate nextOccurrenceAfter(LocalDate fromExclusive) {
        List<LocalDate> all = generateOccurrences();
        LocalDate candidate = null;
        for (LocalDate d : all) {
            if (fromExclusive == null || d.isAfter(fromExclusive)) {
                if (candidate == null || d.isBefore(candidate)) {
                    candidate = d;
                }
            }
        }
        return candidate;
    }

    //helper method to clamp the day of month to the last day of the month
    private static LocalDate clampDayOfMonth(LocalDate monthAnchor, int day) {
        int last = monthAnchor.lengthOfMonth();
        int use = Math.min(day, last);
        return monthAnchor.withDayOfMonth(use);
    }
}
