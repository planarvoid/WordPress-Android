package com.soundcloud.android.explore;

import static rx.android.schedulers.AndroidSchedulers.mainThread;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.actionbar.PullToRefreshController;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.api.legacy.model.PublicApiTrack;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.lightcycle.LightCycleFragment;
import com.soundcloud.android.playback.ExpandPlayerSubscriber;
import com.soundcloud.android.playback.PlaybackOperations;
import com.soundcloud.android.playback.service.PlaySessionSource;
import com.soundcloud.android.utils.AbsListViewParallaxer;
import com.soundcloud.android.view.ListViewController;
import com.soundcloud.android.view.RefreshableListComponent;
import com.soundcloud.android.view.adapters.EndlessAdapter;
import rx.Subscription;
import rx.functions.Action1;
import rx.observables.ConnectableObservable;
import rx.subscriptions.Subscriptions;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;

import javax.inject.Inject;
import javax.inject.Provider;

@SuppressLint("ValidFragment")
public class ExploreTracksFragment extends LightCycleFragment
        implements RefreshableListComponent<ConnectableObservable<SuggestedTracksCollection>> {

    static final String SCREEN_TAG_EXTRA = "screen_tag";

    private String trackingTag;

    @Inject EndlessAdapter<ApiTrack> adapter;
    @Inject PlaybackOperations playbackOperations;
    @Inject ExploreTracksOperations operations;
    @Inject PullToRefreshController pullToRefreshController;
    @Inject ListViewController listViewController;
    @Inject Provider<ExpandPlayerSubscriber> subscriberProvider;

    private ConnectableObservable<SuggestedTracksCollection> observable;
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
        addLifeCycleComponents();
    }

    @VisibleForTesting
    ExploreTracksFragment(EndlessAdapter<ApiTrack> adapter,
                          PlaybackOperations playbackOperations,
                          ExploreTracksOperations operations,
                          PullToRefreshController pullToRefreshController,
                          ListViewController listViewController,
                          Provider<ExpandPlayerSubscriber> subscriberProvider) {
        this.adapter = adapter;
        this.playbackOperations = playbackOperations;
        this.operations = operations;
        this.pullToRefreshController = pullToRefreshController;
        this.listViewController = listViewController;
        this.subscriberProvider = subscriberProvider;
        addLifeCycleComponents();
    }

    private void addLifeCycleComponents() {
        listViewController.setAdapter(adapter, operations.pager());
        listViewController.setScrollListener(new AbsListViewParallaxer(null));
        pullToRefreshController.setRefreshListener(this, adapter);
        addLifeCycleComponent(this.listViewController);
        addLifeCycleComponent(this.pullToRefreshController);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        connectObservable(buildObservable());
    }

    @Override
    public ConnectableObservable<SuggestedTracksCollection> buildObservable() {
        final ExploreGenre category = getArguments().getParcelable(ExploreGenre.EXPLORE_GENRE_EXTRA);
        final ConnectableObservable<SuggestedTracksCollection> observable =
                operations.pager().page(operations.getSuggestedTracks(category)
                .doOnNext(new Action1<SuggestedTracksCollection>() {
                    @Override
                    public void call(SuggestedTracksCollection page) {
                        trackingTag = page.getTrackingTag();
                    }
                }))
                .observeOn(mainThread()).replay();
        observable.subscribe(adapter);
        return observable;
    }

    @Override
    public ConnectableObservable<SuggestedTracksCollection> refreshObservable() {
        return buildObservable();
    }

    @Override
    public Subscription connectObservable(ConnectableObservable<SuggestedTracksCollection> observable) {
        this.observable = observable;
        connectionSubscription = this.observable.connect();
        return connectionSubscription;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.suggested_tracks_fragment, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        listViewController.connect(this, observable);
        pullToRefreshController.connect(observable, adapter);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        final PublicApiTrack track = new PublicApiTrack(adapter.getItem(position));
        final String screenTagExtra = getArguments().getString(SCREEN_TAG_EXTRA);
        final PlaySessionSource playSessionSource = new PlaySessionSource(screenTagExtra);
        playSessionSource.setExploreVersion(trackingTag);
        playbackOperations
                .playTrackWithRecommendations(track.getUrn(), playSessionSource)
                .subscribe(subscriberProvider.get());
    }

    @Override
    public void onDestroy() {
        connectionSubscription.unsubscribe();
        super.onDestroy();
    }
}
