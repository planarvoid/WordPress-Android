package com.soundcloud.android.events;

import static com.soundcloud.android.Expect.expect;

import org.junit.Test;

import android.app.Activity;

public class ActivityLifeCycleEventTest {

    @Test
    public void forOnCreateFactoryMethodShouldReturnAnEventInstanceForOnCreateEvent() throws Exception {
        ActivityLifeCycleEvent activityLifeCycleEvent = ActivityLifeCycleEvent.forOnCreate(Activity.class);
        expect(activityLifeCycleEvent.getKind()).toBe(ActivityLifeCycleEvent.ON_CREATE_EVENT);
        expect(activityLifeCycleEvent.getActivityClass().isAssignableFrom(Activity.class)).toBeTrue();
    }

    @Test
    public void forOnResumeFactoryMethodShouldReturnAnEventInstanceForOnResumeEvent() throws Exception {
        ActivityLifeCycleEvent activityLifeCycleEvent = ActivityLifeCycleEvent.forOnResume(Activity.class);
        expect(activityLifeCycleEvent.getKind()).toBe(ActivityLifeCycleEvent.ON_RESUME_EVENT);
        expect(activityLifeCycleEvent.getActivityClass().isAssignableFrom(Activity.class)).toBeTrue();
    }

    @Test
    public void forOnPauseFactoryMethodShouldReturnAnEventInstanceForOnPauseEvent() throws Exception {
        ActivityLifeCycleEvent activityLifeCycleEvent = ActivityLifeCycleEvent.forOnPause(Activity.class);
        expect(activityLifeCycleEvent.getKind()).toBe(ActivityLifeCycleEvent.ON_PAUSE_EVENT);
        expect(activityLifeCycleEvent.getActivityClass().isAssignableFrom(Activity.class)).toBeTrue();
    }
}
