package com.soundcloud.android.activity.track;

import com.soundcloud.android.R;
import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.fragment.PlaylistTracksFragment;
import com.soundcloud.android.model.Playlist;
import com.soundcloud.android.service.sync.ApiSyncService;
import com.soundcloud.android.view.adapter.PlayableBar;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

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


        if (savedInstanceState == null){
            final Uri playlistUri = mPlaylist.toUri();

            setupTracklistFragment(playlistUri);

            // send sync intent. TODO, this should check whether a sync is necessary for this playlist
            Intent intent = new Intent(getActivity(), ApiSyncService.class)
                    .putExtra(ApiSyncService.EXTRA_IS_UI_REQUEST, true)
                    .setData(playlistUri);

            startService(intent);
        }
    }

    private void setupTracklistFragment(Uri playlistUri) {
        PlaylistTracksFragment fragment = new PlaylistTracksFragment();
        Bundle args = new Bundle();
        args.putParcelable("contentUri", playlistUri.buildUpon().appendPath("tracks").build());
        fragment.setArguments(args);

        getSupportFragmentManager().beginTransaction().add(R.id.playlist_tracks_fragment, fragment).commit();
    }

    @Override
    protected int getSelectedMenuId() {
        return 0;
    }
}
