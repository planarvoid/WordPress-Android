package com.soundcloud.android.explore;

import static rx.android.OperationPaged.Page;

import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.handmark.pulltorefresh.library.PullToRefreshGridView;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.PauseOnScrollListener;
import com.soundcloud.android.R;
import com.soundcloud.android.dagger.DaggerDependencyInjector;
import com.soundcloud.android.dagger.DependencyInjector;
import com.soundcloud.android.model.ExploreGenre;
import com.soundcloud.android.model.SuggestedTracksCollection;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.playback.PlaybackOperations;
import com.soundcloud.android.rx.observers.EmptyViewAware;
import com.soundcloud.android.rx.observers.ListFragmentObserver;
import com.soundcloud.android.utils.AbsListViewParallaxer;
import com.soundcloud.android.view.EmptyListView;
import rx.Observer;
import rx.Subscription;
import rx.android.AndroidObservables;
import rx.observables.ConnectableObservable;
import rx.subscriptions.Subscriptions;

import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;

import javax.inject.Inject;

public class ExploreTracksFragment extends Fragment implements AdapterView.OnItemClickListener,
        EmptyViewAware, PullToRefreshBase.OnRefreshListener<GridView> {

    private static final int GRID_VIEW_ID = R.id.suggested_tracks_grid;
    private int mEmptyViewStatus = EmptyListView.Status.WAITING;

    private EmptyListView mEmptyListView;

    private ExploreTracksObserver mObserver;

    @Inject
    ExploreTracksAdapter mAdapter;

    @Inject
    PlaybackOperations mPlaybackOperations;

    private ConnectableObservable<Page<SuggestedTracksCollection>> mSuggestedTracksObservable;
    private Subscription mSubscription = Subscriptions.empty();
    private DependencyInjector mDependencyInjector;

    public static ExploreTracksFragment create(ExploreGenre category) {
        final ExploreTracksFragment exploreTracksFragment = new ExploreTracksFragment();
        Bundle args = new Bundle();
        args.putParcelable(ExploreGenre.EXPLORE_GENRE_EXTRA, category);
        exploreTracksFragment.setArguments(args);
        return exploreTracksFragment;
    }

    public ExploreTracksFragment() {
        this(new DaggerDependencyInjector());
    }

    public ExploreTracksFragment(DependencyInjector dependencyInjector) {
        mDependencyInjector = dependencyInjector;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mDependencyInjector.inject(this);
        mObserver = new ExploreTracksObserver();

        mSuggestedTracksObservable = buildSuggestedTracksObservable();
        loadTrackSuggestions(mSuggestedTracksObservable, mObserver);
    }

    private ConnectableObservable<Page<SuggestedTracksCollection>> buildSuggestedTracksObservable() {
        final ExploreGenre category = getExploreCategory();
        final ExploreTracksOperations operations = new ExploreTracksOperations();
        return AndroidObservables.fromFragment(this, operations.getSuggestedTracks(category)).replay();
    }

    private ExploreGenre getExploreCategory() {
        return getArguments().getParcelable(ExploreGenre.EXPLORE_GENRE_EXTRA);
    }

    private void loadTrackSuggestions(ConnectableObservable<Page<SuggestedTracksCollection>> observable,
                                      Observer<Page<SuggestedTracksCollection>> fragmentObserver) {
        setEmptyViewStatus(EmptyListView.Status.WAITING);
        observable.subscribe(mAdapter);
        observable.subscribe(fragmentObserver);
        mSubscription = observable.connect();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.suggested_tracks_fragment, container, false);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        final Track track = new Track(mAdapter.getItem(position));
        mPlaybackOperations.playExploreTrack(getActivity(), track, mObserver.getLastExploreTag(),
                Uri.parse("explore:" + getExploreCategory().getTitle())); // todo, no hardcoding tags once we implement full eventlogger
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mEmptyListView = (EmptyListView) view.findViewById(android.R.id.empty);
        mEmptyListView.setStatus(mEmptyViewStatus);
        mEmptyListView.setOnRetryListener(new EmptyListView.RetryListener() {
            @Override
            public void onEmptyViewRetry() {
                mSuggestedTracksObservable = buildSuggestedTracksObservable();
                loadTrackSuggestions(mSuggestedTracksObservable, mObserver);
            }
        });

        PullToRefreshGridView ptrGridView = (PullToRefreshGridView) view.findViewById(GRID_VIEW_ID);
        ptrGridView.setOnRefreshListener(this);
        GridView gridView = ptrGridView.getRefreshableView();
        gridView.setOnItemClickListener(this);
        gridView.setAdapter(mAdapter);
        gridView.setEmptyView(mEmptyListView);

        // make sure this is called /after/ setAdapter, since the listener requires an EndlessPagingAdapter to be set
        gridView.setOnScrollListener(new AbsListViewParallaxer(new PauseOnScrollListener(ImageLoader.getInstance(), false, true, mAdapter)));
    }

    @Override
    public void onDestroyView() {
        ((PullToRefreshGridView) getView().findViewById(GRID_VIEW_ID)).setAdapter(null);
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        mSubscription.unsubscribe();
        super.onDestroy();
    }

    @Override
    public void onRefresh(PullToRefreshBase<GridView> refreshView) {
        final ConnectableObservable<Page<SuggestedTracksCollection>> refreshObservable = buildSuggestedTracksObservable();
        loadTrackSuggestions(refreshObservable, new PullToRefreshObserver(refreshObservable));
    }

    @Override
    public void setEmptyViewStatus(int status) {
        mEmptyViewStatus = status;
        if (mEmptyListView != null) {
            mEmptyListView.setStatus(status);
        }
    }

    private final class ExploreTracksObserver extends ListFragmentObserver<Page<SuggestedTracksCollection>> {

        private String mLastExploreTag;

        private ExploreTracksObserver() {
            super(ExploreTracksFragment.this);
        }

        @Override
        public void onNext(Page<SuggestedTracksCollection> element) {
            mLastExploreTag = element.getPagedCollection().getTrackingTag();
        }

        private String getLastExploreTag() {
            return mLastExploreTag;
        }
    }

    /**
     * TODO: REPLACE ME
     * Turn this into an RX operator if possible
     */
    private final class PullToRefreshObserver implements Observer<Page<SuggestedTracksCollection>> {

        private final ConnectableObservable<Page<SuggestedTracksCollection>> mNewObservable;
        private Page<SuggestedTracksCollection> mRefreshedPage;

        public PullToRefreshObserver(ConnectableObservable<Page<SuggestedTracksCollection>> newObservable) {
            mNewObservable = newObservable;
        }

        @Override
        public void onCompleted() {
            mAdapter.clear();
            mAdapter.notifyDataSetChanged();
            mAdapter.onNext(mRefreshedPage);
            mAdapter.onCompleted();
            mSuggestedTracksObservable = mNewObservable;
            hidePullToRefreshView();
        }

        @Override
        public void onError(Throwable error) {
            hidePullToRefreshView();
        }

        @Override
        public void onNext(Page<SuggestedTracksCollection> page) {
            mRefreshedPage = page;
        }

        private void hidePullToRefreshView() {
            ((PullToRefreshGridView) getView().findViewById(GRID_VIEW_ID)).onRefreshComplete();
        }
    }
}
