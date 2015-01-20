package com.soundcloud.android.likes;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.actionbar.PullToRefreshController;
import com.soundcloud.android.actionbar.menu.ActionMenuController;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.lightcycle.LightCycleFragment;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.ExpandPlayerSubscriber;
import com.soundcloud.android.playback.PlaybackOperations;
import com.soundcloud.android.playback.service.PlaySessionSource;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.android.view.ListViewController;
import com.soundcloud.android.view.RefreshableListComponent;
import com.soundcloud.propeller.PropertySet;
import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
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
import javax.inject.Named;
import javax.inject.Provider;
import java.util.List;

@SuppressLint("ValidFragment")
public class TrackLikesFragment extends LightCycleFragment
        implements RefreshableListComponent<ConnectableObservable<List<PropertySet>>> {

    private final Func1<List<PropertySet>, List<Urn>> LIKES_TO_TRACK_URNS =
            new Func1<List<PropertySet>, List<Urn>>() {
                @Override
                public List<Urn> call(List<PropertySet> likedTracks) {
                    return Lists.transform(likedTracks, new Function<PropertySet, Urn>() {
                        @Override
                        public Urn apply(PropertySet propertySet) {
                            return propertySet.get(TrackProperty.URN);
                        }
                    });
                }
    };

    @Inject TrackLikesAdapter adapter;
    @Inject LikeOperations likeOperations;
    @Inject PlaybackOperations playbackOperations;
    @Inject ShuffleViewController shuffleViewController;
    @Inject ListViewController listViewController;
    @Inject PullToRefreshController pullToRefreshController;
    @Inject @Named("LikedTracks") ActionMenuController actionMenuController;
    @Inject Provider<ExpandPlayerSubscriber> expandPlayerSubscriberProvider;

    private ConnectableObservable<List<PropertySet>> observable;
    private Subscription connectionSubscription = Subscriptions.empty();

    public TrackLikesFragment() {
        setRetainInstance(true);
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
                       ActionMenuController syncActionMenuController,
                       Provider<ExpandPlayerSubscriber> expandPlayerSubscriberProvider) {
        this.adapter = adapter;
        this.likeOperations = likeOperations;
        this.listViewController = listViewController;
        this.pullToRefreshController = pullToRefreshController;
        this.shuffleViewController = shuffleViewController;
        this.playbackOperations = playbackOperations;
        this.actionMenuController = syncActionMenuController;
        this.expandPlayerSubscriberProvider = expandPlayerSubscriberProvider;
        addLifeCycleComponents();
    }

    private void addLifeCycleComponents() {
        listViewController.setAdapter(adapter);
        pullToRefreshController.setRefreshListener(this, adapter);

        addLifeCycleComponent(shuffleViewController);
        addLifeCycleComponent(listViewController);
        addLifeCycleComponent(pullToRefreshController);
        addLifeCycleComponent(adapter);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setRetainInstance(true);
        super.onCreate(savedInstanceState);
        connectionSubscription = connectObservable(buildObservable());
        actionMenuController.onCreate(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.track_likes_fragment, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        listViewController.setHeaderViewController(shuffleViewController);
        listViewController.getEmptyView().setImage(R.drawable.empty_like);
        listViewController.getEmptyView().setMessageText(R.string.list_empty_user_likes_message);

        listViewController.connect(this, observable);
        pullToRefreshController.connect(observable, adapter);
        subscribeShuffleViewController(observable);
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
    public void onDestroy() {
        connectionSubscription.unsubscribe();
        super.onDestroy();
    }

    @Override
    public ConnectableObservable<List<PropertySet>> buildObservable() {
        ConnectableObservable<List<PropertySet>> observable = getLikedTracks().replay();
        observable.subscribe(adapter);
        return observable;
    }

    private Observable<List<PropertySet>> getLikedTracks() {
        return likeOperations.likedTracks().observeOn(AndroidSchedulers.mainThread());
    }

    @Override
    public ConnectableObservable<List<PropertySet>> refreshObservable() {
        ConnectableObservable<List<PropertySet>> observable = getUpdatedLikedTracks().replay();
        subscribeShuffleViewController(observable);
        return observable;
    }

    private Observable<List<PropertySet>> getUpdatedLikedTracks() {
        return likeOperations.updatedLikedTracks().observeOn(AndroidSchedulers.mainThread());
    }

    @Override
    public Subscription connectObservable(ConnectableObservable<List<PropertySet>> observable) {
        this.observable = observable;
        return observable.connect();
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        Observable<List<Urn>> likedTracks = getLikedTracks().map(LIKES_TO_TRACK_URNS);
        Urn initialTrack = ((PropertySet) adapterView.getItemAtPosition(position)).get(TrackProperty.URN);
        PlaySessionSource playSessionSource = new PlaySessionSource(Screen.SIDE_MENU_LIKES);
        playbackOperations
                .playTracks(likedTracks, initialTrack, position, playSessionSource)
                .subscribe(expandPlayerSubscriberProvider.get());
    }

    private void subscribeShuffleViewController(ConnectableObservable<List<PropertySet>> observable) {
        observable.map(LIKES_TO_TRACK_URNS).subscribe(shuffleViewController);
    }
}
