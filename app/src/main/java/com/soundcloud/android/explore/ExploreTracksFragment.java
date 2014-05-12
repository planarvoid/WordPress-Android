package com.soundcloud.android.explore;

import static rx.android.OperatorPaged.Page;
import static rx.android.schedulers.AndroidSchedulers.mainThread;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.actionbar.PullToRefreshController;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.image.ImageOperations;
import com.soundcloud.android.model.ExploreGenre;
import com.soundcloud.android.model.SuggestedTracksCollection;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.playback.PlaybackOperations;
import com.soundcloud.android.rx.observers.EmptyViewAware;
import com.soundcloud.android.rx.observers.ListFragmentSubscriber;
import com.soundcloud.android.utils.AbsListViewParallaxer;
import com.soundcloud.android.view.EmptyListView;
import rx.Subscriber;
import rx.Subscription;
import rx.observables.ConnectableObservable;
import rx.subscriptions.Subscriptions;
import uk.co.senab.actionbarpulltorefresh.extras.actionbarcompat.PullToRefreshLayout;
import uk.co.senab.actionbarpulltorefresh.library.listeners.OnRefreshListener;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
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

    private int emptyViewStatus = EmptyListView.Status.WAITING;
    private EmptyListView emptyView;
    private String lastExploreTag;

    @Inject
    ExploreTracksAdapter adapter;

    @Inject
    PlaybackOperations playbackOperations;

    @Inject
    ImageOperations imageOperations;

    @Inject
    ExploreTracksOperations exploreTracksOperations;

    @Inject
    PullToRefreshController pullToRefreshController;

    private ConnectableObservable<Page<SuggestedTracksCollection>> suggestedTracksObservable;
    private Subscription subscription = Subscriptions.empty();

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

    @VisibleForTesting
    ExploreTracksFragment(ExploreTracksAdapter adapter, PlaybackOperations playbackOperations, ImageOperations imageOperations,
                          ExploreTracksOperations exploreTracksOperations, PullToRefreshController pullToRefreshController) {
        this.adapter = adapter;
        this.playbackOperations = playbackOperations;
        this.imageOperations = imageOperations;
        this.exploreTracksOperations = exploreTracksOperations;
        this.pullToRefreshController = pullToRefreshController;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        suggestedTracksObservable = buildSuggestedTracksObservable();
        loadTrackSuggestions(suggestedTracksObservable, new ExploreTracksSubscriber());
    }

    @Override
    public void onRefreshStarted(View view) {
        final ConnectableObservable<Page<SuggestedTracksCollection>> refreshObservable = buildSuggestedTracksObservable();
        loadTrackSuggestions(refreshObservable, new PullToRefreshSubscriber(refreshObservable));
    }

    private ConnectableObservable<Page<SuggestedTracksCollection>> buildSuggestedTracksObservable() {
        final ExploreGenre category = getExploreCategory();
        return exploreTracksOperations.getSuggestedTracks(category).observeOn(mainThread()).replay();
    }

    private ExploreGenre getExploreCategory() {
        return getArguments().getParcelable(ExploreGenre.EXPLORE_GENRE_EXTRA);
    }

    private void loadTrackSuggestions(ConnectableObservable<Page<SuggestedTracksCollection>> observable,
                                      Subscriber<Page<SuggestedTracksCollection>> fragmentObserver) {
        setEmptyViewStatus(EmptyListView.Status.WAITING);
        observable.subscribe(adapter);
        observable.subscribe(fragmentObserver);
        subscription = observable.connect();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.suggested_tracks_fragment, container, false);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        final Track track = new Track(adapter.getItem(position));
        final String screenTagExtra = getArguments().getString(SCREEN_TAG_EXTRA);
        playbackOperations.playExploreTrack(getActivity(), track, lastExploreTag, screenTagExtra);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        emptyView = (EmptyListView) view.findViewById(android.R.id.empty);
        emptyView.setStatus(emptyViewStatus);
        emptyView.setOnRetryListener(new EmptyListView.RetryListener() {
            @Override
            public void onEmptyViewRetry() {
                suggestedTracksObservable = buildSuggestedTracksObservable();
                loadTrackSuggestions(suggestedTracksObservable, new ExploreTracksSubscriber());
            }
        });

        GridView gridView = (GridView) view.findViewById(GRID_VIEW_ID);
        gridView.setOnItemClickListener(this);
        gridView.setAdapter(adapter);
        gridView.setEmptyView(emptyView);

        // Make sure this is called /after/ setAdapter, since the listener requires an EndlessPagingAdapter to be set
        gridView.setOnScrollListener(new AbsListViewParallaxer(imageOperations.createScrollPauseListener(false, true, adapter)));

        setupPullToRefresh(view);
    }

    private void setupPullToRefresh(View view) {
        // Work around for child fragment issue where getActivity() returns previous instance after rotate
        FragmentActivity actionBarOwner = getParentFragment() == null ? getActivity() : getParentFragment().getActivity();
        PullToRefreshLayout pullToRefreshLayout = (PullToRefreshLayout) view.findViewById(R.id.ptr_layout);
        pullToRefreshController.attach(actionBarOwner, pullToRefreshLayout, this);
    }

    @Override
    public void onDestroyView() {
        pullToRefreshController.detach();
        ((GridView) getView().findViewById(GRID_VIEW_ID)).setAdapter(null);
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        subscription.unsubscribe();
        super.onDestroy();
    }

    @Override
    public void setEmptyViewStatus(int status) {
        emptyViewStatus = status;
        if (emptyView != null) {
            emptyView.setStatus(status);
        }
    }

    private final class ExploreTracksSubscriber extends ListFragmentSubscriber<Page<SuggestedTracksCollection>> {

        private ExploreTracksSubscriber() {
            super(ExploreTracksFragment.this);
        }

        @Override
        public void onNext(Page<SuggestedTracksCollection> element) {
            lastExploreTag = element.getPagedCollection().getTrackingTag();
        }
    }

    /**
     * TODO: REPLACE ME
     * Turn this into an RX operator if possible
     */
    private final class PullToRefreshSubscriber extends ListFragmentSubscriber<Page<SuggestedTracksCollection>> {

        private final ConnectableObservable<Page<SuggestedTracksCollection>> newObservable;
        private Page<SuggestedTracksCollection> refreshedPage;

        public PullToRefreshSubscriber(ConnectableObservable<Page<SuggestedTracksCollection>> newObservable) {
            super(ExploreTracksFragment.this);
            this.newObservable = newObservable;
        }

        @Override
        public void onCompleted() {
            adapter.clear();
            adapter.notifyDataSetChanged();
            adapter.onNext(refreshedPage);
            adapter.onCompleted();
            suggestedTracksObservable = newObservable;
            hidePullToRefreshView();
        }

        @Override
        public void onError(Throwable error) {
            super.onError(error);
            hidePullToRefreshView();
        }

        @Override
        public void onNext(Page<SuggestedTracksCollection> page) {
            refreshedPage = page;
        }

        private void hidePullToRefreshView() {
            pullToRefreshController.stopRefreshing();
        }
    }
}
