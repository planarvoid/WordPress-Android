package com.soundcloud.android.profile;

import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.Calendar;

public class BirthdayInfo implements Serializable {

    private static final long serialVersionUID = 3051966333050380486L;

    public final int month;
    public final int year;

    @Nullable
    public static BirthdayInfo buildFrom(int month, int year) {
        if (validMonthAndYear(month, year)) {
            return new BirthdayInfo(month, year);
        } else {
            return null;
        }
    }

    private BirthdayInfo(int month, int year) {
        this.month = month;
        this.year = year;
    }

    private static boolean validMonthAndYear(int month, int year) {
        Calendar cal = Calendar.getInstance();
        int currentYear = cal.get(Calendar.YEAR);
        return month > 0 && month <= 12 && year >= (currentYear - 100) && year < (currentYear - 13);
    }
}
