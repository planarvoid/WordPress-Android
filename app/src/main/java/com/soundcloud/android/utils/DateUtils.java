package com.soundcloud.android.utils;

import com.soundcloud.java.optional.Optional;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class DateUtils {
    public static int yearFromDateString(String date, String format) throws ParseException {
        SimpleDateFormat parser = new SimpleDateFormat(format, Locale.US);
        Date parsedDate = parser.parse(date);
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(parsedDate);
        return calendar.get(Calendar.YEAR);
    }

    public static boolean isInLastDays(Optional<Date> date, int delta) {
        return isInRange(date, delta, Calendar.DATE);
    }

    public static boolean isInLastHours(Optional<Date> date, int delta) {
        return isInRange(date, delta, Calendar.HOUR_OF_DAY);
    }

    private static boolean isInRange(Optional<Date> date, int delta, int timeUnit) {
        if (!date.isPresent()) {
            return false;
        }

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date.get());
        calendar.set(timeUnit, calendar.get(timeUnit) + delta);
        return Calendar.getInstance().before(calendar);
    }
}
