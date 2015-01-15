package com.soundcloud.android.main;

import com.soundcloud.android.events.ActivityLifeCycleEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.lightcycle.DefaultActivityLightCycle;
import com.soundcloud.android.rx.eventbus.EventBus;
import org.jetbrains.annotations.Nullable;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

import javax.inject.Inject;

public class ActivityLifeCyclePublisher extends DefaultActivityLightCycle {
    private final EventBus eventBus;

    @Inject
    public ActivityLifeCyclePublisher(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    @Override
    public void onCreate(FragmentActivity activity, @Nullable Bundle bundle) {
        eventBus.publish(EventQueue.ACTIVITY_LIFE_CYCLE, ActivityLifeCycleEvent.forOnCreate(activity.getClass()));
    }

    @Override
    public void onResume(FragmentActivity activity) {
        eventBus.publish(EventQueue.ACTIVITY_LIFE_CYCLE, ActivityLifeCycleEvent.forOnResume(activity.getClass()));
    }

    @Override
    public void onPause(FragmentActivity activity) {
        eventBus.publish(EventQueue.ACTIVITY_LIFE_CYCLE, ActivityLifeCycleEvent.forOnPause(activity.getClass()));
    }
}
