package com.soundcloud.android.activity.track;

import com.soundcloud.android.Actions;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.fragment.PlaylistTracksFragment;
import com.soundcloud.android.model.Playable;
import com.soundcloud.android.model.Playlist;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.service.playback.CloudPlaybackService;
import com.soundcloud.android.view.PlayableActionButtonsController;
import com.soundcloud.android.view.adapter.PlayableBar;
import org.jetbrains.annotations.NotNull;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

public class PlaylistActivity extends ScActivity implements Playlist.OnChangeListener {

    public static final String TRACKS_FRAGMENT_TAG = "tracks_fragment";
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
        mPlaylist = SoundCloudApplication.MODEL_MANAGER.getPlaylist(getIntent().getData());

        mPlaylistBar = (PlayableBar) findViewById(R.id.playable_bar);
        mPlaylistBar.addTextShadows();

        mActionButtons = new PlayableActionButtonsController(mPlaylistBar);

        if (refreshPlaylistData() && savedInstanceState == null) {
            setupTracksFragment();
        } else {
            mFragment = (PlaylistTracksFragment) getSupportFragmentManager().findFragmentByTag(TRACKS_FRAGMENT_TAG);
        }

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Playable.ACTION_PLAYABLE_ASSOCIATION_CHANGED);
        registerReceiver(mReceiver, intentFilter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mReceiver);
    }

    private boolean refreshPlaylistData() {
        if (mPlaylist != null) {
            mPlaylistBar.display(mPlaylist);
            mActionButtons.update(mPlaylist);

            return true;

        } else {
            Log.e(SoundCloudApplication.TAG, "Playlist data missing: " + getIntent().getDataString());
            finish();
            return false;
        }
    }

    private void setupTracksFragment() {
        mFragment = PlaylistTracksFragment.newInstance(getIntent().getData());
        getSupportFragmentManager().beginTransaction().add(R.id.playlist_tracks_fragment, mFragment, TRACKS_FRAGMENT_TAG).commit();
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
            finish();
        } else {
            mFragment.refresh(mPlaylist);
        }

    }
}
