package com.soundcloud.android.utils;

import javax.inject.Inject;
import java.util.Date;

public class CurrentDateProvider implements DateProvider {

    @Inject
    public CurrentDateProvider() {
    }

    @Override
    public Date getCurrentDate(){
        return new Date();
    }

    @Override
    public long getCurrentTime() {
        return System.currentTimeMillis();
    }
}
