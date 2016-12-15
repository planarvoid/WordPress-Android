package com.soundcloud.android.presentation;

import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.tracks.LegacyUpdatePlayingTrackSubscriber;
import com.soundcloud.android.tracks.TrackItemRenderer;
import com.soundcloud.android.view.adapters.LikeEntityListSubscriber;
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
    private final RecyclerItemAdapter<? extends ListItem, ?> adapter;
    private final TrackItemRenderer trackItemRenderer;

    private CompositeSubscription fragmentLifeCycle;

    public PlayableListUpdater(EventBus eventBus,
                               RecyclerItemAdapter<? extends ListItem, ?> adapter,
                               TrackItemRenderer trackItemRenderer) {
        this.eventBus = eventBus;
        this.adapter = adapter;
        this.trackItemRenderer = trackItemRenderer;
    }

    @Override
    public void onCreate(Fragment fragment, @Nullable Bundle bundle) {
        super.onCreate(fragment, bundle);
        fragmentLifeCycle = new CompositeSubscription(
                eventBus.subscribe(EventQueue.CURRENT_PLAY_QUEUE_ITEM,
                                   new LegacyUpdatePlayingTrackSubscriber(adapter, trackItemRenderer)),
                eventBus.subscribe(EventQueue.ENTITY_STATE_CHANGED, new UpdateEntityListSubscriber(adapter)),
                eventBus.subscribe(EventQueue.LIKE_CHANGED, new LikeEntityListSubscriber(adapter))
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

        public PlayableListUpdater create(RecyclerItemAdapter<? extends ListItem, ?> adapter,
                                          TrackItemRenderer trackItemRenderer) {
            return new PlayableListUpdater(eventBus, adapter, trackItemRenderer);
        }
    }
}
