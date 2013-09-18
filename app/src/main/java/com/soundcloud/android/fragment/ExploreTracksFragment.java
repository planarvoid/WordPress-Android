package com.soundcloud.android.fragment;

import com.actionbarsherlock.app.SherlockFragment;
import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.handmark.pulltorefresh.library.PullToRefreshGridView;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.PauseOnScrollListener;
import com.soundcloud.android.R;
import com.soundcloud.android.adapter.ExploreTracksAdapter;
import com.soundcloud.android.api.ExploreTracksOperations;
import com.soundcloud.android.fragment.behavior.EmptyViewAware;
import com.soundcloud.android.model.ExploreTracksCategory;
import com.soundcloud.android.model.ExploreTracksSuggestion;
import com.soundcloud.android.model.PlayInfo;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.rx.observers.ListFragmentObserver;
import com.soundcloud.android.rx.observers.PullToRefreshObserver;
import com.soundcloud.android.utils.PlayUtils;
import com.soundcloud.android.view.EmptyListView;
import rx.Observable;
import rx.android.concurrency.AndroidSchedulers;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;

public class ExploreTracksFragment extends SherlockFragment implements AdapterView.OnItemClickListener,
        EmptyViewAware, PullToRefreshBase.OnRefreshListener<GridView> {

    private static final int GRID_VIEW_ID = R.id.suggested_tracks_grid;
    private EmptyListView mEmptyListView;
    private ExploreTracksAdapter mExploreTracksAdapter;

    private int mEmptyViewStatus = EmptyListView.Status.WAITING;

    public static ExploreTracksFragment fromCategory(ExploreTracksCategory category) {
        final ExploreTracksFragment exploreTracksFragment = new ExploreTracksFragment();
        Bundle args = new Bundle();
        args.putParcelable(ExploreTracksCategory.EXTRA, category);
        exploreTracksFragment.setArguments(args);
        return exploreTracksFragment;
    }

    public ExploreTracksFragment() {
        this(null);
    }

    protected ExploreTracksFragment(ExploreTracksAdapter adapter) {
        mExploreTracksAdapter = adapter;
        setRetainInstance(true);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (mExploreTracksAdapter == null){
            final ExploreTracksCategory category = getArguments().getParcelable(ExploreTracksCategory.EXTRA);
            final Observable<Observable<ExploreTracksSuggestion>> suggestedTracks = new ExploreTracksOperations().getSuggestedTracks(category);

            ListFragmentObserver<ExploreTracksSuggestion, ExploreTracksFragment> observer = new ListFragmentObserver<ExploreTracksSuggestion, ExploreTracksFragment>(this);
            mExploreTracksAdapter = new ExploreTracksAdapter(suggestedTracks.observeOn(AndroidSchedulers.mainThread()), observer);
        }

        loadTrackSuggestions();
    }

    private void loadTrackSuggestions() {
        setEmptyViewStatus(EmptyListView.Status.WAITING);
        mExploreTracksAdapter.subscribe();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.suggested_tracks_fragment, container, false);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

        PlayUtils.playTrack(getActivity(), new PlayInfo(new Track(mExploreTracksAdapter.getItem(position)), true));
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mEmptyListView = (EmptyListView) view.findViewById(android.R.id.empty);
        mEmptyListView.setStatus(mEmptyViewStatus);
        mEmptyListView.setOnRetryListener(new EmptyListView.RetryListener() {
            @Override
            public void onEmptyViewRetry() {
                loadTrackSuggestions();
            }
        });

        PullToRefreshGridView ptrGridView = (PullToRefreshGridView) view.findViewById(GRID_VIEW_ID);
        ptrGridView.setOnRefreshListener(this);
        GridView gridView = ptrGridView.getRefreshableView();
        gridView.setOnItemClickListener(this);
        gridView.setAdapter(mExploreTracksAdapter);
        gridView.setEmptyView(mEmptyListView);

        // make sure this is called /after/ setAdapter, since the listener requires an EndlessPagingAdapter to be set
        gridView.setOnScrollListener(new PauseOnScrollListener(ImageLoader.getInstance(),false, true, mExploreTracksAdapter));
    }

    @Override
    public void onRefresh(PullToRefreshBase<GridView> refreshView) {
        mExploreTracksAdapter.subscribe(new PullToRefreshObserver<ExploreTracksFragment, ExploreTracksSuggestion>(
                this, GRID_VIEW_ID, mExploreTracksAdapter, mExploreTracksAdapter));
    }

    @Override
    public void setEmptyViewStatus(int status) {
        mEmptyViewStatus = status;
        if (mEmptyListView != null) {
            mEmptyListView.setStatus(status);
        }
    }
}
