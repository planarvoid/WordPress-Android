package com.soundcloud.android.presentation;

import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.tracks.TrackItemRenderer;
import com.soundcloud.android.tracks.UpdatePlayingTrackSubscriber;
import com.soundcloud.android.view.adapters.UpdateEntityListSubscriber;
import com.soundcloud.lightcycle.DefaultSupportFragmentLightCycle;
import com.soundcloud.rx.eventbus.EventBus;
import org.jetbrains.annotations.Nullable;
import rx.subscriptions.CompositeSubscription;

import android.os.Bundle;
import android.support.v4.app.Fragment;

import javax.inject.Inject;

public class PlayableListUpdater extends DefaultSupportFragmentLightCycle<Fragment> {

    private final EventBus eventBus;
    private final ItemAdapter<? extends ListItem> adapter;
    private final TrackItemRenderer trackItemRenderer;

    private CompositeSubscription fragmentLifeCycle;

    public PlayableListUpdater(EventBus eventBus, ItemAdapter<? extends ListItem> adapter,
                               TrackItemRenderer trackItemRenderer) {
        this.eventBus = eventBus;
        this.adapter = adapter;
        this.trackItemRenderer = trackItemRenderer;
    }

    @Override
    public void onCreate(Fragment fragment, @Nullable Bundle bundle) {
        super.onCreate(fragment, bundle);
        fragmentLifeCycle = new CompositeSubscription(
                eventBus.subscribe(EventQueue.PLAY_QUEUE_TRACK, new UpdatePlayingTrackSubscriber(adapter, trackItemRenderer)),
                eventBus.subscribe(EventQueue.ENTITY_STATE_CHANGED, new UpdateEntityListSubscriber(adapter))
        );
    }

    @Override
    public void onDestroy(Fragment fragment) {
        fragmentLifeCycle.unsubscribe();
        super.onDestroy(fragment);
    }

    public static class Factory {

        private final EventBus eventBus;

        @Inject
        public Factory(EventBus eventBus) {
            this.eventBus = eventBus;
        }

        public PlayableListUpdater create(ItemAdapter<? extends ListItem> adapter, TrackItemRenderer trackItemRenderer){
            return new PlayableListUpdater(eventBus, adapter, trackItemRenderer);
        }
    }
}
