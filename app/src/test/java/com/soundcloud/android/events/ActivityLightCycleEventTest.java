package com.soundcloud.android.events;

import static org.assertj.core.api.Java6Assertions.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import android.app.Activity;

@RunWith(MockitoJUnitRunner.class)
public class ActivityLightCycleEventTest {

    @Mock Activity activity;

    @Test
    public void forOnCreateFactoryMethodShouldReturnAnEventInstanceForOnCreateEvent() {
        ActivityLifeCycleEvent activityLifeCycleEvent = ActivityLifeCycleEvent.forOnCreate(activity);
        assertThat(activityLifeCycleEvent.getKind()).isEqualTo(ActivityLifeCycleEvent.ON_CREATE_EVENT);
        assertThat(activityLifeCycleEvent.getActivityClass().getSuperclass().isAssignableFrom(Activity.class)).isTrue();
    }

    @Test
    public void forOnResumeFactoryMethodShouldReturnAnEventInstanceForOnResumeEvent() {
        ActivityLifeCycleEvent activityLifeCycleEvent = ActivityLifeCycleEvent.forOnResume(activity);
        assertThat(activityLifeCycleEvent.getKind()).isEqualTo(ActivityLifeCycleEvent.ON_RESUME_EVENT);
        assertThat(activityLifeCycleEvent.getActivityClass().getSuperclass().isAssignableFrom(Activity.class)).isTrue();
    }

    @Test
    public void forOnResumeFactoryMethodShouldReturnAnEventInstanceForOnPauseEvent() {
        ActivityLifeCycleEvent activityLifeCycleEvent = ActivityLifeCycleEvent.forOnPause(activity);
        assertThat(activityLifeCycleEvent.getKind()).isEqualTo(ActivityLifeCycleEvent.ON_PAUSE_EVENT);
        assertThat(activityLifeCycleEvent.getActivityClass().getSuperclass().isAssignableFrom(Activity.class)).isTrue();
    }

}
