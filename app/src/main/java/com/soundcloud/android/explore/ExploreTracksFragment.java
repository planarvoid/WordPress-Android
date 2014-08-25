package com.soundcloud.android.explore;

import static rx.android.OperatorPaged.Page;
import static rx.android.schedulers.AndroidSchedulers.mainThread;

import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.actionbar.PullToRefreshController;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.api.legacy.model.PublicApiTrack;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PlayerUIEvent;
import com.soundcloud.android.playback.PlaybackOperations;
import com.soundcloud.android.playback.service.PlaySessionSource;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.utils.AbsListViewParallaxer;
import com.soundcloud.android.view.ListViewController;
import com.soundcloud.android.view.RefreshableListComponent;
import com.soundcloud.android.view.adapters.PagingItemAdapter;
import rx.Subscription;
import rx.functions.Action1;
import rx.observables.ConnectableObservable;
import rx.subscriptions.Subscriptions;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;

import javax.inject.Inject;

public class ExploreTracksFragment extends Fragment
        implements RefreshableListComponent<ConnectableObservable<Page<SuggestedTracksCollection>>> {

    static final String SCREEN_TAG_EXTRA = "screen_tag";

    private String trackingTag;

    @Inject PagingItemAdapter<ApiTrack> adapter;
    @Inject PlaybackOperations playbackOperations;
    @Inject ExploreTracksOperations exploreTracksOperations;
    @Inject PullToRefreshController pullToRefreshController;
    @Inject ListViewController listViewController;
    @Inject EventBus eventBus;

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
        // on the initial load or when retrying, we want the adapter subscribed as well
        final ConnectableObservable<Page<SuggestedTracksCollection>> observable = refreshObservable();
        observable.subscribe(adapter);
        return observable;
    }

    @Override
    public ConnectableObservable<Page<SuggestedTracksCollection>> refreshObservable() {
        final ExploreGenre category = getArguments().getParcelable(ExploreGenre.EXPLORE_GENRE_EXTRA);
        return exploreTracksOperations
                .getSuggestedTracks(category)
                .doOnNext(new Action1<Page<SuggestedTracksCollection>>() {
                    @Override
                    public void call(Page<SuggestedTracksCollection> page) {
                        trackingTag = page.getPagedCollection().getTrackingTag();
                    }
                })
                .observeOn(mainThread())
                .replay();
    }

    @Override
    public Subscription connectObservable(ConnectableObservable<Page<SuggestedTracksCollection>> observable) {
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

        listViewController.onViewCreated(this, observable, view, adapter, new AbsListViewParallaxer(adapter));
        pullToRefreshController.onViewCreated(this, observable, adapter);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        final PublicApiTrack track = new PublicApiTrack(adapter.getItem(position));
        final String screenTagExtra = getArguments().getString(SCREEN_TAG_EXTRA);
        final PlaySessionSource playSessionSource = new PlaySessionSource(screenTagExtra);
        playSessionSource.setExploreVersion(trackingTag);
        playbackOperations.playTrackWithRecommendations(track.getUrn(), playSessionSource);
        eventBus.publish(EventQueue.PLAYER_UI, PlayerUIEvent.forExpandPlayer());
    }

    @Override
    public void onDestroyView() {
        pullToRefreshController.onDestroyView();
        listViewController.onDestroyView();
        super.onDestroyView();
    }

    @Override
    public void onDestroy() {
        connectionSubscription.unsubscribe();
        super.onDestroy();
    }
}
