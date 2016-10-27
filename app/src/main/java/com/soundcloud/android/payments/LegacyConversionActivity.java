package com.soundcloud.android.payments;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.main.LoggedInActivity;
import com.soundcloud.android.main.Screen;
import com.soundcloud.lightcycle.LightCycle;

import javax.inject.Inject;

@Deprecated // To be replaced by TieredConversionActivity
public class LegacyConversionActivity extends LoggedInActivity {

    @Inject @LightCycle LegacyConversionPresenter presenter;

    public LegacyConversionActivity() {
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    @Override
    public Screen getScreen() {
        return Screen.CONVERSION;
    }

    @Override
    protected void setActivityContentView() {
        super.setContentView(R.layout.legacy_conversion_activity);
    }

    @Override
    protected boolean receiveConfigurationUpdates() {
        return false;
    }
}
