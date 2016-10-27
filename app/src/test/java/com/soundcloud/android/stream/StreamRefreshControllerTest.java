package com.soundcloud.android.stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.StreamEvent;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.utils.TestDateProvider;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observable;
import rx.schedulers.TestScheduler;

import android.support.v7.app.AppCompatActivity;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class StreamRefreshControllerTest extends AndroidUnitTest {

    private static final StreamEvent REFRESH_STREAM_EVENT = StreamEvent.fromStreamRefresh();

    @Mock AppCompatActivity activity;
    @Mock StreamOperations operations;

    private TestEventBus eventBus = new TestEventBus();
    private TestDateProvider dateProvider = new TestDateProvider();
    private TestScheduler scheduler = new TestScheduler();

    private StreamRefreshController controller;

    @Before
    public void setUp() {
        controller = new StreamRefreshController(eventBus, operations, dateProvider, scheduler);
        when(operations.updatedStreamItems()).thenReturn(Observable.just(Collections.<StreamItem>emptyList()));
    }

    @Test
    public void onResumeEmitsRefreshStreamEventWhenStale() throws Exception {
        when(operations.lastSyncTime()).thenReturn(Observable.just(-1L));

        controller.onResume(activity);
        scheduler.advanceTimeBy(30, TimeUnit.SECONDS);

        assertThat(eventBus.lastEventOn(EventQueue.STREAM)).isEqualTo(REFRESH_STREAM_EVENT);
    }

    @Test
    public void onResumeDoesNotEmitRefreshStreamEventWhenNotStale() throws Exception {
        when(operations.lastSyncTime()).thenReturn(Observable.just(dateProvider.getCurrentTime()));

        controller.onResume(activity);
        scheduler.advanceTimeBy(30, TimeUnit.SECONDS);

        eventBus.verifyNoEventsOn(EventQueue.STREAM);
    }

    @Test
    public void onSyncErrorDoesNotPropagateToSubscriber() {
        when(operations.lastSyncTime()).thenReturn(Observable.just(-1L));
        when(operations.updatedStreamItems()).thenReturn(Observable.<List<StreamItem>>error(new Throwable()));

        controller.onResume(activity);
        scheduler.advanceTimeBy(30, TimeUnit.SECONDS);

        eventBus.verifyNoEventsOn(EventQueue.STREAM);
    }

}
