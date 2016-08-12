package com.soundcloud.android.settings;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.R;
import com.soundcloud.android.main.LoggedInActivity;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.view.screen.BaseLayoutHelper;
import com.soundcloud.android.more.BasicSettingsFragment;

import android.os.Bundle;

import javax.inject.Inject;

public class SettingsActivity extends LoggedInActivity {

    @Inject FeatureFlags featureFlags;
    @Inject BaseLayoutHelper baseLayoutHelper;
    @Inject Navigator navigator;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);

        setContentFragment(BasicSettingsFragment.create());

        setTitle(R.string.title_settings);
    }

    @Override
    public Screen getScreen() {
        return Screen.SETTINGS_MAIN;
    }

    @Override
    public boolean onSupportNavigateUp() {
        navigator.openHome(this);
        finish();
        return true;
    }

    @Override
    protected void setActivityContentView() {
        baseLayoutHelper.setContainerLayout(this);
    }

}
