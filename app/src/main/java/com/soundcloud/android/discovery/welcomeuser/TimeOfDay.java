package com.soundcloud.android.discovery.welcomeuser;

import java.util.Calendar;

public enum TimeOfDay {
    MORNING, AFTERNOON, EVENING, NIGHT;

    private static final int AFTERNOON_START = 12;
    private static final int MORNING_START = 5;
    private static final int EVENING_START = 17;
    private static final int NIGHT_START = 21;

    public static TimeOfDay getCurrent() {
        Calendar calendar = Calendar.getInstance();
        int hourOfDay = calendar.get(Calendar.HOUR_OF_DAY);

        if (isMorningHours(hourOfDay)) {
            return MORNING;
        } else if (isAfternoonHours(hourOfDay)) {
            return AFTERNOON;
        } else if (isEveningHours(hourOfDay)) {
            return EVENING;
        } else {
            return NIGHT;
        }
    }

    private static boolean isMorningHours(int hourOfDay) {
        return hourOfDay >= MORNING_START && hourOfDay < AFTERNOON_START;
    }

    private static boolean isAfternoonHours(int hourOfDay) {
        return hourOfDay >= AFTERNOON_START && hourOfDay < EVENING_START;
    }

    private static boolean isEveningHours(int hourOfDay) {
        return hourOfDay >= EVENING_START && hourOfDay < NIGHT_START;
    }
}
