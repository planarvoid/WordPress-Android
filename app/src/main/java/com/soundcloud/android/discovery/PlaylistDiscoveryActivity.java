package com.soundcloud.android.discovery;

import static com.soundcloud.java.checks.Preconditions.checkNotNull;

import com.soundcloud.android.R;
import com.soundcloud.android.main.PlayerController;
import com.soundcloud.android.main.ScActivity;
import com.soundcloud.android.search.PlaylistResultsFragment;
import com.soundcloud.android.view.screen.BaseLayoutHelper;
import com.soundcloud.lightcycle.LightCycle;

import android.os.Bundle;
import android.support.v4.app.Fragment;

import javax.inject.Inject;

public class PlaylistDiscoveryActivity extends ScActivity {
    public static final String EXTRA_PLAYLIST_TAG = "playlistTag";

    @Inject @LightCycle PlayerController playerController;

    @Inject BaseLayoutHelper baseLayoutHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTitle(R.string.activity_title_search_results);

        if (savedInstanceState == null) {
            final String playListTag = getIntent().getStringExtra(EXTRA_PLAYLIST_TAG);
            checkNotNull(playListTag, "Invalid playlist discovery tag");

            setTitle("#" + playListTag);
            createFragmentForPlaylistDiscovery(playListTag);
        }
    }

    private void createFragmentForPlaylistDiscovery(String playlistTag) {
        Fragment fragment = PlaylistResultsFragment.create(playlistTag);
        getSupportFragmentManager().beginTransaction().replace(R.id.container, fragment).commit();
    }

    @Override
    protected void setActivityContentView() {
        baseLayoutHelper.setBaseLayout(this);
    }
}
