package com.soundcloud.android.utils;

import javax.inject.Inject;
import java.util.Date;

public class DateProvider {

    @Inject
    public DateProvider() {
    }

    public Date getCurrentDate(){
        return new Date();
    }
}
