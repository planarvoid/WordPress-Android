package com.soundcloud.android.explore;

import static rx.android.OperationPaged.Page;
import static rx.android.schedulers.AndroidSchedulers.mainThread;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.ExploreGenre;
import com.soundcloud.android.model.SuggestedTracksCollection;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.playback.PlaybackOperations;
import com.soundcloud.android.rx.observers.EmptyViewAware;
import com.soundcloud.android.rx.observers.ListFragmentSubscriber;
import com.soundcloud.android.utils.AbsListViewParallaxer;
import com.soundcloud.android.utils.ViewUtils;
import com.soundcloud.android.view.EmptyListView;
import rx.Subscriber;
import rx.Subscription;
import rx.observables.ConnectableObservable;
import rx.subscriptions.Subscriptions;
import uk.co.senab.actionbarpulltorefresh.extras.actionbarcompat.PullToRefreshLayout;
import uk.co.senab.actionbarpulltorefresh.library.ActionBarPullToRefresh;
import uk.co.senab.actionbarpulltorefresh.library.listeners.OnRefreshListener;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;

import javax.inject.Inject;

@SuppressLint("ValidFragment")
public class ExploreTracksFragment extends Fragment implements AdapterView.OnItemClickListener,
        EmptyViewAware, OnRefreshListener {

    static final String SCREEN_TAG_EXTRA = "screen_tag";

    private static final int GRID_VIEW_ID = R.id.suggested_tracks_grid;
    private int mEmptyViewStatus = EmptyListView.Status.WAITING;

    private EmptyListView mEmptyListView;
    private String mLastExploreTag;

    @Inject
    ExploreTracksAdapter mAdapter;

    @Inject
    PlaybackOperations mPlaybackOperations;

    @Inject
    ImageOperations mImageOperations;

    private PullToRefreshLayout mPullToRefreshLayout;

    private ConnectableObservable<Page<SuggestedTracksCollection>> mSuggestedTracksObservable;
    private Subscription mSubscription = Subscriptions.empty();

    public static ExploreTracksFragment create(ExploreGenre category, Screen screenTag) {
        final ExploreTracksFragment exploreTracksFragment = new ExploreTracksFragment();
        Bundle args = new Bundle();
        args.putParcelable(ExploreGenre.EXPLORE_GENRE_EXTRA, category);
        args.putString(ExploreTracksFragment.SCREEN_TAG_EXTRA, screenTag.get());
        exploreTracksFragment.setArguments(args);
        return exploreTracksFragment;
    }

    public ExploreTracksFragment() {
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mSuggestedTracksObservable = buildSuggestedTracksObservable();
        loadTrackSuggestions(mSuggestedTracksObservable, new ExploreTracksSubscriber());
    }

    @Override
    public void onRefreshStarted(View view) {
        final ConnectableObservable<Page<SuggestedTracksCollection>> refreshObservable = buildSuggestedTracksObservable();
        loadTrackSuggestions(refreshObservable, new PullToRefreshSubscriber(refreshObservable));
    }

    private ConnectableObservable<Page<SuggestedTracksCollection>> buildSuggestedTracksObservable() {
        final ExploreGenre category = getExploreCategory();
        final ExploreTracksOperations operations = new ExploreTracksOperations();
        return operations.getSuggestedTracks(category).observeOn(mainThread()).replay();
    }

    private ExploreGenre getExploreCategory() {
        return getArguments().getParcelable(ExploreGenre.EXPLORE_GENRE_EXTRA);
    }

    private void loadTrackSuggestions(ConnectableObservable<Page<SuggestedTracksCollection>> observable,
                                      Subscriber<Page<SuggestedTracksCollection>> fragmentObserver) {
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
        final String screenTagExtra = getArguments().getString(SCREEN_TAG_EXTRA);
        mPlaybackOperations.playExploreTrack(getActivity(), track, mLastExploreTag, screenTagExtra);
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
                loadTrackSuggestions(mSuggestedTracksObservable, new ExploreTracksSubscriber());
            }
        });

        GridView gridView = (GridView) view.findViewById(GRID_VIEW_ID);
        gridView.setOnItemClickListener(this);
        gridView.setAdapter(mAdapter);
        gridView.setEmptyView(mEmptyListView);

        // Make sure this is called /after/ setAdapter, since the listener requires an EndlessPagingAdapter to be set
        gridView.setOnScrollListener(new AbsListViewParallaxer(mImageOperations.createScrollPauseListener(false, true, mAdapter)));

        setupPullToRefresh(view);
    }

    private void setupPullToRefresh(View view) {
        // Work around for child fragment issue where getActivity() returns previous instance after rotate
        Activity actionBarOwner = getParentFragment() == null ? getActivity() : getParentFragment().getActivity();

        mPullToRefreshLayout = (PullToRefreshLayout) view.findViewById(R.id.ptr_layout);
        ActionBarPullToRefresh.from(actionBarOwner)
                .allChildrenArePullable()
                .listener(this)
                .setup(mPullToRefreshLayout);
        ViewUtils.stylePtrProgress(actionBarOwner, mPullToRefreshLayout.getHeaderView());
    }

    @Override
    public void onDestroyView() {
        ((GridView) getView().findViewById(GRID_VIEW_ID)).setAdapter(null);
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        mSubscription.unsubscribe();
        super.onDestroy();
    }


    @Override
    public void setEmptyViewStatus(int status) {
        mEmptyViewStatus = status;
        if (mEmptyListView != null) {
            mEmptyListView.setStatus(status);
        }
    }

    private final class ExploreTracksSubscriber extends ListFragmentSubscriber<Page<SuggestedTracksCollection>> {

        private ExploreTracksSubscriber() {
            super(ExploreTracksFragment.this);
        }

        @Override
        public void onNext(Page<SuggestedTracksCollection> element) {
            mLastExploreTag = element.getPagedCollection().getTrackingTag();
        }
    }

    /**
     * TODO: REPLACE ME
     * Turn this into an RX operator if possible
     */
    private final class PullToRefreshSubscriber extends ListFragmentSubscriber<Page<SuggestedTracksCollection>> {

        private final ConnectableObservable<Page<SuggestedTracksCollection>> mNewObservable;
        private Page<SuggestedTracksCollection> mRefreshedPage;

        public PullToRefreshSubscriber(ConnectableObservable<Page<SuggestedTracksCollection>> newObservable) {
            super(ExploreTracksFragment.this);
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
            super.onError(error);
            hidePullToRefreshView();
        }

        @Override
        public void onNext(Page<SuggestedTracksCollection> page) {
            mRefreshedPage = page;
        }

        private void hidePullToRefreshView() {
            mPullToRefreshLayout.setRefreshComplete();
        }
    }
}
