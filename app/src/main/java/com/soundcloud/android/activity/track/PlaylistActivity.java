package com.soundcloud.android.activity.track;

import com.soundcloud.android.Actions;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.activity.UserBrowser;
import com.soundcloud.android.fragment.PlaylistTracksFragment;
import com.soundcloud.android.model.Playable;
import com.soundcloud.android.model.Playlist;
import com.soundcloud.android.service.playback.CloudPlaybackService;
import com.soundcloud.android.view.PlayableActionButtonsController;
import com.soundcloud.android.view.adapter.PlayableBar;
import org.jetbrains.annotations.NotNull;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

public class PlaylistActivity extends ScActivity implements Playlist.OnChangeListener {

    public static final String TRACKS_FRAGMENT_TAG = "tracks_fragment";
    public static final String EXTRA_SCROLL_TO_PLAYING_TRACK = "scroll_to_playing_track";
    private Playlist mPlaylist;
    private PlayableBar mPlaylistBar;
    private PlayableActionButtonsController mActionButtons;

    private PlaylistTracksFragment mFragment;

    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (mPlaylist != null) {
                mPlaylist.user_like = intent.getBooleanExtra(CloudPlaybackService.BroadcastExtras.isLike, false);
                mPlaylist.user_repost = intent.getBooleanExtra(CloudPlaybackService.BroadcastExtras.isRepost, false);
                mActionButtons.update(mPlaylist);
            }

            mFragment.refreshTrackList();
        }
    };

    public static void start(Context context, @NotNull Playlist playlist) {
        SoundCloudApplication.MODEL_MANAGER.cache(playlist);
        context.startActivity(getIntent(playlist));
    }

    public static Intent getIntent(@NotNull Playlist playlist) {
        Intent intent = new Intent(Actions.PLAYLIST);
        intent.setData(playlist.toUri());
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.playlist_activity);
        // hold on to the playlist instance instead of the URI or id as they may change going from local > global

        mPlaylistBar = (PlayableBar) findViewById(R.id.playable_bar);
        mPlaylistBar.addTextShadows();
        mPlaylistBar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                UserBrowser.startFromPlayable(PlaylistActivity.this, mPlaylist);
            }
        });

        mActionButtons = new PlayableActionButtonsController(mPlaylistBar);

        if (savedInstanceState == null) {
            mFragment = new PlaylistTracksFragment();
            getSupportFragmentManager().beginTransaction().add(R.id.playlist_tracks_fragment, mFragment, TRACKS_FRAGMENT_TAG).commit();
        } else {
            mFragment = (PlaylistTracksFragment) getSupportFragmentManager().findFragmentByTag(TRACKS_FRAGMENT_TAG);
        }

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Playable.ACTION_PLAYABLE_ASSOCIATION_CHANGED);
        registerReceiver(mReceiver, intentFilter);

        handleIntent();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent();
    }

    private void handleIntent(){
        final Playlist playlist = SoundCloudApplication.MODEL_MANAGER.getPlaylist(getIntent().getData());
        if (playlist != null) {
            final boolean changed = playlist != mPlaylist;
            if (mPlaylist != null && changed) {
                mPlaylist.stopObservingChanges(getContentResolver(), this);
            }

            mPlaylist = playlist;
            if (changed) refresh();
            if (getIntent().getBooleanExtra(EXTRA_SCROLL_TO_PLAYING_TRACK, false)) {
                mFragment.scrollToPosition(CloudPlaybackService.getPlaylistManager().getPosition());
            }

        } else {
            Log.e(SoundCloudApplication.TAG, "Playlist data missing: " + getIntent().getDataString());
            finish();
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mPlaylist != null){
            mPlaylist.startObservingChanges(getContentResolver(), this);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mPlaylist != null) {
            mPlaylist.stopObservingChanges(getContentResolver(), this);
        }
    }

    @Override
    protected int getSelectedMenuId() {
        return 0;
    }

    @Override
    public void onPlaylistChanged() {
        if (mPlaylist.removed){
            showToast(R.string.playlist_removed);
            finish();
        } else {
            mFragment.refresh(mPlaylist);
        }
    }

    private void refresh() {
        mFragment.refresh(mPlaylist);
        mPlaylistBar.display(mPlaylist);
        mActionButtons.update(mPlaylist);
    }
}
