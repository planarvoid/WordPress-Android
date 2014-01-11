package com.soundcloud.android.analytics.eventlogger;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.events.EventBus;
import com.soundcloud.android.events.PlaybackEvent;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
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

    @Mock
    EventLoggerHandlerFactory eventLoggerHandlerFactory;
    @Mock
    PlaybackEvent playbackEvent;
    @Mock
    EventLoggerHandler handler;
    @Mock
    Message message;
    @Mock
    Message finishMessage;

    @Before
    public void setUp() throws Exception {
        eventLogger = new EventLogger(eventLoggerHandlerFactory);
        when(eventLoggerHandlerFactory.create(any(Looper.class))).thenReturn(handler);
        when(handler.obtainMessage(EventLoggerHandler.FINISH_TOKEN)).thenReturn(finishMessage);
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
    public void shouldRemoveFinishTokenOnTrackEvent() throws Exception {
        when(handler.obtainMessage(EventLoggerHandler.INSERT_TOKEN, playbackEvent)).thenReturn(message);
        eventLogger.trackEvent(playbackEvent);
        verify(handler).removeMessages(EventLoggerHandler.FINISH_TOKEN);
    }

    @Test
    public void playbackServiceShutdownEventShouldSendFinishToken() throws Exception {
        // send event to start handler
        when(handler.obtainMessage(EventLoggerHandler.INSERT_TOKEN, playbackEvent)).thenReturn(message);
        eventLogger.trackEvent(playbackEvent);

        EventBus.PLAYBACK_SERVICE_DESTROYED.publish();
        verify(finishMessage).sendToTarget();
    }

    @Test
    public void shouldCreateNewHandlerAfterShutDown() throws Exception {
        when(handler.obtainMessage(EventLoggerHandler.INSERT_TOKEN, playbackEvent)).thenReturn(message);
        eventLogger.trackEvent(playbackEvent);
        EventBus.PLAYBACK_SERVICE_DESTROYED.publish();
        eventLogger.trackEvent(playbackEvent);
        verify(eventLoggerHandlerFactory, times(2)).create(any(Looper.class));
    }
}
