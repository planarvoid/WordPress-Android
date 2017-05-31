package com.soundcloud.android.settings;

import com.soundcloud.android.navigation.NavigationExecutor;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.main.LoggedInActivity;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.view.screen.BaseLayoutHelper;

import android.os.Bundle;

import javax.inject.Inject;

public class OfflineSettingsActivity extends LoggedInActivity {

    @Inject BaseLayoutHelper baseLayoutHelper;
    @Inject NavigationExecutor navigationExecutor;

    public OfflineSettingsActivity() {
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentFragment(OfflineSettingsFragment.create());
    }

    @Override
    public boolean onSupportNavigateUp() {
        navigationExecutor.openMore(this);
        return true;
    }

    @Override
    public Screen getScreen() {
        return Screen.SETTINGS_OFFLINE;
    }

    @Override
    protected void setActivityContentView() {
        baseLayoutHelper.setContainerLayout(this);
    }

}
