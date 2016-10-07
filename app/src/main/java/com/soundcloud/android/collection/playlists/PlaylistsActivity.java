package com.soundcloud.android.collection.playlists;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.main.PlayerActivity;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.playlists.PlaylistsModule;
import com.soundcloud.android.view.screen.BaseLayoutHelper;
import dagger.Lazy;

import android.os.Bundle;
import android.support.v4.app.Fragment;

import javax.inject.Inject;
import javax.inject.Named;

public class PlaylistsActivity extends PlayerActivity {

    @Inject BaseLayoutHelper baseLayoutHelper;
    @Inject @Named(PlaylistsModule.PLAYLISTS_FRAGMENT) Lazy<Fragment> fragmentLazy;

    public PlaylistsActivity() {
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
        return Screen.PLAYLISTS;
    }

    private void attachFragment() {
        getSupportFragmentManager().beginTransaction()
                                   .replace(R.id.container, fragmentLazy.get())
                                   .commit();
    }

}
