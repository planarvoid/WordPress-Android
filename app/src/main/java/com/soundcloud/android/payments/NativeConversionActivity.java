package com.soundcloud.android.payments;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.main.LoggedInActivity;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.payments.googleplay.BillingResult;
import com.soundcloud.android.view.screen.BaseLayoutHelper;
import com.soundcloud.lightcycle.LightCycle;

import android.content.Intent;
import android.support.annotation.VisibleForTesting;

import javax.inject.Inject;

public class NativeConversionActivity extends LoggedInActivity {

    @Inject @LightCycle NativeConversionPresenter nativeConversionPresenter;

    @Inject BaseLayoutHelper baseLayoutHelper;

    public NativeConversionActivity() {
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    @VisibleForTesting
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        nativeConversionPresenter.handleBillingResult(new BillingResult(requestCode, resultCode, data));
    }

    @Override
    public Screen getScreen() {
        return Screen.CONVERSION;
    }

    @Override
    protected void setActivityContentView() {
        super.setContentView(R.layout.conversion_activity);
        baseLayoutHelper.setupActionBar(this);
    }

    @Override
    public Object onRetainCustomNonConfigurationInstance() {
        return nativeConversionPresenter.getState();
    }
}
