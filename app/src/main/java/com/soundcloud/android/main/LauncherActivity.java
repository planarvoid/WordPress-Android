package com.soundcloud.android.main;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.R;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.rx.eventbus.EventBus;

import android.os.Bundle;
import android.support.annotation.Nullable;

import javax.inject.Inject;

public class LauncherActivity extends RootActivity {

    @Inject AccountOperations accountOperations;
    @Inject EventBus eventBus;
    @Inject Navigator navigator;

    @Override
    public Screen getScreen() {
        return Screen.UNKNOWN;
    }

    @Override
    protected boolean receiveConfigurationUpdates() {
        return false;
    }

    @Override
    protected void setActivityContentView() {
        setContentView(R.layout.launch);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (accountOperations.isUserLoggedIn()) {
            handleLoggedInUser();
        } else {
            accountOperations.triggerLoginFlow(LauncherActivity.this);
        }
    }

    private void handleLoggedInUser() {
        final Bundle extras = getIntent().getExtras();
        if (hasPendingActivity(extras)) {
            navigator.openPendingActivity(this, extras);
        } else {
            navigator.launchHome(this, extras);
        }
    }

    private boolean hasPendingActivity(@Nullable Bundle extras) {
        return extras != null && extras.containsKey(Navigator.EXTRA_PENDING_ACTIVITY);
    }

}
