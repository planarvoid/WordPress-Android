package com.soundcloud.android.explore;

import static rx.android.schedulers.AndroidSchedulers.mainThread;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.actionbar.PullToRefreshController;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.playback.ExpandPlayerSubscriber;
import com.soundcloud.android.playback.PlaySessionSource;
import com.soundcloud.android.playback.PlaybackInitiator;
import com.soundcloud.android.presentation.PagingListItemAdapter;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.utils.AbsListViewParallaxer;
import com.soundcloud.android.view.ListViewController;
import com.soundcloud.android.view.RefreshableListComponent;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.lightcycle.LightCycle;
import com.soundcloud.lightcycle.LightCycleSupportFragment;
import rx.Subscription;
import rx.functions.Action1;
import rx.observables.ConnectableObservable;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.support.annotation.VisibleForTesting;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.List;

@SuppressLint("ValidFragment")
public class ExploreTracksFragment extends LightCycleSupportFragment<ExploreTracksFragment>
        implements RefreshableListComponent<ConnectableObservable<List<TrackItem>>> {

    static final String SCREEN_TAG_EXTRA = "screen_tag";

    private String trackingTag;

    @Inject PagingListItemAdapter<TrackItem> adapter;
    @Inject PlaybackInitiator playbackInitiator;
    @Inject ExploreTracksOperations operations;
    @Inject @LightCycle PullToRefreshController pullToRefreshController;
    @Inject @LightCycle ListViewController listViewController;
    @Inject Provider<ExpandPlayerSubscriber> subscriberProvider;

    private ConnectableObservable<List<TrackItem>> observable;
    private Subscription connectionSubscription = RxUtils.invalidSubscription();

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
        init();
    }

    @VisibleForTesting
    ExploreTracksFragment(PagingListItemAdapter<TrackItem> adapter,
                          PlaybackInitiator playbackInitiator,
                          ExploreTracksOperations operations,
                          PullToRefreshController pullToRefreshController,
                          ListViewController listViewController,
                          Provider<ExpandPlayerSubscriber> subscriberProvider) {
        this.adapter = adapter;
        this.playbackInitiator = playbackInitiator;
        this.operations = operations;
        this.pullToRefreshController = pullToRefreshController;
        this.listViewController = listViewController;
        this.subscriberProvider = subscriberProvider;
        init();
    }

    private void init() {
        this.listViewController.setAdapter(this.adapter,
                                           this.operations.pager(),
                                           TrackItem.<SuggestedTracksCollection>fromApiTracks());
        this.listViewController.setScrollListener(new AbsListViewParallaxer(null));
        this.pullToRefreshController.setRefreshListener(this, this.adapter);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ConnectableObservable<List<TrackItem>> observable = buildObservable();
        observable.subscribe(adapter);
        connectObservable(observable);
    }

    @Override
    public ConnectableObservable<List<TrackItem>> buildObservable() {
        final ExploreGenre category = getArguments().getParcelable(ExploreGenre.EXPLORE_GENRE_EXTRA);

        return operations.pager().page(operations.getSuggestedTracks(category))
                 .doOnNext(new Action1<SuggestedTracksCollection>() {
                      @Override
                      public void call(SuggestedTracksCollection page) {
                          trackingTag = page.getTrackingTag();
                      }
                  })
                 .map(TrackItem.<SuggestedTracksCollection>fromApiTracks())
                 .observeOn(mainThread()).replay();
    }

    @Override
    public ConnectableObservable<List<TrackItem>> refreshObservable() {
        return buildObservable();
    }

    @Override
    public Subscription connectObservable(ConnectableObservable<List<TrackItem>> observable) {
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
        final String screenTagExtra = getArguments().getString(SCREEN_TAG_EXTRA);
        final PlaySessionSource playSessionSource = PlaySessionSource.forExplore(screenTagExtra, trackingTag);
        playbackInitiator.playTracks(Lists.transform(adapter.getItems(), TrackItem.TO_URN), position, playSessionSource)
                         .subscribe(subscriberProvider.get());
    }

    @Override
    public void onDestroy() {
        connectionSubscription.unsubscribe();
        super.onDestroy();
    }

}
