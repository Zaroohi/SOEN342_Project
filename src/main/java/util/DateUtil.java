package util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;


public class DateUtil {

    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    //parsing string to LocalDate
    public static LocalDate parseDate(String dateText) {
        if (dateText == null || dateText.trim().isEmpty()) {
            return null;
        }

        try {
            return LocalDate.parse(dateText, formatter);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Date format is invalid. Please use yyyy-MM-dd.");
        }
    }
    //formatting LocalDate to string
    public static String formatDate(LocalDate date) {
        if (date == null) {
            return "";
        }
        return date.format(formatter);
    }
}
