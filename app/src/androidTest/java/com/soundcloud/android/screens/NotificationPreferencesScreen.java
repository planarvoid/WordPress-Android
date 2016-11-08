package com.soundcloud.android.screens;

import com.soundcloud.android.framework.Han;
import com.soundcloud.android.settings.notifications.NotificationPreferencesActivity;

public class NotificationPreferencesScreen extends Screen {

    private static final Class ACTIVITY = NotificationPreferencesActivity.class;

    public NotificationPreferencesScreen(Han solo) {
        super(solo);
    }

    @Override
    protected Class getActivity() {
        return ACTIVITY;
    }
}
