package com.soundcloud.android.likes;

import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.lightcycle.FragmentLightCycle;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.tracks.TrackChangedSubscriber;
import com.soundcloud.android.tracks.TrackItemPresenter;
import com.soundcloud.android.view.adapters.EndlessAdapter;
import com.soundcloud.android.view.adapters.ListContentChangedSubscriber;
import com.soundcloud.android.view.adapters.ListContentSyncedSubscriber;
import com.soundcloud.android.view.adapters.ReactiveAdapter;
import com.soundcloud.propeller.PropertySet;
import org.jetbrains.annotations.Nullable;
import rx.Subscription;
import rx.subscriptions.CompositeSubscription;
import rx.subscriptions.Subscriptions;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.View;

import javax.inject.Inject;

public class TrackLikesAdapter extends EndlessAdapter<PropertySet>
        implements ReactiveAdapter<Iterable<PropertySet>>, FragmentLightCycle {

    private final TrackItemPresenter trackPresenter;
    private final EventBus eventBus;

    private Subscription eventSubscriptions = Subscriptions.empty();

    @Inject
    public TrackLikesAdapter(TrackItemPresenter trackPresenter, EventBus eventBus) {
        super(trackPresenter);
        this.trackPresenter = trackPresenter;
        this.eventBus = eventBus;
    }

    public TrackItemPresenter getTrackPresenter() {
        return trackPresenter;
    }

    @Override
    public void onViewCreated(Fragment fragment, View view, @Nullable Bundle savedInstanceState) {
        eventSubscriptions = new CompositeSubscription(
                eventBus.subscribe(EventQueue.PLAY_QUEUE_TRACK, new TrackChangedSubscriber(this, trackPresenter)),
                eventBus.subscribe(EventQueue.PLAYABLE_CHANGED, new ListContentChangedSubscriber(this)),
                eventBus.subscribe(EventQueue.RESOURCES_SYNCED, new ListContentSyncedSubscriber(this))
        );
    }

    @Override
    public void onDestroyView(Fragment fragment) {
        eventSubscriptions.unsubscribe();
    }


    @Override
    public void onCreate(Fragment fragment, @Nullable Bundle bundle) {
        // No-op
    }

    @Override
    public void onStart(Fragment fragment) {
        // No-op
    }

    @Override
    public void onResume(Fragment fragment) {
        // No-op
    }

    @Override
    public void onPause(Fragment fragment) {
        // No-op
    }

    @Override
    public void onStop(Fragment fragment) {
        // No-op
    }

    @Override
    public void onSaveInstanceState(Fragment fragment, Bundle bundle) {
        // No-op
    }

    @Override
    public void onRestoreInstanceState(Fragment fragment, Bundle bundle) {
        // No-op
    }

    @Override
    public void onDestroy(Fragment fragment) {
        // No-op
    }
}
