package com.soundcloud.android.profile;

import com.soundcloud.android.events.CurrentPlayQueueItemEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.view.adapters.NowPlayingAdapter;
import com.soundcloud.lightcycle.DefaultSupportFragmentLightCycle;
import com.soundcloud.rx.eventbus.EventBus;
import rx.Subscription;

import android.support.v4.app.Fragment;

import javax.inject.Inject;

public class ProfileNowPlayingController extends DefaultSupportFragmentLightCycle<Fragment> {
    private final EventBus eventBus;
    private NowPlayingAdapter adapter;
    private Subscription subscription = RxUtils.invalidSubscription();

    @Inject
    ProfileNowPlayingController(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    @Override
    public void onResume(Fragment fragment) {
        subscription = eventBus.subscribe(EventQueue.CURRENT_PLAY_QUEUE_ITEM, new Subscriber());
    }

    @Override
    public void onPause(Fragment fragment) {
        subscription.unsubscribe();
    }

    public void setAdapter(NowPlayingAdapter adapter) {
        this.adapter = adapter;
    }

    private class Subscriber extends DefaultSubscriber<CurrentPlayQueueItemEvent> {
        @Override
        public void onNext(CurrentPlayQueueItemEvent event) {
            if (adapter != null) {
                adapter.updateNowPlaying(event.getCurrentPlayQueueItem().getUrn()); // implement our own verision of this!!!
            }
        }
    }
}
