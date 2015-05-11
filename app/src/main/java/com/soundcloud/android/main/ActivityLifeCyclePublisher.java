package com.soundcloud.android.main;

import com.soundcloud.android.events.ActivityLifeCycleEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.lightcycle.DefaultLightCycleActivity;
import com.soundcloud.android.rx.eventbus.EventBus;
import org.jetbrains.annotations.Nullable;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;

import javax.inject.Inject;

public class ActivityLifeCyclePublisher extends DefaultLightCycleActivity<ActionBarActivity> {
    private final EventBus eventBus;

    @Inject
    public ActivityLifeCyclePublisher(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    @Override
    public void onCreate(ActionBarActivity activity, @Nullable Bundle bundle) {
        eventBus.publish(EventQueue.ACTIVITY_LIFE_CYCLE, ActivityLifeCycleEvent.forOnCreate(activity.getClass()));
    }

    @Override
    public void onResume(ActionBarActivity activity) {
        eventBus.publish(EventQueue.ACTIVITY_LIFE_CYCLE, ActivityLifeCycleEvent.forOnResume(activity.getClass()));
    }

    @Override
    public void onPause(ActionBarActivity activity) {
        eventBus.publish(EventQueue.ACTIVITY_LIFE_CYCLE, ActivityLifeCycleEvent.forOnPause(activity.getClass()));
    }
}
