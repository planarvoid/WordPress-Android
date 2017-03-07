package com.soundcloud.android.main;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.rx.eventbus.EventBus;

import javax.inject.Inject;

public class LauncherActivity extends RootActivity {

    @Inject AccountOperations accountOperations;
    @Inject EventBus eventBus;
    @Inject Navigator navigator;

    public LauncherActivity() {
        SoundCloudApplication.getObjectGraph().inject(this);
    }

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
        navigator.launchHome(this, getIntent().getExtras());
    }

}
