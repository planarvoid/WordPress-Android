package com.soundcloud.android.collection.playhistory;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.main.PlayerActivity;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.view.screen.BaseLayoutHelper;

import android.os.Bundle;

import javax.inject.Inject;

public class PlayHistoryActivity extends PlayerActivity {

    @Inject BaseLayoutHelper baseLayoutHelper;

    public PlayHistoryActivity() {
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    @Override
    protected void setActivityContentView() {
        baseLayoutHelper.setBaseLayout(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            attachFragment();
        }
    }

    @Override
    public Screen getScreen() {
        return Screen.PLAY_HISTORY;
    }

    private void attachFragment() {
        getSupportFragmentManager().beginTransaction().replace(R.id.container, new PlayHistoryFragment()).commit();
    }

}
