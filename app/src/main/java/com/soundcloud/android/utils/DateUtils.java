package com.soundcloud.android.utils;

import com.soundcloud.java.optional.Optional;

import android.support.annotation.VisibleForTesting;

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
        return isInLast(date, delta, Calendar.DATE);
    }

    public static boolean isInLastHours(Optional<Date> date, int delta) {
        return isInLast(date, delta, Calendar.HOUR_OF_DAY);
    }

    @VisibleForTesting
    static boolean isInLast(Optional<Date> date, int delta, int timeUnit) {
        if (!date.isPresent()) {
            return false;
        }

        Calendar toCheck = Calendar.getInstance();
        toCheck.setTime(date.get());

        Calendar earliest = Calendar.getInstance();
        earliest.set(timeUnit, earliest.get(timeUnit) - delta);
        return earliest.before(toCheck);
    }
}
