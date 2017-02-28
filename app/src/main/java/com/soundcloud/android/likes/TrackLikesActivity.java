package com.soundcloud.android.likes;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.main.PlayerActivity;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.view.screen.BaseLayoutHelper;

import android.os.Bundle;

import javax.inject.Inject;

public class TrackLikesActivity extends PlayerActivity {

    @Inject BaseLayoutHelper baseLayoutHelper;
    @Inject TrackLikesIntentResolver intentResolver;

    public TrackLikesActivity() {
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    @Override
    protected void setActivityContentView() {
        baseLayoutHelper.setBaseLayout(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        intentResolver.onIntent(getIntent());

        if (savedInstanceState == null) {
            attachFragment();
        }
    }

    @Override
    public Screen getScreen() {
        return Screen.LIKES;
    }

    private void attachFragment() {
        getSupportFragmentManager().beginTransaction().replace(R.id.container, new TrackLikesFragment()).commit();
    }

}
