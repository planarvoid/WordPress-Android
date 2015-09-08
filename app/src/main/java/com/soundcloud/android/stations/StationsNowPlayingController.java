package com.soundcloud.android.stations;

import com.soundcloud.android.events.CurrentPlayQueueTrackEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.lightcycle.DefaultSupportFragmentLightCycle;
import com.soundcloud.rx.eventbus.EventBus;
import rx.Subscription;

import android.support.v4.app.Fragment;

import javax.inject.Inject;

class StationsNowPlayingController extends DefaultSupportFragmentLightCycle<Fragment> {
    private final EventBus eventBus;
    private StationsNowPlayingAdapter adapter;
    private Subscription subscription = RxUtils.invalidSubscription();

    @Inject
    StationsNowPlayingController(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    @Override
    public void onResume(Fragment fragment) {
        subscription = eventBus.subscribe(EventQueue.PLAY_QUEUE_TRACK, new Subscriber());
    }

    @Override
    public void onPause(Fragment fragment) {
        subscription.unsubscribe();
    }

    void setAdapter(StationsNowPlayingAdapter adapter) {
        this.adapter = adapter;
    }

    private class Subscriber extends DefaultSubscriber<CurrentPlayQueueTrackEvent> {
        @Override
        public void onNext(CurrentPlayQueueTrackEvent event) {
            if (adapter != null) {
                adapter.updateNowPlaying(event.getCollectionUrn());
            }
        }
    }

    interface StationsNowPlayingAdapter {
        void updateNowPlaying(Urn currentlyPlayingCollectionUrn);
    }
}
