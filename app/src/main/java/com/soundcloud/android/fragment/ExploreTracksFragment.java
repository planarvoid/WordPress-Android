package com.soundcloud.android.fragment;

import static rx.android.OperationPaged.Page;

import com.actionbarsherlock.app.SherlockFragment;
import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.handmark.pulltorefresh.library.PullToRefreshGridView;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.PauseOnScrollListener;
import com.soundcloud.android.R;
import com.soundcloud.android.adapter.ExploreTracksAdapter;
import com.soundcloud.android.adapter.ItemAdapter;
import com.soundcloud.android.api.ExploreTracksOperations;
import com.soundcloud.android.fragment.behavior.EmptyViewAware;
import com.soundcloud.android.model.ExploreTracksCategory;
import com.soundcloud.android.model.SuggestedTracksCollection;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.rx.observers.ListFragmentObserver;
import com.soundcloud.android.utils.AbsListViewParallaxer;
import com.soundcloud.android.utils.PlayUtils;
import com.soundcloud.android.view.EmptyListView;
import rx.Observer;
import rx.Subscription;
import rx.android.BufferingObserver;
import rx.android.RxFragmentObserver;
import rx.android.concurrency.AndroidSchedulers;
import rx.observables.ConnectableObservable;
import rx.subscriptions.Subscriptions;

import android.os.Bundle;
import android.support.v4.app.Fragment;
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
    private Subscription mSubscription = Subscriptions.empty();

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
            mExploreTracksAdapter = new ExploreTracksAdapter();
        }

        loadTrackSuggestions(new ListFragmentObserver<ExploreTracksFragment, Page<SuggestedTracksCollection>>(this));
    }

    private void loadTrackSuggestions(Observer<Page<SuggestedTracksCollection>> fragmentObserver) {
        setEmptyViewStatus(EmptyListView.Status.WAITING);
        final ConnectableObservable<Page<SuggestedTracksCollection>> suggestedTracks = buildGetSuggestedTracksObservable();
        suggestedTracks.subscribe(mExploreTracksAdapter);
        suggestedTracks.subscribe(fragmentObserver);
        mSubscription = suggestedTracks.connect();
    }

    private ConnectableObservable<Page<SuggestedTracksCollection>> buildGetSuggestedTracksObservable() {
        final ExploreTracksCategory category = getArguments().getParcelable(ExploreTracksCategory.EXTRA);
        ConnectableObservable<Page<SuggestedTracksCollection>> observable = new ExploreTracksOperations().getSuggestedTracks(category)
                .observeOn(AndroidSchedulers.mainThread())
                .publish();
        return observable;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.suggested_tracks_fragment, container, false);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        final Track track = new Track(mExploreTracksAdapter.getItem(position));
        new PlayUtils(getActivity()).playExploreTrack(track, "EXPLORE-TAG");
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mEmptyListView = (EmptyListView) view.findViewById(android.R.id.empty);
        mEmptyListView.setStatus(mEmptyViewStatus);
        mEmptyListView.setOnRetryListener(new EmptyListView.RetryListener() {
            @Override
            public void onEmptyViewRetry() {
                loadTrackSuggestions(
                        new ListFragmentObserver<ExploreTracksFragment, Page<SuggestedTracksCollection>>(ExploreTracksFragment.this));
            }
        });

        PullToRefreshGridView ptrGridView = (PullToRefreshGridView) view.findViewById(GRID_VIEW_ID);
        ptrGridView.setOnRefreshListener(this);
        GridView gridView = ptrGridView.getRefreshableView();
        gridView.setOnItemClickListener(this);
        gridView.setAdapter(mExploreTracksAdapter);
        gridView.setEmptyView(mEmptyListView);

        // make sure this is called /after/ setAdapter, since the listener requires an EndlessPagingAdapter to be set
        gridView.setOnScrollListener(new AbsListViewParallaxer(new PauseOnScrollListener(ImageLoader.getInstance(), false, true, mExploreTracksAdapter)));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mSubscription.unsubscribe();
    }

    @Override
    public void onRefresh(PullToRefreshBase<GridView> refreshView) {
        loadTrackSuggestions(
                new PullToRefreshObserver<ExploreTracksFragment>(this, GRID_VIEW_ID, mExploreTracksAdapter));
    }

    @Override
    public void setEmptyViewStatus(int status) {
        mEmptyViewStatus = status;
        if (mEmptyListView != null) {
            mEmptyListView.setStatus(status);
        }
    }

    /**
     * TODO: REPLACE ME
     */
    private static class PullToRefreshObserver<FragmentType extends Fragment>
            extends BufferingObserver<Page<SuggestedTracksCollection>> {

        private ItemAdapter<?> mAdapter;

        public PullToRefreshObserver(FragmentType fragment, int ptrViewId, ExploreTracksAdapter adapter) {
            super(new InnerObserver<FragmentType>(fragment, ptrViewId, adapter));
            mAdapter = adapter;
        }

        @Override
        public void onCompleted() {
            mAdapter.clear();
            mAdapter.notifyDataSetChanged();
            super.onCompleted();
        }

        // receives the actual observer calls from the outer buffering observer
        private static final class InnerObserver<FragmentType extends Fragment>
                extends RxFragmentObserver<FragmentType, Page<SuggestedTracksCollection>> {

            private final int mPtrViewId;
            private ExploreTracksAdapter mDelegate;

            public InnerObserver(FragmentType fragment, int ptrViewId, ExploreTracksAdapter adapter) {
                super(fragment);
                mPtrViewId = ptrViewId;
                mDelegate = adapter;
            }

            @Override
            public void onNext(FragmentType fragment, Page<SuggestedTracksCollection> item) {
                mDelegate.onNext(item);
            }

            @Override
            public void onCompleted(FragmentType fragment) {
                mDelegate.onCompleted();
                findPullToRefreshView(fragment).onRefreshComplete();
            }

            @Override
            public void onError(FragmentType fragment, Throwable error) {
                mDelegate.onError(error);
                findPullToRefreshView(fragment).onRefreshComplete();
            }

            private PullToRefreshBase<?> findPullToRefreshView(FragmentType fragment) {
                return (PullToRefreshBase<?>) fragment.getView().findViewById(mPtrViewId);
            }
        }
    }
}
