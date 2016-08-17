package com.soundcloud.android.analytics;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;

public class EventTrackingManagerTest extends AndroidUnitTest {
    private EventTrackingManager eventTrackingManager;

    @Mock TrackingHandlerFactory trackingHandlerFactory;
    @Mock TrackingHandler handler;
    @Mock TrackingRecord event;
    @Mock Message message;
    @Mock Message flushMessage;
    @Mock Message finishMessage;

    @Before
    public void setUp() throws Exception {
        eventTrackingManager = new EventTrackingManager(trackingHandlerFactory);
        when(trackingHandlerFactory.create(any(Looper.class))).thenReturn(handler);
        when(handler.obtainMessage(TrackingHandler.FINISH_TOKEN)).thenReturn(finishMessage);
        final HandlerThread handlerThread = new HandlerThread("t");
        handlerThread.start();
        when(handler.getLooper()).thenReturn(handlerThread.getLooper());
    }

    @Test
    public void shouldCreateNewEventLoggerOnTrackEvent() throws Exception {
        eventTrackingManager.trackEvent(event);
        verify(trackingHandlerFactory).create(any(Looper.class));
    }

    @Test
    public void shouldSendTrackingEventToHandler() throws Exception {
        when(handler.obtainMessage(TrackingHandler.INSERT_TOKEN, event)).thenReturn(message);
        eventTrackingManager.trackEvent(event);
        verify(handler).sendMessage(message);
    }

    @Test
    public void shouldSendFlushMessageToHandlerForSpecificBackend() {
        eventTrackingManager.trackEvent(event); // make sure handler is alive

        when(handler.obtainMessage(TrackingHandler.FLUSH_TOKEN, "backend")).thenReturn(flushMessage);
        eventTrackingManager.flush("backend");
        verify(flushMessage).sendToTarget();
    }

    @Test
    public void shouldRemovePendingFinishMessageOnTrackEvent() throws Exception {
        when(handler.obtainMessage(TrackingHandler.INSERT_TOKEN, event)).thenReturn(message);
        eventTrackingManager.trackEvent(event);
        verify(handler).removeMessages(TrackingHandler.FINISH_TOKEN);
    }

    @Test
    public void shouldSendDelayedFinishMessageWhenTrackingEvent() throws Exception {
        // send event to start handler
        when(handler.obtainMessage(TrackingHandler.INSERT_TOKEN, event)).thenReturn(message);
        eventTrackingManager.trackEvent(event);

        verify(handler).sendMessageDelayed(same(finishMessage), anyLong());
    }

    @Test
    public void shouldCreateNewHandlerForTrackingEventAfterShutDown() throws Exception {
        handler.getLooper().quit();

        eventTrackingManager.trackEvent(event);

        verify(trackingHandlerFactory).create(any(Looper.class));
    }

    @Test
    public void shouldCreateNewHandlerForFlushEventAfterShutDown() throws Exception {
        when(handler.obtainMessage(TrackingHandler.FLUSH_TOKEN, "backend")).thenReturn(flushMessage);
        handler.getLooper().quit();

        eventTrackingManager.flush("backend");

        verify(trackingHandlerFactory).create(any(Looper.class));
    }
}
