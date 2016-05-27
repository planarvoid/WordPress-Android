package com.soundcloud.android.utils;

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
}
