package com.soundcloud.android.payments;

import com.soundcloud.android.actionbar.ActionBarController;
import com.soundcloud.android.main.ScActivity;
import com.soundcloud.android.payments.googleplay.PlayBillingResult;

import android.content.Intent;
import android.os.Bundle;

import javax.inject.Inject;

public class SubscribeActivity extends ScActivity {

    @Inject SubscribeController controller;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        controller.onCreate(this);
    }

    @Override
    protected void onDestroy() {
        controller.onDestroy();
        super.onDestroy();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        controller.handleBillingResult(new PlayBillingResult(requestCode, resultCode, data));
    }

    @Override
    protected ActionBarController createActionBarController() {
        // No overflow or search
        return null;
    }

}
