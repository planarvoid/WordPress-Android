package com.soundcloud.android.profile;

import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.Calendar;

public class BirthdayInfo implements Serializable {

    private static final long serialVersionUID = 3051966333050380486L;
    private static final int VALID_AGE = 13;
    private static final Calendar calendar = Calendar.getInstance();

    public final int age;

    @Nullable
    public static BirthdayInfo buildFrom(int age) {
        return new BirthdayInfo(age);
    }

    private BirthdayInfo(int age) {
        this.age = age;
    }

    public int getMonth() {
        return calendar.get(Calendar.MONTH) + 1;
    }

    public int getYear() {
        return calendar.get(Calendar.YEAR) - age;
    }

    public boolean isValid() {
        return age >= VALID_AGE;
    }
}
