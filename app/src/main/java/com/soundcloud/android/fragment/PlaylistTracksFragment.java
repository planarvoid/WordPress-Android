package com.soundcloud.android.fragment;

import com.handmark.pulltorefresh.extras.listfragment.PullToRefreshListFragment;
import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.adapter.PlaylistTracksAdapter;
import com.soundcloud.android.model.Playlist;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.service.sync.ApiSyncService;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;

public class PlaylistTracksFragment extends PullToRefreshListFragment implements LoaderManager.LoaderCallbacks<Cursor>,
        PullToRefreshBase.OnRefreshListener {

    public static final String PLAYLIST_URI = "playlistUri";

    private static final int PLAYER_LIST_LOADER = 0x01;
    private Uri mContentUri, mPlaylistUri;

    private PlaylistTracksAdapter mAdapter;

    public static PlaylistTracksFragment newInstance(Uri playlistUri) {
        PlaylistTracksFragment playlistTracksFragment = new PlaylistTracksFragment();
        Bundle args = new Bundle();
        args.putParcelable("playlistUri", playlistUri);
        playlistTracksFragment.setArguments(args);
        return playlistTracksFragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mPlaylistUri = (Uri) getArguments().get(PLAYLIST_URI);
        Playlist p = SoundCloudApplication.MODEL_MANAGER.getPlaylist(mPlaylistUri);
        mContentUri = Content.PLAYLIST_TRACKS.forId(p.id);
        getLoaderManager().initLoader(PLAYER_LIST_LOADER, null, this);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new CursorLoader(getActivity(), mContentUri, null, null, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        if (mAdapter == null) {

            // if we don't have the entire playlist, re-sync the playlist.
            final Playlist playlist = SoundCloudApplication.MODEL_MANAGER.getPlaylist(mPlaylistUri);
            if (data == null || data.getCount() < playlist.track_count) {
                getActivity().startService(new Intent(getActivity(), ApiSyncService.class)
                        .putExtra(ApiSyncService.EXTRA_IS_UI_REQUEST, true)
                        .setData(mPlaylistUri));
            }

            // if we can show something (or should show nothing), set the adapter
            if (data != null && (data.getCount() > 0 || playlist.track_count == 0)) {
                mAdapter = new PlaylistTracksAdapter(getActivity().getApplicationContext(), data, true);
                setListShownNoAnimation(true);
                setListAdapter(mAdapter);
            }

        } else {
            mAdapter.swapCursor(data);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        if (mAdapter != null) mAdapter.swapCursor(null);

    }

    @Override
    public void onRefresh(PullToRefreshBase refreshView) {
        // TODO, refresh logic
    }
}
