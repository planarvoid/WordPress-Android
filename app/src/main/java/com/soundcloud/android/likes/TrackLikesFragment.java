package com.soundcloud.android.likes;

import static rx.android.schedulers.AndroidSchedulers.mainThread;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.actionbar.PullToRefreshController;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.lightcycle.LightCycleFragment;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.OfflineContentOperations;
import com.soundcloud.android.playback.ExpandPlayerSubscriber;
import com.soundcloud.android.playback.PlaybackOperations;
import com.soundcloud.android.playback.service.PlaySessionSource;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.android.view.ListViewController;
import com.soundcloud.android.view.RefreshableListComponent;
import com.soundcloud.propeller.PropertySet;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
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
public class TrackLikesFragment extends LightCycleFragment
        implements RefreshableListComponent<ConnectableObservable<List<PropertySet>>> {

    @Inject TrackLikesAdapter adapter;
    @Inject LikeOperations likeOperations;
    @Inject PlaybackOperations playbackOperations;
    @Inject ShuffleViewController shuffleViewController;
    @Inject ListViewController listViewController;
    @Inject PullToRefreshController pullToRefreshController;
    @Inject OfflineContentOperations offlineOperations;
    @Inject TrackLikesActionMenuController actionMenuController;
    @Inject Provider<ExpandPlayerSubscriber> expandPlayerSubscriberProvider;

    private ConnectableObservable<List<PropertySet>> observable;
    private Observable<List<Urn>> trackUrnsObservable;
    private Subscription connectionSubscription = Subscriptions.empty();
    private Subscription shuffleSubscription = Subscriptions.empty();

    private DefaultSubscriber<List<PropertySet>> refreshShuffleHeader = new DefaultSubscriber<List<PropertySet>>() {
        @Override
        public void onNext(List<PropertySet> args) {
            createShuffleObservable();
            subscribeShuffleViewController();
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
                       ShuffleViewController shuffleViewController,
                       PlaybackOperations playbackOperations,
                       OfflineContentOperations offlineOperations,
                       TrackLikesActionMenuController actionMenuController,
                       Provider<ExpandPlayerSubscriber> expandPlayerSubscriberProvider) {
        this.adapter = adapter;
        this.likeOperations = likeOperations;
        this.listViewController = listViewController;
        this.pullToRefreshController = pullToRefreshController;
        this.shuffleViewController = shuffleViewController;
        this.playbackOperations = playbackOperations;
        this.offlineOperations = offlineOperations;
        this.actionMenuController = actionMenuController;
        this.expandPlayerSubscriberProvider = expandPlayerSubscriberProvider;
        addLifeCycleComponents();
    }

    private void addLifeCycleComponents() {
        listViewController.setAdapter(adapter, likeOperations.likedTracksPager());
        pullToRefreshController.setRefreshListener(this, adapter);

        addLifeCycleComponent(shuffleViewController);
        addLifeCycleComponent(listViewController);
        addLifeCycleComponent(pullToRefreshController);
        addLifeCycleComponent(adapter.getLifeCycleHandler());
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setRetainInstance(true);
        setHasOptionsMenu(true);
        super.onCreate(savedInstanceState);
        connectObservable(buildObservable());
        createShuffleObservable();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.default_list_with_refresh, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        listViewController.setHeaderViewController(shuffleViewController);
        listViewController.getEmptyView().setImage(R.drawable.empty_like);
        listViewController.getEmptyView().setMessageText(R.string.list_empty_user_likes_message);

        listViewController.connect(this, observable);
        pullToRefreshController.connect(observable, adapter);

        subscribeShuffleViewController();
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
        shuffleSubscription.unsubscribe();
        super.onDestroy();
    }

    @Override
    public ConnectableObservable<List<PropertySet>> buildObservable() {
        return pagedObservable(getLikedTracks());
    }

    private Observable<List<PropertySet>> getLikedTracks() {
        return likeOperations.likedTracks().observeOn(AndroidSchedulers.mainThread());
    }

    private ConnectableObservable<List<PropertySet>> pagedObservable(Observable<List<PropertySet>> source) {
        final ConnectableObservable<List<PropertySet>> observable =
                likeOperations.likedTracksPager().page(source).observeOn(mainThread()).replay();
        observable.subscribe(adapter);
        return observable;
    }

    @Override
    public ConnectableObservable<List<PropertySet>> refreshObservable() {
        final ConnectableObservable<List<PropertySet>> refreshObservable = pagedObservable(getUpdatedLikedTracks());
        refreshObservable.first().subscribe(refreshShuffleHeader);
        return refreshObservable;
    }

    private Observable<List<PropertySet>> getUpdatedLikedTracks() {
        return likeOperations.updatedLikedTracks().observeOn(AndroidSchedulers.mainThread());
    }

    @Override
    public Subscription connectObservable(ConnectableObservable<List<PropertySet>> observable) {
        this.observable = observable;
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
                .playTracks(trackUrnsObservable, initialTrack, position, playSessionSource)
                .subscribe(expandPlayerSubscriberProvider.get());
    }

    private void createShuffleObservable() {
        trackUrnsObservable = likeOperations.likedTrackUrns();
    }

    private void subscribeShuffleViewController() {
        shuffleSubscription = trackUrnsObservable.subscribe(shuffleViewController);
    }
}
