package com.soundcloud.android.activities;

import com.soundcloud.android.navigation.NavigationExecutor;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.main.PlayerActivity;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.view.screen.BaseLayoutHelper;

import android.os.Bundle;

import javax.inject.Inject;

public class ActivitiesActivity extends PlayerActivity {

    @Inject BaseLayoutHelper baseLayoutHelper;
    @Inject NavigationExecutor navigationExecutor;

    public ActivitiesActivity() {
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                                       .add(getContentHolderViewId(), new ActivitiesFragment()).commit();
        }
    }

    @Override
    protected void setActivityContentView() {
        baseLayoutHelper.setBaseLayoutWithMargins(this);
    }

    @Override
    public Screen getScreen() {
        return Screen.ACTIVITIES;
    }

    @Override
    public boolean onSupportNavigateUp() {
        navigationExecutor.openHome(this);
        finish();
        return true;
    }
}
