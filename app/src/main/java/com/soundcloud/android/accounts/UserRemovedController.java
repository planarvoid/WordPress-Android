package com.soundcloud.android.accounts;

import com.soundcloud.android.events.CurrentUserChangedEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.lightcycle.DefaultActivityLightCycle;
import com.soundcloud.rx.eventbus.EventBus;
import org.jetbrains.annotations.Nullable;
import rx.Subscription;

import android.app.Activity;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import javax.inject.Inject;

public class UserRemovedController extends DefaultActivityLightCycle<AppCompatActivity> {
    private final EventBus eventBus;
    private Subscription userEventSubscription = RxUtils.invalidSubscription();

    @Inject
    public UserRemovedController(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    @Override
    public void onCreate(AppCompatActivity activity, @Nullable Bundle bundle) {
        userEventSubscription = eventBus.subscribe(EventQueue.CURRENT_USER_CHANGED, new CurrentUserChangedSubscriber(activity));
    }

    @Override
    public void onDestroy(AppCompatActivity activity) {
        userEventSubscription.unsubscribe();
    }

    private static class CurrentUserChangedSubscriber extends DefaultSubscriber<CurrentUserChangedEvent> {
        private final Activity activity;

        public CurrentUserChangedSubscriber(Activity activity) {
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
