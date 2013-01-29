package com.soundcloud.android.activity.track;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.activity.ScActivity;
import com.soundcloud.android.fragment.PlaylistTracksFragment;
import com.soundcloud.android.model.Playlist;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.view.PlayableActionButtonsController;
import com.soundcloud.android.utils.ScTextUtils;
import com.soundcloud.android.view.adapter.PlayableBar;
import org.jetbrains.annotations.NotNull;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.TextView;

public class PlaylistActivity extends ScActivity {

    private Playlist mPlaylist;

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

        mPlaylist = SoundCloudApplication.MODEL_MANAGER.getPlaylist(getIntent().getData());
        mPlaylistBar = (PlayableBar) findViewById(R.id.playable_bar);
        mPlaylistBar.display(mPlaylist);

        mActionButtons = new PlayableActionButtonsController(mPlaylistBar);
        mActionButtons.update(mPlaylist);

        setupPlaylistInfoHeader();

        setupTracklistFragment(Content.PLAYLIST_TRACKS.forId(mPlaylist.id));
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
