package com.soundcloud.android.activity.track;

import com.soundcloud.android.R;
import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.fragment.PlaylistTracksFragment;
import com.soundcloud.android.model.Playlist;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.android.view.adapter.PlayableBar;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.TextView;

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
        mPlaylistBar.display(mPlaylist);

        setupPlaylistInfoHeader();

        if (savedInstanceState == null){
            setupTracklistFragment(Content.PLAYLIST_TRACKS.forId(mPlaylist.id));
        }
    }

    private void setupPlaylistInfoHeader() {
        TextView infoText = (TextView) findViewById(R.id.playlist_info_header);

        final String trackCount = getResources().getQuantityString(R.plurals.number_of_sounds, mPlaylist.track_count, mPlaylist.track_count);
        final String duration = ScTextUtils.formatTimestamp(mPlaylist.duration);
        infoText.setText(getString(R.string.playlist_info_header_text, trackCount, duration));
    }

    private void setupTracklistFragment(Uri playlistTracksUri) {
        PlaylistTracksFragment fragment = new PlaylistTracksFragment();
        Bundle args = new Bundle();
        args.putParcelable("contentUri", playlistTracksUri);
        fragment.setArguments(args);

        getSupportFragmentManager().beginTransaction().add(R.id.playlist_tracks_fragment, fragment).commit();
    }

    @Override
    protected int getSelectedMenuId() {
        return 0;
    }
}
