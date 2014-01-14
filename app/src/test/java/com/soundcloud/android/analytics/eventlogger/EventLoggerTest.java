package com.soundcloud.android.analytics.eventlogger;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.events.EventBus;
import com.soundcloud.android.events.PlaybackEvent;
import com.soundcloud.android.events.PlayerLifeCycleEvent;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.os.Looper;
import android.os.Message;

@RunWith(SoundCloudTestRunner.class)
public class EventLoggerTest {
    private EventLogger eventLogger;
    private PlaybackEvent playbackEvent;

    @Mock
    EventLoggerHandlerFactory eventLoggerHandlerFactory;
    @Mock
    EventLoggerHandler handler;
    @Mock
    Message message;
    @Mock
    Message flushMessage;
    @Mock
    Message finishMessage;

    @Before
    public void setUp() throws Exception {
        eventLogger = new EventLogger(eventLoggerHandlerFactory);
        when(eventLoggerHandlerFactory.create(any(Looper.class))).thenReturn(handler);
        when(handler.obtainMessage(EventLoggerHandler.FINISH_TOKEN)).thenReturn(finishMessage);

        playbackEvent = TestHelper.getModelFactory().createModel(PlaybackEvent.class);
    }

    @After
    public void tearDown() throws Exception {
        eventLogger.stop();
    }

    @Test
    public void shouldCreateNewEventLoggerOnTrackEvent() throws Exception {
        eventLogger.trackEvent(playbackEvent);
        verify(eventLoggerHandlerFactory).create(any(Looper.class));
    }

    @Test
    public void shouldSendTrackingEventToHandler() throws Exception {
        when(handler.obtainMessage(EventLoggerHandler.INSERT_TOKEN, playbackEvent)).thenReturn(message);
        eventLogger.trackEvent(playbackEvent);
        verify(handler).sendMessage(message);
    }

    @Test
    public void shouldSendFlushEventToHandler() {
        eventLogger.trackEvent(playbackEvent); // make sure handler is alive

        when(handler.obtainMessage(EventLoggerHandler.FLUSH_TOKEN)).thenReturn(flushMessage);
        eventLogger.flush();
        verify(flushMessage).sendToTarget();
    }

    @Test
    public void shouldRemoveFinishTokenOnTrackEvent() throws Exception {
        when(handler.obtainMessage(EventLoggerHandler.INSERT_TOKEN, playbackEvent)).thenReturn(message);
        eventLogger.trackEvent(playbackEvent);
        verify(handler).removeMessages(EventLoggerHandler.FINISH_TOKEN);
    }

    @Test
    public void shouldSendFinishMessageWhenCallingStop() throws Exception {
        // send event to start handler
        when(handler.obtainMessage(EventLoggerHandler.INSERT_TOKEN, playbackEvent)).thenReturn(message);
        eventLogger.trackEvent(playbackEvent);

        eventLogger.stop();

        verify(finishMessage).sendToTarget();
    }

    @Test
    public void shouldNotStopOnOtherPlayerLifeCycleEvents() throws Exception {
        // send event to start handler
        when(handler.obtainMessage(EventLoggerHandler.INSERT_TOKEN, playbackEvent)).thenReturn(message);
        eventLogger.trackEvent(playbackEvent);

        EventBus.PLAYER_LIFECYCLE.publish(PlayerLifeCycleEvent.forIdle());
        verify(finishMessage, never()).sendToTarget();
    }

    @Test
    public void shouldCreateNewHandlerAfterShutDown() throws Exception {
        when(handler.obtainMessage(EventLoggerHandler.INSERT_TOKEN, playbackEvent)).thenReturn(message);
        eventLogger.trackEvent(playbackEvent);
        EventBus.PLAYER_LIFECYCLE.publish(PlayerLifeCycleEvent.forDestroyed());
        eventLogger.trackEvent(playbackEvent);
        verify(eventLoggerHandlerFactory, times(2)).create(any(Looper.class));
    }
}
