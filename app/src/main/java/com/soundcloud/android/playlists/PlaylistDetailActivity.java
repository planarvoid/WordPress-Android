package com.soundcloud.android.playlists;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.analytics.PromotedSourceInfo;
import com.soundcloud.android.analytics.SearchQuerySourceInfo;
import com.soundcloud.android.main.FullscreenablePlayerActivity;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.navigation.BottomNavigationViewPresenter;
import com.soundcloud.android.navigation.NavigationExecutor;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.utils.LightCycleLogger;
import com.soundcloud.android.utils.Log;
import com.soundcloud.android.utils.Urns;
import com.soundcloud.android.view.screen.BaseLayoutHelper;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.lightcycle.ActivityLightCycle;
import com.soundcloud.lightcycle.LightCycle;
import org.jetbrains.annotations.NotNull;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;

import javax.inject.Inject;
import javax.inject.Named;

public class PlaylistDetailActivity extends FullscreenablePlayerActivity {

    static final String LOG_TAG = "PlaylistDetails";

    public static final String EXTRA_URN = "urn";
    public static final String EXTRA_AUTO_PLAY = "autoplay";
    public static final String EXTRA_QUERY_SOURCE_INFO = "query_source_info";
    public static final String EXTRA_PROMOTED_SOURCE_INFO = "promoted_source_info";

    @Inject BaseLayoutHelper baseLayoutHelper;
    @Inject FeatureFlags featureFlags;
    @Inject NavigationExecutor navigationExecutor;
    @Inject @Named(PlaylistsModule.FULLSCREEN_PLAYLIST_DETAILS) boolean showFullscreenPlaylistDetails;
    // Chasing https://fabric.io/soundcloudandroid/android/apps/com.soundcloud.android/issues/594beedebe077a4dcc7a2de0?time=last-thirty-days
    @LightCycle ActivityLightCycle<Activity> logger = LightCycleLogger.forActivity("PlaylistDetailActivity");
    @Inject @LightCycle BottomNavigationViewPresenter bottomNavigationViewPresenter;

    public static Intent getIntent(Context context,
                                   @NotNull Urn playlistUrn,
                                   Screen screen,
                                   boolean autoPlay,
                                   Optional<SearchQuerySourceInfo> queryInfo,
                                   Optional<PromotedSourceInfo> promotedInfo) {
        Intent intent = new Intent(context, PlaylistDetailActivity.class);
        screen.addToIntent(intent);
        Urns.writeToIntent(intent, EXTRA_URN, playlistUrn);
        return intent
                .putExtra(EXTRA_AUTO_PLAY, autoPlay)
                .putExtra(EXTRA_QUERY_SOURCE_INFO, queryInfo.orNull())
                .putExtra(EXTRA_PROMOTED_SOURCE_INFO, promotedInfo.orNull());
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
        Urn urn = Urns.urnFromIntent(intent, EXTRA_URN);
        if (urn == null) {
            throw new IllegalStateException("Playlist URN may not be null. " + intent.toString());
        }
        PromotedSourceInfo promotedSourceInfo = intent.getParcelableExtra(EXTRA_PROMOTED_SOURCE_INFO);
        SearchQuerySourceInfo searchQuerySourceInfo = intent.getParcelableExtra(EXTRA_QUERY_SOURCE_INFO);
        boolean autoplay = intent.getBooleanExtra(EXTRA_AUTO_PLAY, false);
        Log.d(LOG_TAG, "(Re-)creating fragment for " + urn);

        Fragment fragment = PlaylistDetailFragment.create(urn, Screen.fromIntent(intent), searchQuerySourceInfo, promotedSourceInfo, autoplay);

        getSupportFragmentManager().beginTransaction().replace(R.id.container, fragment).commit();
    }

    @Override
    protected void setActivityContentView() {
        super.setActivityContentView();
        if (shouldBeFullscreen()) {
            baseLayoutHelper.setBaseNoToolbar(this);
        } else {
            baseLayoutHelper.setBaseLayout(this);
        }
    }

    @Override
    protected boolean shouldBeFullscreen() {
        return showFullscreenPlaylistDetails;
    }

}
