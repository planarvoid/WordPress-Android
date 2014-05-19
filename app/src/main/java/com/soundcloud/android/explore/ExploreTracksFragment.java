package com.soundcloud.android.explore;

import static rx.android.OperatorPaged.Page;
import static rx.android.schedulers.AndroidSchedulers.mainThread;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.actionbar.PullToRefreshController;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.model.ExploreGenre;
import com.soundcloud.android.model.SuggestedTracksCollection;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.playback.PlaybackOperations;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.utils.AbsListViewParallaxer;
import com.soundcloud.android.view.ListViewController;
import com.soundcloud.android.view.ReactiveListComponent;
import rx.Subscription;
import rx.observables.ConnectableObservable;
import rx.subscriptions.Subscriptions;
import uk.co.senab.actionbarpulltorefresh.extras.actionbarcompat.PullToRefreshLayout;
import uk.co.senab.actionbarpulltorefresh.library.listeners.OnRefreshListener;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;

import javax.inject.Inject;

public class ExploreTracksFragment extends Fragment
        implements ReactiveListComponent<ConnectableObservable<Page<SuggestedTracksCollection>>>,
        OnRefreshListener {

    static final String SCREEN_TAG_EXTRA = "screen_tag";

    private String lastExploreTag;

    @Inject
    ExploreTracksAdapter adapter;
    @Inject
    PlaybackOperations playbackOperations;
    @Inject
    ExploreTracksOperations exploreTracksOperations;
    @Inject
    PullToRefreshController pullToRefreshController;
    @Inject
    ListViewController listViewController;

    private ConnectableObservable<Page<SuggestedTracksCollection>> observable;
    private Subscription connectionSubscription = Subscriptions.empty();

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
        connectObservable(buildObservable());
    }

    @Override
    public ConnectableObservable<Page<SuggestedTracksCollection>> buildObservable() {
        final ExploreGenre category = getArguments().getParcelable(ExploreGenre.EXPLORE_GENRE_EXTRA);
        return exploreTracksOperations.getSuggestedTracks(category).observeOn(mainThread()).replay();
    }

    @Override
    public Subscription connectObservable(ConnectableObservable<Page<SuggestedTracksCollection>> observable) {
        this.observable = observable;
        this.observable.subscribe(adapter);
        this.observable.subscribe(new ExploreTracksSubscriber());
        connectionSubscription = this.observable.connect();
        return connectionSubscription;
    }

    @Override
    public void onRefreshStarted(View view) {
        observable = buildObservable();
        observable.subscribe(new PullToRefreshSubscriber());
        observable.subscribe(new ExploreTracksSubscriber());
        connectionSubscription = observable.connect();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.suggested_tracks_fragment, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        listViewController.onViewCreated(this, observable, view, adapter, new AbsListViewParallaxer(adapter));

        setupPullToRefresh(view);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        final Track track = new Track(adapter.getItem(position));
        final String screenTagExtra = getArguments().getString(SCREEN_TAG_EXTRA);
        playbackOperations.playExploreTrack(getActivity(), track, lastExploreTag, screenTagExtra);
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
        listViewController.onDestroyView();
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        connectionSubscription.unsubscribe();
        super.onDestroy();
    }

    private final class ExploreTracksSubscriber extends DefaultSubscriber<Page<SuggestedTracksCollection>> {

        @Override
        public void onNext(Page<SuggestedTracksCollection> element) {
            lastExploreTag = element.getPagedCollection().getTrackingTag();
        }
    }

    /**
     * TODO: REPLACE ME
     * Turn this into an RX operator if possible
     */
    private final class PullToRefreshSubscriber extends DefaultSubscriber<Page<SuggestedTracksCollection>> {

        private Page<SuggestedTracksCollection> refreshedPage;

        @Override
        public void onCompleted() {
            adapter.clear();
            adapter.notifyDataSetChanged();
            adapter.onNext(refreshedPage);
            adapter.onCompleted();
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
