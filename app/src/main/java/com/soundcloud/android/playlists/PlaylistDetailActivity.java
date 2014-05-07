package com.soundcloud.android.playlists;

import com.soundcloud.android.Actions;
import com.soundcloud.android.R;
import com.soundcloud.android.ads.AdPlayerController;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.ScreenEvent;
import com.soundcloud.android.main.ScActivity;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.ui.SlidingPlayerController;
import com.soundcloud.android.utils.Log;
import com.soundcloud.android.view.screen.ScreenPresenter;
import org.jetbrains.annotations.NotNull;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;

import javax.inject.Inject;

public class PlaylistDetailActivity extends ScActivity {

    static final String LOG_TAG = "PlaylistDetails";

    public static final String EXTRA_URN = "urn";
    public static String EXTRA_AUTO_PLAY = "autoplay";

    @Inject SlidingPlayerController playerController;
    @Inject AdPlayerController adPlayerController;
    @Inject ScreenPresenter presenter;

    public static void start(Context context, @NotNull Urn playlist, Screen screen) {
        start(context, playlist, screen, false);
    }

    public static void start(Context context, Urn playlistUrn, Screen screen, boolean autoPlay) {
        context.startActivity(getIntent(playlistUrn, screen, autoPlay));
    }

    public static Intent getIntent(@NotNull Urn playlistUrn, Screen screen) {
        return getIntent(playlistUrn, screen, false);
    }

    public static Intent getIntent(@NotNull Urn playlistUrn, Screen screen, boolean autoPlay) {
        Intent intent = new Intent(Actions.PLAYLIST);
        screen.addToIntent(intent);
        intent.putExtra(EXTRA_AUTO_PLAY, autoPlay);
        return intent.putExtra(EXTRA_URN, playlistUrn);
    }

    public PlaylistDetailActivity() {
        addLifeCycleComponent(playerController);
        addLifeCycleComponent(adPlayerController);
        presenter.attach(this);
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
        Urn urn = getIntent().getParcelableExtra(EXTRA_URN);
        Screen screen = Screen.fromIntent(getIntent());
        Log.d(LOG_TAG, "(Re-)creating fragment for " + urn);

        Fragment fragment = PlaylistFragment.create(urn, screen);
        getSupportFragmentManager().beginTransaction().replace(R.id.container, fragment).commit();
    }

    @Override
    protected void setContentView() {
        presenter.setBaseLayout();
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
