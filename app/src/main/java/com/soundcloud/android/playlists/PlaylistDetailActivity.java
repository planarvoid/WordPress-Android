package com.soundcloud.android.playlists;

import com.soundcloud.android.Actions;
import com.soundcloud.android.R;
import com.soundcloud.android.analytics.PromotedSourceInfo;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.ScreenEvent;
import com.soundcloud.android.main.PlayerController;
import com.soundcloud.android.main.ScActivity;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.utils.Log;
import com.soundcloud.android.view.screen.BaseLayoutHelper;
import com.soundcloud.lightcycle.LightCycle;
import org.jetbrains.annotations.NotNull;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;

import javax.inject.Inject;

public class PlaylistDetailActivity extends ScActivity {

    static final String LOG_TAG = "PlaylistDetails";

    public static final String EXTRA_URN = "urn";
    public static final String EXTRA_AUTO_PLAY = "autoplay";
    public static final String EXTRA_QUERY_SOURCE_INFO = "query_source_info";
    public static final String EXTRA_PROMOTED_SOURCE_INFO = "promoted_source_info";

    @Inject @LightCycle PlayerController playerController;

    @Inject BaseLayoutHelper baseLayoutHelper;

    @Deprecated // Use Navigator
    public static void start(Context context, @NotNull Urn playlist, Screen screen) {
        start(context, playlist, screen, false);
    }

    @Deprecated // Use Navigator
    public static void start(Context context, @NotNull Urn playlist, Screen screen, SearchQuerySourceInfo searchQuerySourceInfo) {
        start(context, playlist, screen, false, searchQuerySourceInfo);
    }

    @Deprecated // Use Navigator
    public static void start(Context context, Urn playlistUrn, Screen screen, boolean autoPlay) {
        start(context, playlistUrn, screen, autoPlay, null);
    }

    @Deprecated // Use Navigator
    public static void start(Context context, Urn playlistUrn, Screen screen, boolean autoPlay, SearchQuerySourceInfo searchQuerySourceInfo) {
        context.startActivity(getIntent(playlistUrn, screen, autoPlay, searchQuerySourceInfo, null));
    }

    public static Intent getIntent(@NotNull Urn playlistUrn, Screen screen) {
        return getIntent(playlistUrn, screen, false);
    }

    public static Intent getIntent(@NotNull Urn playlistUrn, Screen screen, boolean autoPlay) {
        return getIntent(playlistUrn, screen, autoPlay, null, null);
    }

    public static Intent getIntent(@NotNull Urn playlistUrn, Screen screen, boolean autoPlay, SearchQuerySourceInfo queryInfo, PromotedSourceInfo promotedInfo) {
        Intent intent = new Intent(Actions.PLAYLIST);
        screen.addToIntent(intent);
        return intent
                .putExtra(EXTRA_URN, playlistUrn)
                .putExtra(EXTRA_AUTO_PLAY, autoPlay)
                .putExtra(EXTRA_QUERY_SOURCE_INFO, queryInfo)
                .putExtra(EXTRA_PROMOTED_SOURCE_INFO, promotedInfo);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTitle(R.string.activity_title_playlist);

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
        Log.d(LOG_TAG, "(Re-)creating fragment for " + urn);

        Fragment fragment = PlaylistDetailFragment.create(urn, screen, searchQuerySourceInfo, promotedSourceInfo);
        getSupportFragmentManager().beginTransaction().replace(R.id.container, fragment).commit();
    }

    @Override
    protected void setActivityContentView() {
        baseLayoutHelper.setBaseLayout(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (shouldTrackScreen()) {
            eventBus.publish(EventQueue.TRACKING, ScreenEvent.create(Screen.PLAYLIST_DETAILS));
        }
    }

    @Override
    public void onBackPressed() {
        if (!playerController.handleBackPressed()) {
            super.onBackPressed();
        }
    }
}
