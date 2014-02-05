package com.soundcloud.android.main;

import android.app.Activity;
import android.os.Bundle;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.events.ActivityLifeCycleEvent;
import com.soundcloud.android.events.EventBus2;
import com.soundcloud.android.events.EventQueue;

/*
 * This base class can be used for lifecycle tracking where extending from ScActivity is not necessary.
 */
public abstract class TrackedActivity extends Activity {

    private EventBus2 mEventBus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mEventBus = SoundCloudApplication.fromContext(this).getEventBus();
        mEventBus.publish(EventQueue.ACTIVITY_LIFE_CYCLE, ActivityLifeCycleEvent.forOnCreate(this.getClass()));
    }

    @Override
    protected void onResume() {
        super.onResume();
        mEventBus.publish(EventQueue.ACTIVITY_LIFE_CYCLE, ActivityLifeCycleEvent.forOnResume(this.getClass()));
    }

    @Override
    protected void onPause() {
        super.onPause();
        mEventBus.publish(EventQueue.ACTIVITY_LIFE_CYCLE, ActivityLifeCycleEvent.forOnPause(this.getClass()));
    }

}
