package com.soundcloud.android.fragment;

import com.actionbarsherlock.app.SherlockFragment;
import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.handmark.pulltorefresh.library.PullToRefreshGridView;
import com.soundcloud.android.R;
import com.soundcloud.android.adapter.ExploreTracksAdapter;
import com.soundcloud.android.api.ExploreTracksOperations;
import com.soundcloud.android.fragment.behavior.EmptyViewAware;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.rx.ScSchedulers;
import com.soundcloud.android.rx.observers.ListFragmentObserver;
import com.soundcloud.android.rx.observers.PullToRefreshObserver;
import com.soundcloud.android.view.EmptyListView;
import rx.Observable;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;

public class ExploreTracksFragment extends SherlockFragment implements AdapterView.OnItemClickListener,
        EmptyViewAware, PullToRefreshBase.OnRefreshListener<GridView> {

    private final int mGridViewId = R.id.suggested_tracks_grid;
    private EmptyListView mEmptyListView;
    private ExploreTracksAdapter mExploreTracksAdapter;

    private int mEmptyViewStatus = EmptyListView.Status.WAITING;

    public ExploreTracksFragment() {
        Observable<Observable<Track>> observable = new ExploreTracksOperations().getSuggestedTracks().observeOn(ScSchedulers.UI_SCHEDULER);
        ListFragmentObserver<Track, ExploreTracksFragment> observer = new ListFragmentObserver<Track, ExploreTracksFragment>(this);
        ExploreTracksAdapter adapter = new ExploreTracksAdapter(observable, observer);
        init(adapter);
    }

    public ExploreTracksFragment(ExploreTracksAdapter adapter) {
        init(adapter);
    }

    private void init(ExploreTracksAdapter adapter) {
        mExploreTracksAdapter = adapter;
        setRetainInstance(true);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mExploreTracksAdapter.subscribe();
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
        mExploreTracksAdapter.subscribe(new PullToRefreshObserver<ExploreTracksFragment, Track>(
                this, mGridViewId, mExploreTracksAdapter, mExploreTracksAdapter));
    }

    @Override
    public void setEmptyViewStatus(int status) {
        mEmptyViewStatus = status;
        mEmptyListView.setStatus(status);
    }
}
