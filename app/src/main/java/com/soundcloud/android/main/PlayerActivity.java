package com.soundcloud.android.main;

import com.soundcloud.lightcycle.LightCycle;

import javax.inject.Inject;

public abstract class PlayerActivity extends LoggedInActivity {
    @Inject @LightCycle PlayerController playerController;

    @Override
    public void onBackPressed() {
        if (accountOperations.isCrawler() || !playerController.handleBackPressed()) {
            super.onBackPressed();
        }
    }
}
