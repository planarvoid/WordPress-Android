package com.soundcloud.android.activity.track;

import com.soundcloud.android.R;
import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.fragment.ScListFragment;
import com.soundcloud.android.model.Playlist;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.view.adapter.PlayableBar;

import android.content.Context;
import android.content.Intent;
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

        getSupportFragmentManager().beginTransaction()
                .add(R.id.playlist_tracks_fragment, ScListFragment.newInstance(Content.ME_ACTIVITIES)).commit();



    }

    @Override
    protected int getSelectedMenuId() {
        return 0;
    }
}
