package com.soundcloud.android.settings;

import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.ScreenEvent;
import com.soundcloud.android.main.ScActivity;

import android.os.Bundle;

public class OfflineSettingsActivity extends ScActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getFragmentManager()
                .beginTransaction()
                .replace(android.R.id.content, OfflineSettingsFragment.create())
                .commit();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (shouldTrackScreen()) {
            eventBus.publish(EventQueue.TRACKING, ScreenEvent.create(Screen.SETTINGS_OFFLINE));
        }
    }

    @Override
    public boolean onNavigateUp() {
        finish();
        return true;
    }

}
