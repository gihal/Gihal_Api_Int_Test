package common.datetime;

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
}
