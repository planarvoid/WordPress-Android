package com.soundcloud.android.activity.track;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.fragment.PlaylistTracksFragment;
import com.soundcloud.android.model.Playlist;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.android.view.PlayableActionButtonsController;
import com.soundcloud.android.view.adapter.PlayableBar;
import org.jetbrains.annotations.NotNull;

import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;

public class PlaylistActivity extends ScActivity {

    private Uri mPlaylistUri;
    private PlayableBar mPlaylistBar;
    private PlayableActionButtonsController mActionButtons;

    public static void start(Context context, @NotNull Playlist playlist) {
        Intent intent = new Intent(context, PlaylistActivity.class);
        intent.setData(playlist.toUri());
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.playlist_activity);
        mPlaylistUri = getIntent().getData();

        mPlaylistBar = (PlayableBar) findViewById(R.id.playable_bar);
        mPlaylistBar.addTextShadows();

        mActionButtons = new PlayableActionButtonsController(mPlaylistBar);

        if (refreshPlaylistData() && savedInstanceState == null) setupTracksFragment();

    }

    private boolean refreshPlaylistData(){
        Playlist mPlaylist;
        if (mPlaylistUri != null && (mPlaylist = getPlaylist()) != null) {
            mPlaylistBar.display(mPlaylist);
            mActionButtons.update(mPlaylist);

            TextView infoText = (TextView) findViewById(R.id.playlist_info_header);
            final String trackCount = getResources().getQuantityString(R.plurals.number_of_sounds, mPlaylist.track_count, mPlaylist.track_count);
            final String duration = ScTextUtils.formatTimestamp(mPlaylist.duration);
            infoText.setText(getString(R.string.playlist_info_header_text, trackCount, duration));

            return true;

        } else {
            Log.e(SoundCloudApplication.TAG,"Playlist not found: " + (mPlaylistUri == null ? "null" : mPlaylistUri.toString()));
            finish();
            return false;
        }
    }

    private Playlist getPlaylist() {
        return SoundCloudApplication.MODEL_MANAGER.getPlaylist(mPlaylistUri);
    }

    private void setupTracksFragment() {
        PlaylistTracksFragment fragment = PlaylistTracksFragment.newInstance(mPlaylistUri);
        getSupportFragmentManager().beginTransaction().add(R.id.playlist_tracks_fragment, fragment).commit();
    }

    @Override
    protected void onStart() {
        super.onStart();
        getContentResolver().registerContentObserver(mPlaylistUri, false, mPlaylistObserver);
    }

    @Override
    protected void onStop() {
        super.onStop();
        getContentResolver().unregisterContentObserver(mPlaylistObserver);
    }

    @Override
    protected int getSelectedMenuId() {
        return 0;
    }

    private ContentObserver mPlaylistObserver = new ContentObserver(new Handler()) {
        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            getContentResolver().notifyChange(Content.PLAYLIST_TRACKS.forId(getPlaylist().id), null);
        }
    };
}
