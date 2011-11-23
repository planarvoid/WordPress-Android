package com.soundcloud.android.fragment;

import com.soundcloud.android.adapter.LazyBaseAdapter;
import com.soundcloud.android.adapter.LazyEndlessAdapter;
import com.soundcloud.android.loader.ApiCollectionLoader;
import com.soundcloud.android.view.ScListView;

import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

public abstract class RemoteListFragment extends ScListFragment {

    private final static String LOG_TAG = RemoteListFragment.class.getName();

    private boolean mLoading;
    private ApiCollectionLoader.ApiResult mLastResponse;

    @Override public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        setRetainInstance(true);
        if (savedInstanceState != null)
        if (savedInstanceState != null && getListView() != null) {
            // Restore last state for checked position.

            getListView().postSelect(savedInstanceState.getInt("firstVisible", 1),savedInstanceState.getInt("top", 0), true);
        }
    }

    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setListAdapter(newAdapter());
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        final ListView lv = getListView();
        outState.putInt("firstVisible",lv  == null ? null : lv.getFirstVisiblePosition() == 0 ? 1 : lv.getFirstVisiblePosition());
        outState.putInt("top",lv == null ? null : lv.getChildAt(0) == null || lv.getFirstVisiblePosition() == 0 ? 0 : lv.getChildAt(0).getTop());

    }

    @Override public void onListItemClick(ListView l, View v, int position, long id) {
        onListItemClick(position);
    }

    abstract protected LazyEndlessAdapter newAdapter();
    abstract protected void onListItemClick(int position);



}
