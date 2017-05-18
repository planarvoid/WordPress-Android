package com.soundcloud.android.discovery;

import com.squareup.sqldelight.ColumnAdapter;

import android.support.annotation.NonNull;

import java.util.Date;

class DateAdapter implements ColumnAdapter<Date, Long> {

    @NonNull
    @Override
    public Date decode(Long timestamp) {
        return new Date(timestamp);
    }

    @Override
    public Long encode(@NonNull Date date) {
        return date.getTime();
    }
}
