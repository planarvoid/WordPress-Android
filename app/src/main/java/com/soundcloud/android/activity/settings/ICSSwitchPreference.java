package com.soundcloud.android.activity.settings;

import android.annotation.TargetApi;
import android.content.Context;
import android.preference.SwitchPreference;

@SuppressWarnings("UnusedDeclaration")
@TargetApi(14)
public class ICSSwitchPreference extends SwitchPreference {
    public ICSSwitchPreference(Context context, boolean initialState) {
        super(context);
        setChecked(initialState);
    }

    @Override
    protected void onClick() {
    }
}
