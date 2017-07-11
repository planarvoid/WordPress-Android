package com.soundcloud.android.analytics.eventlogger;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.main.LoggedInActivity;
import com.soundcloud.android.view.screen.BaseLayoutHelper;
import com.soundcloud.lightcycle.LightCycle;

import javax.inject.Inject;

public class DevEventLoggerMonitorActivity extends LoggedInActivity {

    @Inject BaseLayoutHelper baseLayoutHelper;
    @Inject @LightCycle DevEventLoggerMonitorPresenter presenter;

    public DevEventLoggerMonitorActivity() {
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    @Override
    protected void setActivityContentView() {
        super.setContentView(R.layout.dev_event_logger_monitor_activity);
        baseLayoutHelper.setupActionBar(this);
    }

    @Override
    public boolean onNavigateUp() {
        finish();
        return true;
    }
}
