package com.soundcloud.android.settings;

import com.soundcloud.android.Actions;
import com.soundcloud.android.R;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.ScreenEvent;
import com.soundcloud.android.main.ScActivity;
import com.soundcloud.android.view.screen.BaseLayoutHelper;

import android.content.Intent;
import android.os.Bundle;

import javax.inject.Inject;

public class SettingsActivity extends ScActivity {

    @Inject BaseLayoutHelper baseLayoutHelper;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentFragment(SettingsFragment.create());

        setTitle(R.string.title_settings);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (shouldTrackScreen()) {
            eventBus.publish(EventQueue.TRACKING, ScreenEvent.create(Screen.SETTINGS_MAIN));
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        startActivity(new Intent(Actions.STREAM).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
        finish();
        return true;
    }

    @Override
    protected void setActivityContentView() {
        baseLayoutHelper.setContainerLayout(this);
    }

}
