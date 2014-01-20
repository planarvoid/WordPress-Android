package com.soundcloud.android.main;

import com.soundcloud.android.events.ActivityLifeCycleEvent;
import com.soundcloud.android.events.EventBus;

import android.app.Activity;
import android.os.Bundle;

/*
 * This base class can be used for lifecycle tracking where extending from ScActivity is not necessary.
 */
public abstract class TrackedActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EventBus.ACTIVITY_LIFECYCLE.publish(ActivityLifeCycleEvent.forOnCreate(this.getClass()));
    }

    @Override
    protected void onResume() {
        super.onResume();
        EventBus.ACTIVITY_LIFECYCLE.publish(ActivityLifeCycleEvent.forOnResume(this.getClass()));
    }

    @Override
    protected void onPause() {
        super.onPause();
        EventBus.ACTIVITY_LIFECYCLE.publish(ActivityLifeCycleEvent.forOnPause(this.getClass()));
    }

}
