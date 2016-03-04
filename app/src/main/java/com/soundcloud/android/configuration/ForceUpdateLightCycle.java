package com.soundcloud.android.configuration;

import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.lightcycle.DefaultActivityLightCycle;
import com.soundcloud.rx.eventbus.EventBus;
import rx.Subscription;

import android.support.v7.app.AppCompatActivity;

import javax.inject.Inject;

public class ForceUpdateLightCycle extends DefaultActivityLightCycle<AppCompatActivity> {

    private final EventBus eventBus;
    private Subscription subscription = RxUtils.invalidSubscription();

    @Inject
    public ForceUpdateLightCycle(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    @Override
    public void onResume(final AppCompatActivity activity) {
        subscription = eventBus.queue(EventQueue.FORCE_UPDATE)
                .take(1)
                .subscribe(new ForceUpdateSubscriber(activity));
    }

    @Override
    public void onPause(AppCompatActivity activity) {
        subscription.unsubscribe();
    }

    private static class ForceUpdateSubscriber extends DefaultSubscriber<ForceUpdateEvent> {
        private final AppCompatActivity activity;

        public ForceUpdateSubscriber(AppCompatActivity activity) {
            this.activity = activity;
        }

        @Override
        public void onNext(ForceUpdateEvent event) {
            ForceUpdateDialog.show(activity.getSupportFragmentManager());
        }
    }
}
