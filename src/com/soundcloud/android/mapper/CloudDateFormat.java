package com.soundcloud.android.mapper;

import org.codehaus.jackson.map.util.StdDateFormat;

import java.text.DateFormat;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Date;

public class CloudDateFormat extends StdDateFormat {
    public static final DateFormat DATEFMT = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss Z");
    public static final DateFormat INSTANCE = new CloudDateFormat();

    @Override
    public StdDateFormat clone() {
        return new CloudDateFormat();
    }

    @Override
    public Date parse(String dateStr, ParsePosition pos) {
        final Date d = DATEFMT.parse(dateStr, pos);
        return (d == null) ? super.parse(dateStr, pos) : d;
    }
}
