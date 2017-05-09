package com.soundcloud.android.discovery.systemplaylist;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.main.PlayerActivity;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.view.screen.BaseLayoutHelper;
import com.soundcloud.java.strings.Strings;

import android.os.Bundle;

import javax.inject.Inject;

public class SystemPlaylistActivity extends PlayerActivity {

    public static final String EXTRA_PLAYLIST_URN = "extra_system_playlist_urn";
    public static final String EXTRA_FOR_NEW_FOR_YOU = "extra_new_for_you";

    @Inject BaseLayoutHelper baseLayoutHelper;

    public SystemPlaylistActivity() {
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
        final boolean forNewForYou = getIntent().getBooleanExtra(EXTRA_FOR_NEW_FOR_YOU, false);
        return forNewForYou ? Screen.NEW_FOR_YOU : Screen.SYSTEM_PLAYLIST;
    }

    private void attachFragment() {
        final boolean forNewForYou = getIntent().getBooleanExtra(EXTRA_FOR_NEW_FOR_YOU, false);
        final SystemPlaylistFragment fragment;
        if (forNewForYou) {
            fragment = SystemPlaylistFragment.newNewForYouInstance();
            setTitle(R.string.new_for_you_title);
        } else {
            final Urn urn = getIntent().getParcelableExtra(EXTRA_PLAYLIST_URN);
            fragment = SystemPlaylistFragment.newInstance(urn);
            setTitle(Strings.EMPTY);
        }
        getSupportFragmentManager().beginTransaction()
                                   .replace(R.id.container, fragment, SystemPlaylistFragment.TAG)
                                   .commit();
    }
}
