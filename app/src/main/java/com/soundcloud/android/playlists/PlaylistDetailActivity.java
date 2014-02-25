package com.soundcloud.android.playlists;

import com.soundcloud.android.Actions;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.analytics.OriginProvider;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.associations.EngagementsController;
import com.soundcloud.android.associations.SoundAssociationOperations;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.image.ImageSize;
import com.soundcloud.android.main.ScActivity;
import com.soundcloud.android.model.Playlist;
import com.soundcloud.android.model.ScModelManager;
import com.soundcloud.android.playback.service.PlaybackService;
import com.soundcloud.android.playback.service.PlaybackStateProvider;
import com.soundcloud.android.playback.views.PlayablePresenter;
import com.soundcloud.android.profile.ProfileActivity;
import com.soundcloud.android.utils.images.ImageUtils;
import com.soundcloud.android.view.FullImageDialog;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import javax.inject.Inject;

public class PlaylistDetailActivity extends ScActivity implements Playlist.OnChangeListener {

    public static final String EXTRA_SCROLL_TO_PLAYING_TRACK = "scroll_to_playing_track";
    private static final String TRACKS_FRAGMENT_TAG = "tracks_fragment";
    private Playlist mPlaylist;
    private PlayablePresenter mPlayablePresenter;
    private EngagementsController mEngagementsController;

    @Inject
    ScModelManager mModelManager;
    @Inject
    ImageOperations mImageOperations;
    @Inject
    SoundAssociationOperations mSoundAssocOps;
    @Inject
    PlaybackStateProvider mPlaybackStateProvider;

    private PlaylistTracksFragment mFragment;

    private final BroadcastReceiver mPlaybackStatusListener = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (PlaybackService.Broadcasts.PLAYSTATE_CHANGED.equals(action)) {
                mFragment.getAdapter().notifyDataSetChanged();
            }
        }
    };

    public static void start(Context context, @NotNull Playlist playlist, Screen screen) {
        start(context, playlist, SoundCloudApplication.sModelManager, screen);
    }

    public static void start(Context context, @NotNull Playlist playlist, ScModelManager modelManager, Screen screen) {
        modelManager.cache(playlist);
        context.startActivity(getIntent(playlist, screen));
    }

    public static Intent getIntent(@NotNull Playlist playlist, Screen screen) {
        Intent intent = new Intent(Actions.PLAYLIST);
        screen.addToIntent(intent);
        return intent.setData(playlist.toUri());
    }

    public PlaylistDetailActivity() {
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setTitle(R.string.activity_title_playlist);
        setContentView(R.layout.playlist_activity);

        mPlayablePresenter = new PlayablePresenter(this);
        mPlayablePresenter.setPlayableRowView(findViewById(R.id.playable_bar))
            .setArtwork((ImageView) findViewById(R.id.icon), ImageSize.getListItemImageSize(this), R.drawable.artwork_badge);
        mPlayablePresenter.addTextShadowForGrayBg();


        mEngagementsController = new EngagementsController(this, findViewById(R.id.playlist_action_bar),
                getApp().getEventBus(), mSoundAssocOps, new OriginProvider() {
            @Override
            public String getScreenTag() {
                return Screen.fromIntent(getIntent()).get();
            }
        });
        mEngagementsController.startListeningForChanges();

        handleIntent(savedInstanceState, true);

        // listen for playback changes, so that we can update the now-playing indicator
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(PlaybackService.Broadcasts.PLAYSTATE_CHANGED);
        registerReceiver(mPlaybackStatusListener, intentFilter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (shouldTrackScreen()) {
            mEventBus.publish(EventQueue.SCREEN_ENTERED, Screen.PLAYLIST_DETAILS.get());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mPlaybackStatusListener);
        mEngagementsController.stopListeningForChanges();
    }

    private void handleIntent(@Nullable Bundle savedInstanceState, boolean setupViews) {
        final Playlist playlist = mModelManager.getPlaylist(getIntent().getData());
        if (playlist != null) {
            boolean playlistChanged = setPlaylist(playlist);

            if (setupViews) {
                setupViews(savedInstanceState);
            }

            if (playlistChanged) refresh();

            if (getIntent().getBooleanExtra(EXTRA_SCROLL_TO_PLAYING_TRACK, false)) {
                mFragment.scrollToPosition(mPlaybackStateProvider.getPlayPosition());
            }
        } else {
            Toast.makeText(this, R.string.playlist_removed, Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void setupViews(@Nullable Bundle savedInstanceState) {

        View playlistBar = findViewById(R.id.playable_bar);
        playlistBar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ProfileActivity.startFromPlayable(PlaylistDetailActivity.this, mPlaylist);
            }
        });

        playlistBar.findViewById(R.id.icon).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String artwork = mPlaylist.getArtwork();
                if (ImageUtils.checkIconShouldLoad(artwork)) {
                    new FullImageDialog(PlaylistDetailActivity.this, ImageSize.CROP.formatUri(artwork), mImageOperations).show();
                }

            }
        });

        if (savedInstanceState == null) {
            mFragment = PlaylistTracksFragment.create(getIntent().getData(), Screen.fromIntent(getIntent()));
            getSupportFragmentManager().beginTransaction().add(R.id.playlist_tracks_fragment, mFragment, TRACKS_FRAGMENT_TAG).commit();
        } else {
            mFragment = (PlaylistTracksFragment) getSupportFragmentManager().findFragmentByTag(TRACKS_FRAGMENT_TAG);
        }
    }

    private boolean setPlaylist(@NotNull Playlist playlist) {
        boolean changed = playlist != mPlaylist;
        if (mPlaylist != null && changed) {
            mPlaylist.stopObservingChanges(getContentResolver(), this);
        }
        mPlaylist = playlist;
        mPlayablePresenter.setPlayable(playlist);
        mEngagementsController.setPlayable(playlist);
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
        if (mPlaylist != null) {
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
    public void onPlaylistChanged() {
        if (mPlaylist.removed) {
            showToast(R.string.playlist_removed);
            finish();
        } else {
            refresh();
        }
    }

    private void refresh() {
        mFragment.refresh();
        mPlayablePresenter.setPlayable(mPlaylist);
    }
}
