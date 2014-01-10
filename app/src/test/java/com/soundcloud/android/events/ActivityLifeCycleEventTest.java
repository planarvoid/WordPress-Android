package com.soundcloud.android.events;

import static com.soundcloud.android.Expect.expect;

import org.junit.Test;

import android.app.Activity;

public class ActivityLifeCycleEventTest {

    @Test
    public void forOnCreateFactoryMethodShouldReturnAnEventInstanceForOnCreateEvent() throws Exception {
        ActivityLifeCycleEvent activityLifeCycleEvent = ActivityLifeCycleEvent.forOnCreate(Activity.class);
        expect(activityLifeCycleEvent.isCreateEvent()).toBeTrue();
        expect(activityLifeCycleEvent.isResumeEvent()).toBeFalse();
        expect(activityLifeCycleEvent.isPauseEvent()).toBeFalse();
    }

    @Test
    public void forOnResumeFactoryMethodShouldReturnAnEventInstanceForOnResumeEvent() throws Exception {
        ActivityLifeCycleEvent activityLifeCycleEvent = ActivityLifeCycleEvent.forOnResume(Activity.class);
        expect(activityLifeCycleEvent.isResumeEvent()).toBeTrue();
        expect(activityLifeCycleEvent.isCreateEvent()).toBeFalse();
        expect(activityLifeCycleEvent.isPauseEvent()).toBeFalse();
    }

    @Test
    public void forOnPauseFactoryMethodShouldReturnAnEventInstanceForOnPauseEvent() throws Exception {
        ActivityLifeCycleEvent activityLifeCycleEvent = ActivityLifeCycleEvent.forOnPause(Activity.class);
        expect(activityLifeCycleEvent.isPauseEvent()).toBeTrue();
        expect(activityLifeCycleEvent.isResumeEvent()).toBeFalse();
        expect(activityLifeCycleEvent.isCreateEvent()).toBeFalse();
    }

    @Test
    public void shouldStoreTheActivityClass() throws Exception {
        ActivityLifeCycleEvent activityLifeCycleEvent = new ActivityLifeCycleEvent(Activity.class, 0);
        expect(activityLifeCycleEvent.getActivityClass().isAssignableFrom(Activity.class)).toBeTrue();
    }

}
