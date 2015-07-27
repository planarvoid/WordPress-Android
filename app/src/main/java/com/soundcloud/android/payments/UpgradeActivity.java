package com.soundcloud.android.payments;

import com.soundcloud.android.R;
import com.soundcloud.android.main.ScActivity;
import com.soundcloud.android.payments.googleplay.BillingResult;
import com.soundcloud.lightcycle.LightCycle;

import android.content.Intent;
import android.support.annotation.VisibleForTesting;

import javax.inject.Inject;

public class UpgradeActivity extends ScActivity {

    @Inject @LightCycle UpgradePresenter upgradePresenter;

    @VisibleForTesting
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        upgradePresenter.handleBillingResult(new BillingResult(requestCode, resultCode, data));
    }

    @Override
    protected void setContentView() {
        super.setContentView(R.layout.upgrade_activity);
        presenter.setToolBar();
    }

    @Override
    public Object onRetainCustomNonConfigurationInstance() {
        return upgradePresenter.getState();
    }
}
