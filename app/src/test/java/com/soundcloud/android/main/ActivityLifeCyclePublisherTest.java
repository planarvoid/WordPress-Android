package com.soundcloud.android.main;

import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.android.events.ActivityLifeCycleEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import android.support.v7.app.AppCompatActivity;

@RunWith(MockitoJUnitRunner.class)
public class ActivityLifeCyclePublisherTest {
    @Mock private AppCompatActivity activity;
    private TestEventBus eventBus;

    private ActivityLifeCyclePublisher lightCycle;

    @Before
    public void setUp() throws Exception {
        eventBus = new TestEventBus();
        lightCycle = new ActivityLifeCyclePublisher(eventBus);
    }

    @Test
    public void sendOnCreateWhenActivityStarts() {
        lightCycle.onCreate(activity, null);

        expectEvent(ActivityLifeCycleEvent.ON_CREATE_EVENT);
    }

    @Test
    public void sendOnResumeWhenActivityResumes() {
        lightCycle.onResume(activity);

        expectEvent(ActivityLifeCycleEvent.ON_RESUME_EVENT);
    }

    @Test
    public void sendOnPauseWhenActivityPauses() {
        lightCycle.onPause(activity);

        expectEvent(ActivityLifeCycleEvent.ON_PAUSE_EVENT);
    }

    private void expectEvent(int eventType) {
        final ActivityLifeCycleEvent event = getEventOnQueue();
        assertThat(event.getActivityClass() == activity.getClass()).isTrue();
        assertThat(event.getKind()).isEqualTo(eventType);
    }

    private ActivityLifeCycleEvent getEventOnQueue() {
        return eventBus.eventsOn(EventQueue.ACTIVITY_LIFE_CYCLE).get(0);
    }
}
