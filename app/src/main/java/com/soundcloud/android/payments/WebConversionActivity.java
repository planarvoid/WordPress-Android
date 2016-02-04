package com.soundcloud.android.payments;

import com.soundcloud.android.R;
import com.soundcloud.android.main.ScActivity;
import com.soundcloud.android.main.Screen;
import com.soundcloud.lightcycle.LightCycle;

import javax.inject.Inject;

public class WebConversionActivity extends ScActivity {
    @Inject @LightCycle WebConversionPresenter presenter;

    @Override
    public Screen getScreen() {
        return Screen.CONVERSION;
    }

    @Override
    protected void setActivityContentView() {
        super.setContentView(R.layout.conversion_activity);
    }
}
