package com.soundcloud.android.main;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.events.ActivityLifeCycleEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.rx.eventbus.EventBus;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

/*
 * This base class can be used for lifecycle tracking where extending from ScActivity is not necessary.
 */
public abstract class TrackedActivity extends AppCompatActivity {

    public EventBus eventBus;

    public TrackedActivity() {
    }

    public TrackedActivity(EventBus eventBus) {
        this.eventBus = eventBus;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getEventBus().publish(EventQueue.ACTIVITY_LIFE_CYCLE, ActivityLifeCycleEvent.forOnCreate(this));
    }

    @Override
    protected void onResume() {
        super.onResume();
        getEventBus().publish(EventQueue.ACTIVITY_LIFE_CYCLE, ActivityLifeCycleEvent.forOnResume(this));
    }

    @Override
    protected void onPause() {
        super.onPause();
        getEventBus().publish(EventQueue.ACTIVITY_LIFE_CYCLE, ActivityLifeCycleEvent.forOnPause(this));
    }

    // lazy mans injection. no proper DI in this class or subclasses
    protected EventBus getEventBus() {
        return eventBus == null ? SoundCloudApplication.fromContext(this).getEventBus() : eventBus;
    }

}
