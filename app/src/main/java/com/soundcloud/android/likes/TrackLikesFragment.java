package com.soundcloud.android.likes;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.actionbar.PullToRefreshController;
import com.soundcloud.android.analytics.Screen;
import com.soundcloud.android.main.DefaultFragment;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.ExpandPlayerSubscriber;
import com.soundcloud.android.playback.PlaybackOperations;
import com.soundcloud.android.playback.service.PlaySessionSource;
import com.soundcloud.android.tracks.TrackOperations;
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
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.List;

@SuppressLint("ValidFragment")
public class TrackLikesFragment extends DefaultFragment
        implements RefreshableListComponent<ConnectableObservable<List<PropertySet>>> {

    private final Func1<PropertySet, Observable<PropertySet>> LIKES_TO_TRACKS_LIKED =
            new Func1<PropertySet, Observable<PropertySet>>() {
                @Override
                public Observable<PropertySet> call(PropertySet propertySet) {
                    return trackOperations.track(propertySet.get(LikeProperty.TARGET_URN));
                }
            };

    private final Func1<List<PropertySet>, List<Urn>> LIKES_TO_TRACK_URNS =
            new Func1<List<PropertySet>, List<Urn>>() {
                @Override
                public List<Urn> call(List<PropertySet> likedTracks) {
                    return Lists.transform(likedTracks, new Function<PropertySet, Urn>() {
                        @Override
                        public Urn apply(PropertySet propertySet) {
                            return propertySet.get(LikeProperty.TARGET_URN);
                        }
                    });
                }
    };

    private final Func1<PropertySet, Urn> LIKES_TO_TRACK_URN =
            new Func1<PropertySet, Urn>() {
                @Override
                public Urn call(PropertySet propertySet) {
                    return propertySet.get(LikeProperty.TARGET_URN);
                }
            };

    @Inject TrackLikesAdapter adapter;
    @Inject LikeOperations likeOperations;
    @Inject TrackOperations trackOperations;
    @Inject PlaybackOperations playbackOperations;
    @Inject ShuffleViewController shuffleViewController;
    @Inject ListViewController listViewController;
    @Inject PullToRefreshController pullToRefreshController;
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
                       TrackOperations trackOperations,
                       ListViewController listViewController,
                       PullToRefreshController pullToRefreshController,
                       ShuffleViewController shuffleViewController) {
        this.adapter = adapter;
        this.likeOperations = likeOperations;
        this.trackOperations = trackOperations;
        this.listViewController = listViewController;
        this.pullToRefreshController = pullToRefreshController;
        this.shuffleViewController = shuffleViewController;
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
        Observable<PropertySet> propertySetObservable = likeOperations.likedTracks();
        return propertySetObservable.flatMap(LIKES_TO_TRACKS_LIKED).observeOn(AndroidSchedulers.mainThread()).toList();
    }

    @Override
    public ConnectableObservable<List<PropertySet>> refreshObservable() {
        ConnectableObservable<List<PropertySet>> observable = buildObservable();
        subscribeShuffleViewController(observable);
        return observable;
    }

    @Override
    public Subscription connectObservable(ConnectableObservable<List<PropertySet>> observable) {
        this.observable = observable;
        return observable.connect();
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        Observable<Urn> likedTracks = likeOperations.likedTracks().map(LIKES_TO_TRACK_URN);
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
