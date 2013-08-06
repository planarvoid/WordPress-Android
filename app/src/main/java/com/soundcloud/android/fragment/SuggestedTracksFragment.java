package com.soundcloud.android.fragment;

import com.actionbarsherlock.app.SherlockFragment;
import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.handmark.pulltorefresh.library.PullToRefreshGridView;
import com.soundcloud.android.R;
import com.soundcloud.android.adapter.EndlessPagingAdapter;
import com.soundcloud.android.adapter.SuggestedTracksAdapter;
import com.soundcloud.android.api.SuggestedTracksOperations;
import com.soundcloud.android.fragment.behavior.PagingAdapterViewAware;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.paging.AdapterViewPager;
import com.soundcloud.android.paging.PageItemObserver;
import com.soundcloud.android.rx.ScSchedulers;
import com.soundcloud.android.rx.observers.PullToRefreshObserver;
import com.soundcloud.android.view.EmptyListView;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;

public class SuggestedTracksFragment extends SherlockFragment implements AdapterView.OnItemClickListener,
        PagingAdapterViewAware<Track>, PullToRefreshBase.OnRefreshListener<GridView> {

    private final int mGridViewId = R.id.suggested_tracks_grid;
    private EmptyListView mEmptyListView;
    private SuggestedTracksAdapter mSuggestedTracksAdapter;
    private AdapterViewPager<Track, SuggestedTracksFragment> mAdapterViewPager;
    private PageItemObserver<Track,SuggestedTracksFragment> mItemObserver;

    private int mEmptyViewStatus = EmptyListView.Status.WAITING;

    public SuggestedTracksFragment() {
        this(new SuggestedTracksAdapter(), new AdapterViewPager<Track, SuggestedTracksFragment>(
                new SuggestedTracksOperations().getSuggestedTracks().observeOn(ScSchedulers.UI_SCHEDULER)));
    }

    public SuggestedTracksFragment(SuggestedTracksAdapter adapter, AdapterViewPager<Track, SuggestedTracksFragment> adapterViewPager) {
        mSuggestedTracksAdapter = adapter;
        mAdapterViewPager = adapterViewPager;
        setRetainInstance(true);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mItemObserver = new PageItemObserver<Track, SuggestedTracksFragment>(this);
        mAdapterViewPager.subscribe(this, mItemObserver);
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
        gridView.setAdapter(mSuggestedTracksAdapter);
        gridView.setEmptyView(mEmptyListView);

        // make sure this is called /after/ setAdapter, since the listener requires an EndlessPagingAdapter to be set
        gridView.setOnScrollListener(mAdapterViewPager.new PageScrollListener(mItemObserver));
    }

    @Override
    public void onRefresh(PullToRefreshBase<GridView> refreshView) {
        PageItemObserver<Track, SuggestedTracksFragment> itemObserver = new PageItemObserver<Track, SuggestedTracksFragment>(this);
        mAdapterViewPager.subscribe(this, new PullToRefreshObserver<SuggestedTracksFragment, Track>(
                this, mGridViewId, itemObserver));
    }

    @Override
    public void setEmptyViewStatus(int status) {
        mEmptyViewStatus = status;
        mEmptyListView.setStatus(status);
    }

    @Override
    public EndlessPagingAdapter<Track> getAdapter() {
        return mSuggestedTracksAdapter;
    }
}
