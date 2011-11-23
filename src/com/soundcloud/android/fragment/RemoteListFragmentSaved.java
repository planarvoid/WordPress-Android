package com.soundcloud.android.fragment;

import com.soundcloud.android.adapter.LazyBaseAdapter;
import com.soundcloud.android.loader.ApiCollectionLoader;

import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.View;
import android.widget.ListView;

public abstract class RemoteListFragmentSaved extends ScListFragment
        implements LoaderManager.LoaderCallbacks<ApiCollectionLoader.ApiResult> {

    private final static String LOG_TAG = RemoteListFragmentSaved.class.getName();

    LazyBaseAdapter mAdapter;

    private boolean mLoading;
    private ApiCollectionLoader.ApiResult mLastResponse;

    @Override public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // Create an empty adapter we will use to display the loaded data.
        mAdapter = newAdapter();
        setListAdapter(mAdapter);
        setRetainInstance(true);

        // Prepare the loader.  Either re-connect with an existing one,
        // or start a new one.
        getLoaderManager().initLoader(0, null, this);
    }



    @Override public void onListItemClick(ListView l, View v, int position, long id) {
        onListItemClick(position);
    }

    @Override public Loader<ApiCollectionLoader.ApiResult> onCreateLoader(int id, Bundle args) {
        // This is called when a new Loader needs to be created.  This
        // sample only has one Loader with no arguments, so it is simple.

        return newLoader(mLastResponse);

    }

    @Override public void onLoadFinished(Loader<ApiCollectionLoader.ApiResult> loader, ApiCollectionLoader.ApiResult response) {
        // Set the new data in the adapter.
        mAdapter.getData().addAll(response.items);
        mAdapter.notifyDataSetChanged();
        mLastResponse = response;
        mLoading = false;
    }

    @Override public void onLoaderReset(Loader<ApiCollectionLoader.ApiResult> loader) {
        // Clear the data in the adapter.
        mAdapter.setData(null);
    }

    protected void loadMore(){
        if (!mLoading){
            getLoaderManager().restartLoader(0,null,this);
        }
    }

    abstract protected ApiCollectionLoader newLoader(ApiCollectionLoader.ApiResult lastResponse);
    abstract protected LazyBaseAdapter newAdapter();
    abstract protected void onListItemClick(int position);



}
