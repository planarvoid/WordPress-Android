package com.soundcloud.android.fragment;

import com.actionbarsherlock.app.SherlockFragment;
import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.handmark.pulltorefresh.library.PullToRefreshGridView;
import com.soundcloud.android.R;
import com.soundcloud.android.adapter.ExploreTracksAdapter;
import com.soundcloud.android.api.ExploreTracksOperations;
import com.soundcloud.android.fragment.behavior.AdapterViewAware;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.rx.ScSchedulers;
import com.soundcloud.android.rx.observers.ItemObserver;
import com.soundcloud.android.rx.observers.PullToRefreshObserver;
import com.soundcloud.android.view.EmptyListView;
import rx.Observer;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;

public class ExploreTracksFragment extends SherlockFragment implements AdapterView.OnItemClickListener,
        AdapterViewAware<Track>, PullToRefreshBase.OnRefreshListener<GridView> {

    private final int mGridViewId = R.id.suggested_tracks_grid;
    private EmptyListView mEmptyListView;
    private ExploreTracksAdapter mExploreTracksAdapter;
    private ItemObserver<Track,ExploreTracksFragment> mItemObserver;

    private int mEmptyViewStatus = EmptyListView.Status.WAITING;

    public ExploreTracksFragment() {
        this(new ExploreTracksAdapter(new ExploreTracksOperations().getSuggestedTracks().observeOn(ScSchedulers.UI_SCHEDULER)));
    }

    public ExploreTracksFragment(ExploreTracksAdapter adapter) {
        mExploreTracksAdapter = adapter;
        setRetainInstance(true);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mItemObserver = new ItemObserver<Track, ExploreTracksFragment>(this);
        mExploreTracksAdapter.subscribe(mItemObserver);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.suggested_tracks_fragment, container, false);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mEmptyListView = (EmptyListView) view.findViewById(android.R.id.empty);
        mEmptyListView.setStatus(mEmptyViewStatus);

        PullToRefreshGridView ptrGridView = (PullToRefreshGridView) view.findViewById(mGridViewId);
        ptrGridView.setOnRefreshListener(this);
        GridView gridView = ptrGridView.getRefreshableView();
        gridView.setOnItemClickListener(this);
        gridView.setAdapter(mExploreTracksAdapter);
        gridView.setEmptyView(mEmptyListView);

        // make sure this is called /after/ setAdapter, since the listener requires an EndlessPagingAdapter to be set
        gridView.setOnScrollListener(mExploreTracksAdapter);
    }

    @Override
    public void onRefresh(PullToRefreshBase<GridView> refreshView) {
        ItemObserver<Track, ExploreTracksFragment> itemObserver = new ItemObserver<Track, ExploreTracksFragment>(this);
        mExploreTracksAdapter.subscribe(new PullToRefreshObserver<ExploreTracksFragment, Track>(
                this, mGridViewId, mExploreTracksAdapter, itemObserver));
    }

    @Override
    public void setEmptyViewStatus(int status) {
        mEmptyViewStatus = status;
        mEmptyListView.setStatus(status);
    }

    @Override
    public Observer<Track> getAdapterObserver() {
        return mExploreTracksAdapter;
    }


}
