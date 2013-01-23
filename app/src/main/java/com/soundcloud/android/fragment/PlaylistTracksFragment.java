package com.soundcloud.android.fragment;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.adapter.PlaylistTracksAdapter;
import com.soundcloud.android.model.Playlist;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.service.sync.ApiSyncService;
import org.jetbrains.annotations.NotNull;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;

public class PlaylistTracksFragment extends ListFragment implements LoaderManager.LoaderCallbacks<Cursor> {

    public static final String PLAYLIST_URI = "playlistUri";

    private static final int PLAYER_LIST_LOADER = 0x01;
    private Uri mContentUri;
    private PlaylistTracksAdapter mAdapter;

    public static PlaylistTracksFragment newInstance(Playlist mPlaylist) {
        PlaylistTracksFragment playlistTracksFragment = new PlaylistTracksFragment();
        Bundle args = new Bundle();
        args.putParcelable("playlistUri", mPlaylist.toUri());
        playlistTracksFragment.setArguments(args);
        return playlistTracksFragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Uri playlistUri = (Uri) getArguments().get(PLAYLIST_URI);
        Playlist p = SoundCloudApplication.MODEL_MANAGER.getPlaylist(playlistUri);

        mContentUri = Content.PLAYLIST_TRACKS.forId(p.id);
        getLoaderManager().initLoader(PLAYER_LIST_LOADER, null, this);
        mAdapter = new PlaylistTracksAdapter(getActivity().getApplicationContext(), null, true);
        setListAdapter(mAdapter);

        // send sync intent. TODO, this should check whether a sync is necessary for this playlist
        Intent intent = new Intent(getActivity(), ApiSyncService.class)
                .putExtra(ApiSyncService.EXTRA_IS_UI_REQUEST, true)
                .setData(mContentUri);

        getActivity().startService(intent);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(getActivity(), mContentUri, null, null, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mAdapter.swapCursor(data);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mAdapter.swapCursor(null);
    }
}
