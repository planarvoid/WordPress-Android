package com.soundcloud.android.settings;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.main.LoggedInActivity;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.view.screen.BaseLayoutHelper;

import android.os.Bundle;

import javax.inject.Inject;

public class LicensesActivity extends LoggedInActivity {

    @Inject BaseLayoutHelper baseLayoutHelper;

    public LicensesActivity() {
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentFragment(LicensesFragment.create());
    }

    @Override
    public boolean onNavigateUp() {
        finish();
        return true;
    }

    @Override
    public Screen getScreen() {
        return Screen.SETTINGS_LICENSES;
    }

    @Override
    protected void setActivityContentView() {
        baseLayoutHelper.setContainerLayout(this);
    }

}
