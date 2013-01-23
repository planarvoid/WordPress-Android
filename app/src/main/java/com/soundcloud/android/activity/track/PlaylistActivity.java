package com.soundcloud.android.activity.track;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.fragment.PlaylistTracksFragment;
import com.soundcloud.android.model.Playlist;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.view.adapter.PlayableBar;

import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;

public class PlaylistActivity extends ScActivity {

    public static final String PLAYLIST_EXTRA = "com.soundcloud.android.playlist";

    private Playlist mPlaylist;
    private PlayableBar mPlaylistBar;

    public static void start(Context context, Playlist playlist) {
        Intent intent = new Intent(context, PlaylistActivity.class);
        intent.putExtra(PLAYLIST_EXTRA, playlist);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.playlist_activity);
        mPlaylist = getIntent().getParcelableExtra(PLAYLIST_EXTRA);
        mPlaylistBar = (PlayableBar) findViewById(R.id.playable_bar);

        final Uri playlistUri = getIntent().getData();
        if (playlistUri != null && (mPlaylist = SoundCloudApplication.MODEL_MANAGER.getPlaylist(mPlaylist.id)) != null) {
            mPlaylistBar.display(mPlaylist);
            if (savedInstanceState == null) setupTracksFragment();

        } else {
            finish();
        }
    }

    private void setupTracksFragment() {
        PlaylistTracksFragment fragment = PlaylistTracksFragment.newInstance(mPlaylist);
        getSupportFragmentManager().beginTransaction().add(R.id.playlist_tracks_fragment, fragment).commit();
    }

    @Override
    protected void onStart() {
        super.onStart();
        getContentResolver().registerContentObserver(mPlaylist.toUri(), false, mPlaylistObserver);
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected int getSelectedMenuId() {
        return 0;
    }

    private ContentObserver mPlaylistObserver = new ContentObserver(new Handler()) {
        @Override
        public boolean deliverSelfNotifications() {
            return super.deliverSelfNotifications();
        }

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            getContentResolver().notifyChange(Content.PLAYLIST_TRACKS.forId(mPlaylist.id), null);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            super.onChange(selfChange, uri);
        }
    };
}
