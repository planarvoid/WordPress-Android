package com.soundcloud.android.utils;

import javax.inject.Inject;
import java.util.Date;

public class CurrentDateProvider implements DateProvider {

    @Inject
    public CurrentDateProvider() {
    }

    @Override
    public Date getDate(){
        return new Date();
    }

    @Override
    public long getTime() {
        return System.currentTimeMillis();
    }
}
