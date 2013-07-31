package com.soundcloud.android.fragment;

import com.actionbarsherlock.app.SherlockFragment;
import com.soundcloud.android.R;
import com.soundcloud.android.adapter.EndlessPagingAdapter;
import com.soundcloud.android.adapter.SuggestedTracksAdapter;
import com.soundcloud.android.api.SuggestedTracksOperations;
import com.soundcloud.android.fragment.behavior.PagingAdapterViewAware;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.paging.AdapterViewPager;
import com.soundcloud.android.rx.ScSchedulers;
import com.soundcloud.android.view.EmptyListView;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;

public class SuggestedTracksFragment extends SherlockFragment implements AdapterView.OnItemClickListener,
        PagingAdapterViewAware<Track> {

    private EmptyListView mEmptyListView;
    private SuggestedTracksAdapter mSuggestedTracksAdapter;
    private AdapterViewPager<Track, SuggestedTracksFragment> mAdapterViewPager;

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
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.suggested_track_fragment, container, false);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        mAdapterViewPager.loadNextPage(this);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mEmptyListView = (EmptyListView) view.findViewById(android.R.id.empty);
        mEmptyListView.setStatus(EmptyListView.Status.WAITING);

        GridView mGridView = (GridView) view.findViewById(R.id.gridview);
        mGridView.setOnItemClickListener(this);
        mGridView.setOnScrollListener(mAdapterViewPager.new PageScrollListener(this));
        mGridView.setAdapter(mSuggestedTracksAdapter);
        mGridView.setEmptyView(mEmptyListView);

        mAdapterViewPager.startLoading(this);
    }

    @Override
    public EmptyListView getEmptyView() {
        return mEmptyListView;
    }

    @Override
    public EndlessPagingAdapter<Track> getAdapter() {
        return mSuggestedTracksAdapter;
    }
}
