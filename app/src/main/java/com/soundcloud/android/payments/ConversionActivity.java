package com.soundcloud.android.payments;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.main.LoggedInActivity;
import com.soundcloud.android.main.Screen;
import com.soundcloud.lightcycle.LightCycle;

import javax.inject.Inject;

public class ConversionActivity extends LoggedInActivity {

    @Inject @LightCycle ConversionPresenter presenter;

    public ConversionActivity() {
        SoundCloudApplication.getObjectGraph().inject(this);
        analyticsConnector.suppressInAppMessages();
    }

    @Override
    public Screen getScreen() {
        return Screen.CONVERSION;
    }

    @Override
    protected void setActivityContentView() {
        super.setContentView(R.layout.conversion_activity);
    }

    @Override
    protected boolean receiveConfigurationUpdates() {
        return false;
    }
}
