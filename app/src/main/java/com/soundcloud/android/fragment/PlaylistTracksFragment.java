package com.soundcloud.android.fragment;

import com.soundcloud.android.adapter.PlaylistTracksAdapter;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;

public class PlaylistTracksFragment extends ListFragment implements LoaderManager.LoaderCallbacks<Cursor> {

    private static final int PLAYER_LIST_LOADER = 0x01;
    private Uri mContentUri;
    private PlaylistTracksAdapter mAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mContentUri = (Uri) getArguments().get("contentUri");
        getLoaderManager().initLoader(PLAYER_LIST_LOADER, null, this);
        mAdapter = new PlaylistTracksAdapter(getActivity().getApplicationContext(), null, true);
        setListAdapter(mAdapter);
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
