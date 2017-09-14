package com.soundcloud.android.discovery.systemplaylist;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.main.PlayerActivity;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.utils.Urns;
import com.soundcloud.android.navigation.BottomNavigationViewPresenter;
import com.soundcloud.android.view.screen.BaseLayoutHelper;
import com.soundcloud.java.strings.Strings;
import com.soundcloud.lightcycle.LightCycle;

import android.os.Bundle;

import javax.inject.Inject;

public class SystemPlaylistActivity extends PlayerActivity {

    public static final String EXTRA_PLAYLIST_URN = "extra_system_playlist_urn";

    @Inject BaseLayoutHelper baseLayoutHelper;
    @Inject @LightCycle BottomNavigationViewPresenter bottomNavigationViewPresenter;

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

    private void attachFragment() {
        final SystemPlaylistFragment fragment;
            final Urn urn = Urns.urnFromIntent(getIntent(), EXTRA_PLAYLIST_URN);
            fragment = SystemPlaylistFragment.newInstance(urn);
            setTitle(Strings.EMPTY);
        getSupportFragmentManager().beginTransaction()
                                   .replace(R.id.container, fragment, SystemPlaylistFragment.TAG)
                                   .commit();
    }
}
