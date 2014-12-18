package com.soundcloud.android.likes;

import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.main.DefaultFragment;
import com.soundcloud.android.tracks.TrackOperations;
import com.soundcloud.android.view.RefreshableListComponent;
import com.soundcloud.propeller.PropertySet;
import rx.Observable;
import rx.Subscription;
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
import java.util.List;

@SuppressLint("ValidFragment")
public class TrackLikesFragment extends DefaultFragment
        implements RefreshableListComponent<ConnectableObservable<List<PropertySet>>> {

    @Inject TrackLikesAdapter adapter;
    @Inject LikeOperations likeOperations;
    @Inject TrackOperations trackOperations;

    private Subscription connectionSubscription = Subscriptions.empty();

    public TrackLikesFragment() {
        setRetainInstance(true);
        SoundCloudApplication.getObjectGraph().inject(this);
    }

    @VisibleForTesting
    TrackLikesFragment(TrackLikesAdapter adapter, LikeOperations likeOperations, TrackOperations trackOperations) {
        this.adapter = adapter;
        this.likeOperations = likeOperations;
        this.trackOperations = trackOperations;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        connectionSubscription = connectObservable(buildObservable());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return super.onCreateView(inflater, container, savedInstanceState);
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
        return observable.connect();
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        // TO be implemented
    }
}
