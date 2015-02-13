package com.soundcloud.android.payments;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.main.ScActivity;
import com.soundcloud.android.payments.googleplay.BillingResult;

import android.content.Intent;

import javax.inject.Inject;

public class SubscribeActivity extends ScActivity {

    @Inject SubscribeController controller;

    public SubscribeActivity() {
        lightCycleDispatcher.add(controller);
    }

    @VisibleForTesting
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        controller.handleBillingResult(new BillingResult(requestCode, resultCode, data));
    }

}
