package com.soundcloud.android.fragment;

import com.soundcloud.android.adapter.ScCursorAdapter;
import com.soundcloud.android.adapter.UserFavoritesAdapter;
import com.soundcloud.android.provider.ScContentProvider;

import android.database.Cursor;
import android.database.CursorWrapper;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.View;
import android.widget.AdapterView;

public abstract class CursorListFragment extends ListFragment implements
        LoaderManager.LoaderCallbacks<Cursor> {

	private final static String LOG_TAG = CursorListFragment.class.getName();
	private ScCursorAdapter adapter;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		adapter = newAdapter();
		setListAdapter(adapter);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState){
		super.onActivityCreated(savedInstanceState);

        getListView().setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapter, View view, int position, long id) {
                onListItemClick((CursorWrapper) adapter.getItemAtPosition(position));
            }
        });
	}

	@Override
	public void onResume() {
		super.onResume();
		getLoaderManager().restartLoader(0, null, this);
	}

	@Override
	public void onPause() {
		super.onPause();
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		return new CursorLoader(getActivity(), getCursorUri(), null,null, null,null);
	}

    @Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		adapter.swapCursor(cursor);
	}

	@Override
	public void onLoaderReset(Loader<Cursor> cursor) {
		adapter.swapCursor(null);
	}

	public void refresh() {
		getLoaderManager().restartLoader(0, null, this);
	}

    abstract protected ScCursorAdapter newAdapter();
    abstract protected Uri getCursorUri();
    abstract protected void onListItemClick(CursorWrapper cursorWrapper);
}
