package com.soundcloud.android.settings;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.main.LoggedInActivity;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.view.screen.BaseLayoutHelper;
import com.soundcloud.lightcycle.LightCycle;

import javax.inject.Inject;

public class ChangeStorageLocationActivity extends LoggedInActivity {

    @Inject BaseLayoutHelper baseLayoutHelper;
    @Inject @LightCycle ChangeStorageLocationPresenter presenter;

    public ChangeStorageLocationActivity() {
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    @Override
    protected void setActivityContentView() {
        super.setContentView(R.layout.change_storage_location_activity);
        baseLayoutHelper.setupActionBar(this);
    }

    @Override
    public Screen getScreen() {
        return Screen.SETTINGS_OFFLINE_STORAGE_LOCATION;
    }

    @Override
    public boolean onNavigateUp() {
        finish();
        return true;
    }
}
