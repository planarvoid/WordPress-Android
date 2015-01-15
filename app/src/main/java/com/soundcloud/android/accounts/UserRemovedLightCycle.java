package com.soundcloud.android.accounts;

import com.soundcloud.android.events.CurrentUserChangedEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.lightcycle.DefaultActivityLightCycle;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import org.jetbrains.annotations.Nullable;
import rx.Subscription;
import rx.subscriptions.Subscriptions;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

import javax.inject.Inject;

public class UserRemovedLightCycle extends DefaultActivityLightCycle {
    private final EventBus eventBus;
    private Subscription userEventSubscription = Subscriptions.empty();

    @Inject
    public UserRemovedLightCycle(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    @Override
    public void onCreate(FragmentActivity activity, @Nullable Bundle bundle) {
        userEventSubscription = eventBus.subscribe(EventQueue.CURRENT_USER_CHANGED, new CurrentUserChangedSubscriber(activity));
    }

    @Override
    public void onDestroy(FragmentActivity activity) {
        userEventSubscription.unsubscribe();
    }

    private static class CurrentUserChangedSubscriber extends DefaultSubscriber<CurrentUserChangedEvent> {
        private final Activity activity;

        private CurrentUserChangedSubscriber(Activity activity) {
            this.activity = activity;
        }

        @Override
        public void onNext(CurrentUserChangedEvent args) {
            if (args.getKind() == CurrentUserChangedEvent.USER_REMOVED) {
                activity.finish();
            }
        }
    }
}
