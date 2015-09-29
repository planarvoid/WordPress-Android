package com.soundcloud.android.events;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Test;
import org.mockito.Mock;

import android.app.Activity;

public class ActivityLightCycleEventTest extends AndroidUnitTest {

    @Mock Activity activity;

    @Test
    public void forOnCreateFactoryMethodShouldReturnAnEventInstanceForOnCreateEvent() throws Exception {
        ActivityLifeCycleEvent activityLifeCycleEvent = ActivityLifeCycleEvent.forOnCreate(activity);
        assertThat(activityLifeCycleEvent.getKind()).isEqualTo(ActivityLifeCycleEvent.ON_CREATE_EVENT);
        assertThat(activityLifeCycleEvent.getActivityClass().getSuperclass().isAssignableFrom(Activity.class)).isTrue();
    }

    @Test
    public void forOnResumeFactoryMethodShouldReturnAnEventInstanceForOnResumeEvent() throws Exception {
        ActivityLifeCycleEvent activityLifeCycleEvent = ActivityLifeCycleEvent.forOnResume(activity);
        assertThat(activityLifeCycleEvent.getKind()).isEqualTo(ActivityLifeCycleEvent.ON_RESUME_EVENT);
        assertThat(activityLifeCycleEvent.getActivityClass().getSuperclass().isAssignableFrom(Activity.class)).isTrue();
    }

    @Test
    public void forOnResumeFactoryMethodShouldReturnAnEventInstanceForOnPauseEvent() throws Exception {
        ActivityLifeCycleEvent activityLifeCycleEvent = ActivityLifeCycleEvent.forOnPause(activity);
        assertThat(activityLifeCycleEvent.getKind()).isEqualTo(ActivityLifeCycleEvent.ON_PAUSE_EVENT);
        assertThat(activityLifeCycleEvent.getActivityClass().getSuperclass().isAssignableFrom(Activity.class)).isTrue();
    }

    @Test
    public void forOnPauseFactoryMethodShouldReturnAnEventInstanceForOnStartEvent() throws Exception {
        ActivityLifeCycleEvent activityLifeCycleEvent = ActivityLifeCycleEvent.forOnStart(activity);
        assertThat(activityLifeCycleEvent.getKind()).isEqualTo(ActivityLifeCycleEvent.ON_START_EVENT);
        assertThat(activityLifeCycleEvent.getActivityClass().getSuperclass().isAssignableFrom(Activity.class)).isTrue();
    }

    @Test
    public void forOnPauseFactoryMethodShouldReturnAnEventInstanceForOnStopEvent() throws Exception {
        ActivityLifeCycleEvent activityLifeCycleEvent = ActivityLifeCycleEvent.forOnStop(activity);
        assertThat(activityLifeCycleEvent.getKind()).isEqualTo(ActivityLifeCycleEvent.ON_STOP_EVENT);
        assertThat(activityLifeCycleEvent.getActivityClass().getSuperclass().isAssignableFrom(Activity.class)).isTrue();
    }

}
