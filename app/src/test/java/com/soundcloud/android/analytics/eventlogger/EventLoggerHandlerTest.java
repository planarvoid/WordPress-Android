package com.soundcloud.android.analytics.eventlogger;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.playback.service.TrackSourceInfo;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;

import android.content.Context;
import android.util.Pair;

import java.util.ArrayList;
import java.util.Collections;

@RunWith(SoundCloudTestRunner.class)
public class EventLoggerHandlerTest {

    private EventLoggerHandler eventLoggerHandler;

    @Mock
    Context context;
    @Mock
    EventLoggerApi api;
    @Mock
    EventLoggerStorage storage;
    @Mock
    TrackSourceInfo trackSourceInfo;

    @Before
    public void before() {
        eventLoggerHandler = new EventLoggerHandler(Robolectric.application.getMainLooper(), context, storage, api);
        when(context.getSystemService(Context.CONNECTIVITY_SERVICE)).thenReturn(Robolectric.application.getSystemService(Context.CONNECTIVITY_SERVICE));
    }

    @Test
    public void shouldInsertTrackingEventsIntoDatabase() throws Exception {
        Track track = TestHelper.getModelFactory().createModel(Track.class);

        final EventLoggerEvent event1 = Mockito.mock(EventLoggerEvent.class);
        eventLoggerHandler.sendMessage(eventLoggerHandler.obtainMessage(EventLoggerHandler.INSERT_TOKEN, event1));

        final EventLoggerEvent event2 = Mockito.mock(EventLoggerEvent.class);
        eventLoggerHandler.sendMessage(eventLoggerHandler.obtainMessage(EventLoggerHandler.INSERT_TOKEN, event2));

        InOrder inOrder = Mockito.inOrder(storage);
        inOrder.verify(storage).insertEvent(event1);
        inOrder.verify(storage).insertEvent(event2);
    }

    @Test
    public void shouldNotFlushTrackingEventsWithNoConnection() throws Exception {
        TestHelper.simulateOffline();
        eventLoggerHandler.sendMessage(eventLoggerHandler.obtainMessage(EventLoggerHandler.FLUSH_TOKEN));
        verifyZeroInteractions(storage);
    }

    @Test
    public void shouldNotFlushTrackingEventsWithNoLocalEvents() throws Exception {
        when(storage.getUnpushedEvents(api)).thenReturn(Collections.<Pair<Long, String>>emptyList());
        eventLoggerHandler.sendMessage(eventLoggerHandler.obtainMessage(EventLoggerHandler.FLUSH_TOKEN));
        verifyZeroInteractions(api);
    }

    @Test
    public void shouldPushLocalEventsToApi(){
        final ArrayList<Pair<Long, String>> pairs = setupUnpushedEvents();

        eventLoggerHandler.sendMessage(eventLoggerHandler.obtainMessage(EventLoggerHandler.FLUSH_TOKEN));
        verify(api).pushToRemote(pairs);
    }

    @Test
    public void shouldNotTryToDeleteEventsIfNoEventsPushed(){
        final ArrayList<Pair<Long, String>> pairs = setupUnpushedEvents();
        when(api.pushToRemote(pairs)).thenReturn(new String[]{});

        eventLoggerHandler.sendMessage(eventLoggerHandler.obtainMessage(EventLoggerHandler.FLUSH_TOKEN));
        verify(storage, never()).deleteEventsById(any(String[].class));
    }

    @Test
    public void shouldDeletePushedEvents() throws Exception {
        final ArrayList<Pair<Long, String>> pairs = setupUnpushedEvents();
        final String[] strings = {"1L", "2L"};
        when(api.pushToRemote(pairs)).thenReturn(strings);

        eventLoggerHandler.sendMessage(eventLoggerHandler.obtainMessage(EventLoggerHandler.FLUSH_TOKEN));
        verify(storage).deleteEventsById(strings);
    }

    @Test
    public void shouldFlushOnFinishToken() {
        eventLoggerHandler.sendMessage(eventLoggerHandler.obtainMessage(EventLoggerHandler.FINISH_TOKEN));
        verify(storage).getUnpushedEvents(api);
    }

    private ArrayList<Pair<Long, String>> setupUnpushedEvents() {
        final Pair<Long, String> pair1 = new Pair<Long, String>(1L, "url1");
        final Pair<Long, String> pair2 = new Pair<Long, String>(2L, "url2");
        final ArrayList<Pair<Long,String>> pairs = Lists.newArrayList(pair1, pair2);
        when(storage.getUnpushedEvents(api)).thenReturn(pairs);
        return pairs;
    }
}
