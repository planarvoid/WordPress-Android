package com.soundcloud.android.fragment;

import com.soundcloud.android.adapter.LazyBaseAdapter;
import com.soundcloud.android.adapter.ScCursorAdapter;
import com.soundcloud.android.loader.ApiCollectionLoader;

import android.content.Context;
import android.database.CursorWrapper;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;

import java.util.List;

public abstract class RemoteListFragment extends ListFragment
        implements LoaderManager.LoaderCallbacks<List<Parcelable>> {

    private final static String LOG_TAG = RemoteListFragment.class.getName();

    LazyBaseAdapter mAdapter;

    @Override public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // Create an empty adapter we will use to display the loaded data.
        mAdapter = newAdapter();
        setListAdapter(mAdapter);

        // Start out with a progress indicator.
        setListShown(false);

        // Prepare the loader.  Either re-connect with an existing one,
        // or start a new one.
        getLoaderManager().initLoader(0, null, this);
    }

    @Override public void onListItemClick(ListView l, View v, int position, long id) {
        onListItemClick(position);
    }

    @Override public Loader<List<Parcelable>> onCreateLoader(int id, Bundle args) {
        // This is called when a new Loader needs to be created.  This
        // sample only has one Loader with no arguments, so it is simple.
        return newLoader();
    }

    @Override public void onLoadFinished(Loader<List<Parcelable>> loader, List<Parcelable> data) {
        // Set the new data in the adapter.
        mAdapter.setData(data);

        // The list should now be shown.
        if (isResumed()) {
            setListShown(true);
        } else {
            setListShownNoAnimation(true);
        }
    }

    @Override public void onLoaderReset(Loader<List<Parcelable>> loader) {
        // Clear the data in the adapter.
        mAdapter.setData(null);
    }

    abstract protected ApiCollectionLoader newLoader();
    abstract protected LazyBaseAdapter newAdapter();
    abstract protected void onListItemClick(int position);
}
