package com.soundcloud.android.playlists;

import static com.soundcloud.java.checks.Preconditions.checkNotNull;

import com.soundcloud.android.Actions;
import com.soundcloud.android.Navigator;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.analytics.PromotedSourceInfo;
import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.main.PlayerActivity;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.utils.Log;
import com.soundcloud.android.view.screen.BaseLayoutHelper;
import org.jetbrains.annotations.NotNull;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;

import javax.inject.Inject;

public class PlaylistDetailActivity extends PlayerActivity {

    static final String LOG_TAG = "PlaylistDetails";

    public static final String EXTRA_URN = "urn";
    public static final String EXTRA_AUTO_PLAY = "autoplay";
    public static final String EXTRA_QUERY_SOURCE_INFO = "query_source_info";
    public static final String EXTRA_PROMOTED_SOURCE_INFO = "promoted_source_info";

    @Inject BaseLayoutHelper baseLayoutHelper;
    @Inject FeatureFlags featureFlags;
    @Inject Navigator navigator;

    public static Intent getIntent(@NotNull Urn playlistUrn, Screen screen, boolean autoPlay) {
        return getIntent(playlistUrn, screen, autoPlay, null, null);
    }

    public static Intent getIntent(@NotNull Urn playlistUrn,
                                   Screen screen,
                                   boolean autoPlay,
                                   SearchQuerySourceInfo queryInfo,
                                   PromotedSourceInfo promotedInfo) {
        Intent intent = new Intent(Actions.PLAYLIST);
        screen.addToIntent(intent);

        checkNotNull(playlistUrn, "Playlist URN may no be null. " +
                "Params: playlistUrn = [" + playlistUrn + "], screen = [" + screen + "], autoPlay = [" + autoPlay + "], queryInfo = [" + queryInfo + "], promotedInfo = [" + promotedInfo + "]");

        return intent
                .putExtra(EXTRA_URN, playlistUrn)
                .putExtra(EXTRA_AUTO_PLAY, autoPlay)
                .putExtra(EXTRA_QUERY_SOURCE_INFO, queryInfo)
                .putExtra(EXTRA_PROMOTED_SOURCE_INFO, promotedInfo);
    }

    public PlaylistDetailActivity() {
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            createFragmentForPlaylist();
        }
    }

    private void createFragmentForPlaylist() {
        Intent intent = getIntent();
        Screen screen = Screen.fromIntent(intent);

        Urn urn = intent.getParcelableExtra(EXTRA_URN);
        PromotedSourceInfo promotedSourceInfo = intent.getParcelableExtra(EXTRA_PROMOTED_SOURCE_INFO);
        SearchQuerySourceInfo searchQuerySourceInfo = intent.getParcelableExtra(EXTRA_QUERY_SOURCE_INFO);
        boolean autoplay = intent.getBooleanExtra(EXTRA_AUTO_PLAY, false);
        Log.d(LOG_TAG, "(Re-)creating fragment for " + urn);

        Fragment fragment = featureFlags.isEnabled(Flag.EDIT_PLAYLIST_V2) ? NewPlaylistDetailFragment.create(urn, screen, searchQuerySourceInfo, promotedSourceInfo, autoplay)
                                                                          : PlaylistDetailFragment.create(urn, screen, searchQuerySourceInfo, promotedSourceInfo, autoplay);

        getSupportFragmentManager().beginTransaction().replace(R.id.container, fragment).commit();
    }

    @Override
    protected void setActivityContentView() {
        baseLayoutHelper.setBaseLayout(this);
    }

    @Override
    public Screen getScreen() {
        return Screen.PLAYLIST_DETAILS;
    }

}
