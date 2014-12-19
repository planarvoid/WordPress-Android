package com.soundcloud.android.likes;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.Actions;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.actionbar.PullToRefreshController;
import com.soundcloud.android.main.DefaultFragment;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.tracks.TrackOperations;
import com.soundcloud.android.view.EmptyView;
import com.soundcloud.android.view.ListViewController;
import com.soundcloud.android.view.RefreshableListComponent;
import com.soundcloud.propeller.PropertySet;
import rx.Observable;
import rx.Subscription;
import rx.functions.Func1;
import rx.observables.ConnectableObservable;
import rx.subscriptions.Subscriptions;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;

import javax.inject.Inject;
import java.util.List;

@SuppressLint("ValidFragment")
public class TrackLikesFragment extends DefaultFragment
        implements RefreshableListComponent<ConnectableObservable<List<PropertySet>>> {

    @Inject TrackLikesAdapter adapter;
    @Inject LikeOperations likeOperations;
    @Inject TrackOperations trackOperations;
    @Inject ListViewController listViewController;
    @Inject PullToRefreshController pullToRefreshController;

    private ConnectableObservable<List<PropertySet>> observable;
    private Subscription connectionSubscription = Subscriptions.empty();

    public TrackLikesFragment() {
        setRetainInstance(true);
        SoundCloudApplication.getObjectGraph().inject(this);
        addLifeCycleComponents();
    }

    @VisibleForTesting
    TrackLikesFragment(TrackLikesAdapter adapter, LikeOperations likeOperations, TrackOperations trackOperations) {
        this.adapter = adapter;
        this.likeOperations = likeOperations;
        this.trackOperations = trackOperations;
        addLifeCycleComponents();
    }

    private void addLifeCycleComponents() {
        listViewController.setAdapter(adapter);
        pullToRefreshController.setRefreshListener(this, adapter);

        addLifeCycleComponent(listViewController);
        addLifeCycleComponent(pullToRefreshController);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
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

        final EmptyView emptyView = listViewController.getEmptyView();
        emptyView.setImage(R.drawable.empty_like);

        listViewController.connect(this, observable);
        pullToRefreshController.connect(observable, adapter);
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
        return likeOperations.likedTracks().flatMap(new Func1<PropertySet, Observable<PropertySet>>() {
            @Override
            public Observable<PropertySet> call(PropertySet propertySet) {
                return trackOperations.track(propertySet.get(LikeProperty.TARGET_URN));
            }
        }).toList();
    }

    @Override
    public ConnectableObservable<List<PropertySet>> refreshObservable() {
        return buildObservable();
    }

    @Override
    public Subscription connectObservable(ConnectableObservable<List<PropertySet>> observable) {
        this.observable = observable;
        return observable.connect();
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        // TO be implemented
    }
}
