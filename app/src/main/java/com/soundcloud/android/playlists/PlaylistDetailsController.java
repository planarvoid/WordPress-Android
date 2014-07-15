package com.soundcloud.android.playlists;

import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.rx.observers.EmptyViewAware;
import com.soundcloud.android.view.adapters.ItemAdapter;
import com.soundcloud.android.view.adapters.ListContentChangedSubscriber;
import com.soundcloud.android.view.adapters.TrackChangedSubscriber;
import com.soundcloud.android.view.adapters.TrackItemPresenter;
import com.soundcloud.propeller.PropertySet;
import rx.Subscription;
import rx.subscriptions.CompositeSubscription;
import rx.subscriptions.Subscriptions;

import android.content.res.Resources;
import android.view.View;

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

    abstract boolean hasContent();

    abstract void setListShown(boolean show);

    void onViewCreated(View layout, Resources resources) {
        eventSubscriptions = new CompositeSubscription(
                eventBus.subscribe(EventQueue.PLAY_QUEUE, new TrackChangedSubscriber(adapter, trackPresenter)),
                eventBus.subscribe(EventQueue.PLAYABLE_CHANGED, new ListContentChangedSubscriber(adapter))
        );
    }

    void onDestroyView() {
        eventSubscriptions.unsubscribe();
    }
}
