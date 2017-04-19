package com.soundcloud.android.analytics;

import static com.soundcloud.java.collections.Lists.newArrayList;
import static org.mockito.AdditionalMatchers.or;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.playback.TrackSourceInfo;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.utils.ConnectionHelper;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.List;

public class TrackingHandlerTest extends AndroidUnitTest {

    private TrackingHandler trackingHandler;

    @Mock ConnectionHelper connectionHelper;
    @Mock TrackingApiFactory apiFactory;
    @Mock TrackingApi api;
    @Mock TrackingStorage storage;
    @Mock TrackSourceInfo trackSourceInfo;

    @Before
    public void before() {
        when(connectionHelper.isNetworkConnected()).thenReturn(true);
        when(apiFactory.create(or(isNull(), anyString()))).thenReturn(api);
        trackingHandler = new TrackingHandler(context().getMainLooper(), connectionHelper, storage, apiFactory);
    }

    @Test
    public void shouldInsertTrackingEventsIntoDatabase() throws Exception {
        final TrackingRecord event1 = Mockito.mock(TrackingRecord.class);
        trackingHandler.sendMessage(trackingHandler.obtainMessage(TrackingHandler.INSERT_TOKEN, event1));

        final TrackingRecord event2 = Mockito.mock(TrackingRecord.class);
        trackingHandler.sendMessage(trackingHandler.obtainMessage(TrackingHandler.INSERT_TOKEN, event2));

        InOrder inOrder = Mockito.inOrder(storage);
        inOrder.verify(storage).insertEvent(event1);
        inOrder.verify(storage).insertEvent(event2);
    }

    @Test
    public void shouldNotFlushTrackingEventsWithNoConnection() throws Exception {
        when(connectionHelper.isNetworkConnected()).thenReturn(false);
        trackingHandler.sendMessage(trackingHandler.obtainMessage(TrackingHandler.FLUSH_TOKEN));
        verifyZeroInteractions(storage);
    }

    @Test
    public void shouldNotFlushTrackingEventsWithNoLocalEvents() throws Exception {
        when(storage.getPendingEvents()).thenReturn(Collections.emptyList());
        trackingHandler.sendMessage(trackingHandler.obtainMessage(TrackingHandler.FLUSH_TOKEN));
        verifyZeroInteractions(apiFactory);
    }

    @Test
    public void shouldPushAllPendingEventsToApi() {
        final List<TrackingRecord> events = buildEvents();
        when(storage.getPendingEvents()).thenReturn(events);

        trackingHandler.sendMessage(trackingHandler.obtainMessage(TrackingHandler.FLUSH_TOKEN));
        verify(api).pushToRemote(events);
    }

    @Test
    public void shouldPushPendingEventsForSpecificBackendToApi() {
        final List<TrackingRecord> events = buildEvents();
        when(storage.getPendingEventsForBackend("backend")).thenReturn(events);

        trackingHandler.sendMessage(trackingHandler.obtainMessage(TrackingHandler.FLUSH_TOKEN, "backend"));

        verify(api).pushToRemote(events);
    }

    @Test
    public void shouldNotTryToDeleteEventsIfNoEventsPushed() {
        final List<TrackingRecord> events = buildEvents();
        when(storage.getPendingEvents()).thenReturn(events);
        when(api.pushToRemote(events)).thenReturn(Collections.emptyList());

        trackingHandler.sendMessage(trackingHandler.obtainMessage(TrackingHandler.FLUSH_TOKEN));
        verify(storage, never()).deleteEvents(any(List.class));
    }

    @Test
    public void shouldDeletePushedEvents() throws Exception {
        final List<TrackingRecord> events = buildEvents();
        when(storage.getPendingEvents()).thenReturn(events);
        final List<TrackingRecord> unpushedEvents = events.subList(0, 1);
        when(api.pushToRemote(events)).thenReturn(unpushedEvents);

        trackingHandler.sendMessage(trackingHandler.obtainMessage(TrackingHandler.FLUSH_TOKEN));
        verify(storage).deleteEvents(unpushedEvents);
    }

    private List<TrackingRecord> buildEvents() {
        final TrackingRecord event1 = new TrackingRecord(1L, 1000L, "backend", "url1");
        final TrackingRecord event2 = new TrackingRecord(2L, 2000L, "backend", "url2");
        return newArrayList(event1, event2);
    }
}
