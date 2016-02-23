package com.soundcloud.android.discovery;

import static com.soundcloud.java.checks.Preconditions.checkNotNull;

import com.soundcloud.android.R;
import com.soundcloud.android.main.PlayerActivity;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.search.PlaylistResultsFragment;
import com.soundcloud.android.view.screen.BaseLayoutHelper;

import android.os.Bundle;
import android.support.v4.app.Fragment;

import javax.inject.Inject;

public class PlaylistDiscoveryActivity extends PlayerActivity {
    public static final String EXTRA_PLAYLIST_TAG = "playlistTag";

    @Inject BaseLayoutHelper baseLayoutHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTitle(R.string.discovery_search_title);

        if (savedInstanceState == null) {
            final String playListTag = getIntent().getStringExtra(EXTRA_PLAYLIST_TAG);
            checkNotNull(playListTag, "Invalid playlist discovery tag");

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
