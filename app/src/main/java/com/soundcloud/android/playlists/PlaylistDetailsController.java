package com.soundcloud.android.playlists;

import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.rx.observers.EmptyViewAware;
import com.soundcloud.android.tracks.UpdatePlayingTrackSubscriber;
import com.soundcloud.android.tracks.TrackItemPresenter;
import com.soundcloud.android.view.adapters.ItemAdapter;
import com.soundcloud.android.view.adapters.UpdateEntityListSubscriber;
import com.soundcloud.propeller.PropertySet;
import org.jetbrains.annotations.Nullable;
import rx.Subscription;
import rx.subscriptions.CompositeSubscription;
import rx.subscriptions.Subscriptions;

import android.os.Bundle;
import android.view.View;

import javax.inject.Inject;

abstract class PlaylistDetailsController implements EmptyViewAware {

    private final TrackItemPresenter trackPresenter;
    private final ItemAdapter<PropertySet> adapter;
    private final EventBus eventBus;
    private Subscription eventSubscriptions = Subscriptions.empty();

    protected PlaylistDetailsController(TrackItemPresenter trackPresenter, ItemAdapter<PropertySet> adapter,
                                        EventBus eventBus) {
        this.trackPresenter = trackPresenter;
        this.adapter = adapter;
        this.eventBus = eventBus;
    }

    ItemAdapter<PropertySet> getAdapter() {
        return adapter;
    }

    boolean hasTracks() {
        // do not use isEmpty, as it will return false if loading
        return adapter.getItems().size() > 0;
    }

    abstract boolean hasContent();

    abstract void setListShown(boolean show);

    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        eventSubscriptions = new CompositeSubscription(
                eventBus.subscribe(EventQueue.PLAY_QUEUE_TRACK, new UpdatePlayingTrackSubscriber(adapter, trackPresenter)),
                eventBus.subscribe(EventQueue.ENTITY_STATE_CHANGED, new UpdateEntityListSubscriber(adapter))
        );
    }

    public void onDestroyView() {
        eventSubscriptions.unsubscribe();
    }

    public static class Provider {
        private final javax.inject.Provider<PlaylistDetailsController> injectionProvider;

        @Inject
        Provider(javax.inject.Provider<PlaylistDetailsController> injectionProvider) {
            this.injectionProvider = injectionProvider;
        }

        public PlaylistDetailsController create() {
            return injectionProvider.get();
        }
    }
}
