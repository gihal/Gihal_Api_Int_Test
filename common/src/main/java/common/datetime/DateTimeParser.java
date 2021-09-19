package common.datetime;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DateTimeParser {

    public static Date parseDate(String value) {
        if (value == null
                || value.isEmpty()
                || !value.startsWith("/Date")) {
            return null;
        }
        value = value.substring(value.indexOf("(") + 1, value.indexOf(")"));
        return new Date(Long.parseLong(value));
    }

    public static String formatDate(Date date) {
        if (date == null) {
            return null;
        }
        DateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return format.format(date);
    }
}
