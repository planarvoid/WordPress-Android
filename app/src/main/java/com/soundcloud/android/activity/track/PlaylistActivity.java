package com.soundcloud.android.activity.track;

import com.soundcloud.android.Actions;
import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.activity.UserBrowser;
import com.soundcloud.android.fragment.PlaylistTracksFragment;
import com.soundcloud.android.model.Playlist;
import com.soundcloud.android.service.playback.CloudPlaybackService;
import com.soundcloud.android.service.playback.PlayQueueManager;
import com.soundcloud.android.utils.ImageUtils;
import com.soundcloud.android.view.FullImageDialog;
import com.soundcloud.android.view.PlayableActionButtonsController;
import com.soundcloud.android.view.adapter.PlayableBar;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

public class PlaylistActivity extends ScActivity implements Playlist.OnChangeListener {

    public static final String TRACKS_FRAGMENT_TAG = "tracks_fragment";
    public static final String EXTRA_SCROLL_TO_PLAYING_TRACK = "scroll_to_playing_track";
    private Playlist mPlaylist;
    private PlayableBar mPlaylistBar;
    private PlayableActionButtonsController mActionButtons;

    private PlaylistTracksFragment mFragment;

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

        handleIntent(savedInstanceState, true);
    }

    private void handleIntent(@Nullable Bundle savedInstanceState, boolean setupViews) {
        final Playlist playlist = SoundCloudApplication.MODEL_MANAGER.getPlaylist(getIntent().getData());
        if (assertPlaylistAvailable(playlist)) {
            boolean playlistChanged = setPlaylist(playlist);

            if (setupViews) {
                setupViews(savedInstanceState);
            }

            if (playlistChanged) refresh();

            if (getIntent().getBooleanExtra(EXTRA_SCROLL_TO_PLAYING_TRACK, false)) {
                PlayQueueManager playQueueManager = CloudPlaybackService.getPlaylistManager();
                if (playQueueManager != null) mFragment.scrollToPosition(playQueueManager.getPosition());
            }
        }
    }

    private void setupViews(@Nullable Bundle savedInstanceState) {
        mPlaylistBar = (PlayableBar) findViewById(R.id.playable_bar);
        mPlaylistBar.addTextShadows();
        mPlaylistBar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                UserBrowser.startFromPlayable(PlaylistActivity.this, mPlaylist);
            }
        });

        mPlaylistBar.findViewById(R.id.icon).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String artwork = mPlaylist.getArtwork();
                if (ImageUtils.checkIconShouldLoad(artwork)) {
                    new FullImageDialog(PlaylistActivity.this, Consts.GraphicSize.CROP.formatUri(artwork)).show();
                }

            }
        });

        mActionButtons = new PlayableActionButtonsController(mPlaylistBar);

        if (savedInstanceState == null) {
            mFragment = PlaylistTracksFragment.create(mPlaylist);
            getSupportFragmentManager().beginTransaction().add(R.id.playlist_tracks_fragment, mFragment, TRACKS_FRAGMENT_TAG).commit();
        } else {
            mFragment = (PlaylistTracksFragment) getSupportFragmentManager().findFragmentByTag(TRACKS_FRAGMENT_TAG);
        }
    }

    private boolean assertPlaylistAvailable(@Nullable Playlist playlist) {
        if (playlist == null) {
            Toast.makeText(this, R.string.playlist_removed, Toast.LENGTH_SHORT).show();
            finish();
            return false;
        }
        return true;
    }

    private boolean setPlaylist(@Nullable Playlist playlist) {
        boolean changed = playlist != mPlaylist;
        if (mPlaylist != null && changed) {
            mPlaylist.stopObservingChanges(getContentResolver(), this);
        }
        mPlaylist = playlist;
        return changed;
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);

        handleIntent(null, false);
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
            refresh();
        }
    }

    private void refresh() {
        mFragment.refresh(mPlaylist);
        mPlaylistBar.display(mPlaylist);
        mActionButtons.update(mPlaylist);
    }
}
