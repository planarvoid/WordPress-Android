package com.soundcloud.android.payments;

import com.soundcloud.android.actionbar.ActionBarController;
import com.soundcloud.android.main.ScActivity;

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
    protected ActionBarController createActionBarController() {
        // No overflow or search
        return null;
    }

}
