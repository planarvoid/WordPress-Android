package com.soundcloud.android.activity.settings;

import android.annotation.SuppressLint;
import android.content.Context;
import android.preference.SwitchPreference;

@SuppressWarnings("UnusedDeclaration")
@SuppressLint("NewApi")
public class ICSSwitchPreference extends SwitchPreference {
    public ICSSwitchPreference(Context context, boolean initialState) {
        super(context);
        setChecked(initialState);
    }

    @Override
    protected void onClick() {
    }
}
