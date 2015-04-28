package com.soundcloud.android.profile;

import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.Calendar;

public class BirthdayInfo implements Serializable {

    private static final long serialVersionUID = 3051966333050380486L;

    public final int age;

    @Nullable
    public static BirthdayInfo buildFrom(int age) {
        return new BirthdayInfo(age);
    }

    private BirthdayInfo(int age) {
        this.age = age;
    }

    public int getMonth(){
        return Calendar.getInstance().get(Calendar.MONTH);
    }

    public int getYear(){
        return Calendar.getInstance().get(Calendar.YEAR) - age;
    }

    public boolean isValid(){
        return age >= 13;
    }
}
