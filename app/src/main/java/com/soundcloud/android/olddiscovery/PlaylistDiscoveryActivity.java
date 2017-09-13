package com.soundcloud.android.olddiscovery;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.main.PlayerActivity;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.navigation.BottomNavigationViewPresenter;
import com.soundcloud.android.search.PlaylistResultsFragment;
import com.soundcloud.android.view.screen.BaseLayoutHelper;
import com.soundcloud.lightcycle.LightCycle;

import android.os.Bundle;
import android.support.v4.app.Fragment;

import javax.inject.Inject;

public class PlaylistDiscoveryActivity extends PlayerActivity {
    public static final String EXTRA_PLAYLIST_TAG = "playlistTag";

    @Inject BaseLayoutHelper baseLayoutHelper;
    @Inject @LightCycle BottomNavigationViewPresenter bottomNavigationViewPresenter;

    public PlaylistDiscoveryActivity() {
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTitle(R.string.discovery_search_title);

        if (savedInstanceState == null) {
            final String playListTag = getIntent().getStringExtra(EXTRA_PLAYLIST_TAG);
            if (playListTag == null) {
                throw new IllegalStateException("Invalid playlist discovery tag");
            }

            setTitle("#" + playListTag);
            createFragmentForPlaylistDiscovery(playListTag);
        }
    }

    private void createFragmentForPlaylistDiscovery(String playlistTag) {
        Fragment fragment = PlaylistResultsFragment.create(playlistTag);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.container, fragment, PlaylistResultsFragment.TAG)
                .commit();
    }

    @Override
    public Screen getScreen() {
        return Screen.SEARCH_PLAYLIST_DISCO;
    }

    @Override
    protected void setActivityContentView() {
        baseLayoutHelper.setBaseLayout(this);
    }

}
