package com.soundcloud.android.main;

import com.soundcloud.android.events.ActivityLifeCycleEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.lightcycle.DefaultActivityLightCycle;
import com.soundcloud.rx.eventbus.EventBus;
import org.jetbrains.annotations.Nullable;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import javax.inject.Inject;

public class ActivityLifeCyclePublisher extends DefaultActivityLightCycle<AppCompatActivity> {
    private final EventBus eventBus;

    @Inject
    public ActivityLifeCyclePublisher(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    @Override
    public void onCreate(AppCompatActivity activity, @Nullable Bundle bundle) {
        eventBus.publish(EventQueue.ACTIVITY_LIFE_CYCLE, ActivityLifeCycleEvent.forOnCreate(activity.getClass()));
    }

    @Override
    public void onStart(AppCompatActivity activity) {
        eventBus.publish(EventQueue.ACTIVITY_LIFE_CYCLE, ActivityLifeCycleEvent.forOnStart(activity));
    }

    @Override
    public void onResume(AppCompatActivity activity) {
        eventBus.publish(EventQueue.ACTIVITY_LIFE_CYCLE, ActivityLifeCycleEvent.forOnResume(activity.getClass()));
    }

    @Override
    public void onPause(AppCompatActivity activity) {
        eventBus.publish(EventQueue.ACTIVITY_LIFE_CYCLE, ActivityLifeCycleEvent.forOnPause(activity.getClass()));
    }

    @Override
    public void onStop(AppCompatActivity activity) {
        eventBus.publish(EventQueue.ACTIVITY_LIFE_CYCLE, ActivityLifeCycleEvent.forOnStop(activity));
    }

}
