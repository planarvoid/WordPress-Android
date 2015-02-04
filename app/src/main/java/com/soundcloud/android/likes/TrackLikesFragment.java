package com.soundcloud.android.likes;

import static rx.android.schedulers.AndroidSchedulers.mainThread;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.actionbar.PullToRefreshController;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.OfflineSyncEvent;
import com.soundcloud.android.lightcycle.LightCycleFragment;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.ExpandPlayerSubscriber;
import com.soundcloud.android.playback.PlaybackOperations;
import com.soundcloud.android.playback.service.PlaySessionSource;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.android.view.ListViewController;
import com.soundcloud.android.view.RefreshableListComponent;
import com.soundcloud.propeller.PropertySet;
import rx.Observable;
import rx.Subscription;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.observables.ConnectableObservable;
import rx.subscriptions.Subscriptions;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.List;

@SuppressLint("ValidFragment")
@SuppressWarnings({"PMD.TooManyFields"})
public class TrackLikesFragment extends LightCycleFragment
        implements RefreshableListComponent<ConnectableObservable<List<PropertySet>>> {

    @Inject TrackLikesAdapter adapter;
    @Inject LikeOperations likeOperations;
    @Inject PlaybackOperations playbackOperations;
    @Inject TrackLikesHeaderController headerController;
    @Inject ListViewController listViewController;
    @Inject PullToRefreshController pullToRefreshController;
    @Inject TrackLikesActionMenuController actionMenuController;
    @Inject Provider<ExpandPlayerSubscriber> expandPlayerSubscriberProvider;
    @Inject EventBus eventBus;

    private ConnectableObservable<List<PropertySet>> observable;
    private ConnectableObservable<List<Urn>> allTrackUrnsObservable;
    private Subscription connectionSubscription = Subscriptions.empty();
    private Subscription allTrackUrnsSubscription = Subscriptions.empty();
    private Subscription syncQueueUpdatedSubscription = Subscriptions.empty();

    private final Action1<List<PropertySet>> buildLoadAllTrackUrns = new Action1<List<PropertySet>>() {
        @Override
        public void call(List<PropertySet> propertySets) {
            buildLoadAllTrackUrnsObservable();
        }
    };
    private final Func1<OfflineSyncEvent, Boolean> isQueueUpdateEvent = new Func1<OfflineSyncEvent, Boolean>() {
        @Override
        public Boolean call(OfflineSyncEvent offlineSyncEvent) {
            return offlineSyncEvent.getKind() == OfflineSyncEvent.QUEUE_UPDATED;
        }
    };

    public TrackLikesFragment() {
        setRetainInstance(true);
        setHasOptionsMenu(true);
        SoundCloudApplication.getObjectGraph().inject(this);
        addLifeCycleComponents();
    }

    @VisibleForTesting
    TrackLikesFragment(TrackLikesAdapter adapter,
                       LikeOperations likeOperations,
                       ListViewController listViewController,
                       PullToRefreshController pullToRefreshController,
                       TrackLikesHeaderController headerController,
                       PlaybackOperations playbackOperations,
                       TrackLikesActionMenuController actionMenuController,
                       Provider<ExpandPlayerSubscriber> expandPlayerSubscriberProvider,
                       EventBus eventBus) {
        this.adapter = adapter;
        this.likeOperations = likeOperations;
        this.listViewController = listViewController;
        this.pullToRefreshController = pullToRefreshController;
        this.headerController = headerController;
        this.playbackOperations = playbackOperations;
        this.actionMenuController = actionMenuController;
        this.expandPlayerSubscriberProvider = expandPlayerSubscriberProvider;
        this.eventBus = eventBus;
        addLifeCycleComponents();
    }

    private void addLifeCycleComponents() {
        listViewController.setAdapter(adapter, likeOperations.likedTracksPager());
        pullToRefreshController.setRefreshListener(this, adapter);

        addLifeCycleComponent(headerController);
        addLifeCycleComponent(listViewController);
        addLifeCycleComponent(pullToRefreshController);
        addLifeCycleComponent(adapter.getLifeCycleHandler());
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setRetainInstance(true);
        setHasOptionsMenu(true);
        super.onCreate(savedInstanceState);
        buildLoadAllTrackUrnsObservable();
        connectObservable(buildObservable());
        syncQueueUpdatedSubscription = eventBus.queue(EventQueue.OFFLINE_SYNC)
                .filter(isQueueUpdateEvent)
                .subscribe(new OfflineSyncQueueUpdated());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.default_list_with_refresh, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        listViewController.setHeaderViewPresenter(headerController.getPresenter());
        listViewController.getEmptyView().setImage(R.drawable.empty_like);
        listViewController.getEmptyView().setMessageText(R.string.list_empty_user_likes_message);

        listViewController.connect(this, observable);
        pullToRefreshController.connect(observable, adapter);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        actionMenuController.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return actionMenuController.onOptionsItemSelected(this, item);
    }

    @Override
    public void onResume() {
        super.onResume();
        actionMenuController.onResume(this);
    }

    @Override
    public void onPause() {
        actionMenuController.onPause();
        super.onPause();
    }

    @Override
    public void onDestroy() {
        connectionSubscription.unsubscribe();
        allTrackUrnsSubscription.unsubscribe();
        syncQueueUpdatedSubscription.unsubscribe();
        super.onDestroy();
    }

    @Override
    public ConnectableObservable<List<PropertySet>> buildObservable() {
        ConnectableObservable<List<PropertySet>> listConnectableObservable = pagedObservable(likeOperations.likedTracks());
        listConnectableObservable.first().subscribe(new ConnectAllTrackUrnsSubscriber());
        return listConnectableObservable;
    }

    @Override
    public ConnectableObservable<List<PropertySet>> refreshObservable() {
        final ConnectableObservable<List<PropertySet>> refreshObservable = pagedObservable(likeOperations.updatedLikedTracks());
        refreshObservable.first().doOnNext(buildLoadAllTrackUrns).subscribe(new ConnectAllTrackUrnsSubscriber());
        return refreshObservable;
    }

    private ConnectableObservable<List<PropertySet>> pagedObservable(Observable<List<PropertySet>> source) {
        final ConnectableObservable<List<PropertySet>> observable =
                likeOperations.likedTracksPager().page(source).observeOn(mainThread()).replay();
        observable.subscribe(adapter);
        return observable;
    }

    @Override
    public Subscription connectObservable(ConnectableObservable<List<PropertySet>> observable) {
        this.observable = observable;
        this.connectionSubscription.unsubscribe();
        this.connectionSubscription = observable.connect();
        return connectionSubscription;
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        // here we assume that the list you are looking at is up to date with the database, which is not necessarily the case
        // a sync may have happened in the background. This is def. an edge case, but worth handling maybe??
        Urn initialTrack = ((PropertySet) adapterView.getItemAtPosition(position)).get(TrackProperty.URN);
        PlaySessionSource playSessionSource = new PlaySessionSource(Screen.SIDE_MENU_LIKES);
        playbackOperations
                .playTracks(allTrackUrnsObservable, initialTrack, position, playSessionSource)
                .subscribe(expandPlayerSubscriberProvider.get());
    }

    private void buildLoadAllTrackUrnsObservable() {
        allTrackUrnsObservable = likeOperations.likedTrackUrns().observeOn(mainThread()).replay();
    }

    private void connectLoadAllTrackUrns() {
        allTrackUrnsObservable.subscribe(new AllLikedTrackUrnsSubscriber());
        allTrackUrnsSubscription.unsubscribe();
        allTrackUrnsSubscription = allTrackUrnsObservable.connect();
    }

    private class ConnectAllTrackUrnsSubscriber extends DefaultSubscriber<List<PropertySet>> {
        @Override
        public void onNext(List<PropertySet> propertySetPage) {
            connectLoadAllTrackUrns();
        }
    }

    private class AllLikedTrackUrnsSubscriber extends DefaultSubscriber<List<Urn>> {
        @Override
        public void onNext(List<Urn> likedTrackUrns) {
            headerController.setLikedTrackUrns(likedTrackUrns);
        }
    }

    private class OfflineSyncQueueUpdated extends DefaultSubscriber<OfflineSyncEvent> {
        @Override
        public void onNext(OfflineSyncEvent ignored) {
            adapter.clear();
            connectObservable(buildObservable());
        }
    }
}
