package com.soundcloud.android.api;

import com.fasterxml.jackson.databind.util.StdDateFormat;

import java.text.DateFormat;
import java.text.FieldPosition;
import java.text.ParseException;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class ApiDateFormat extends StdDateFormat {
    // SimpleDateFormat & co are not threadsafe - use thread local instance for static access
    private static ThreadLocal<ApiDateFormat> threadLocal = new ThreadLocal<>();
    /**
     * Used by the SoundCloud API
     */
    private final DateFormat dateFormat;

    public ApiDateFormat() {
        dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss Z", Locale.US);
        dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
    }

    private static ApiDateFormat instance() {
        ApiDateFormat fmt = threadLocal.get();
        if (fmt == null) {
            fmt = new ApiDateFormat();
            threadLocal.set(fmt);
        }
        return fmt;
    }

    public static String formatDate(long tstamp) {
        return instance().format(tstamp);
    }

    @SuppressWarnings({"CloneDoesntCallSuperClone"})
    @Override
    public ApiDateFormat clone() {
        return instance();
    }

    @Override
    public Date parse(String dateStr) throws ParseException {
        final Date d = dateFormat.parse(dateStr);
        return (d == null) ? super.parse(dateStr) : d;
    }

    @Override
    public Date parse(String dateStr, ParsePosition pos) {
        final Date d = dateFormat.parse(dateStr, pos);
        return (d == null) ? super.parse(dateStr, pos) : d;
    }

    @Override
    public StringBuffer format(Date date, StringBuffer toAppendTo, FieldPosition fieldPosition) {
        return dateFormat.format(date, toAppendTo, fieldPosition);
    }
}
