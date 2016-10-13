package com.soundcloud.android.payments;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.main.LoggedInActivity;
import com.soundcloud.android.main.Screen;
import com.soundcloud.lightcycle.LightCycle;

import javax.inject.Inject;

public class TieredConversionActivity extends LoggedInActivity {

    @Inject @LightCycle TieredConversionPresenter presenter;

    public TieredConversionActivity() {
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    @Override
    public Screen getScreen() {
        return Screen.CONVERSION;
    }

    @Override
    protected void setActivityContentView() {
        super.setContentView(R.layout.tiered_conversion_activity);
    }

    @Override
    protected boolean receiveConfigurationUpdates() {
        return false;
    }
}
