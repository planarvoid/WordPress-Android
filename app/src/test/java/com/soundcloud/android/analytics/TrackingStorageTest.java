package com.soundcloud.android.analytics;

import static com.soundcloud.android.analytics.TrackingDbHelper.EVENTS_TABLE;
import static com.soundcloud.propeller.query.Query.from;
import static com.soundcloud.propeller.test.matchers.QueryMatchers.counts;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

import com.soundcloud.android.playback.service.TrackSourceInfo;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.robolectric.StorageIntegrationTest;
import com.soundcloud.android.utils.NetworkConnectionHelper;
import com.soundcloud.propeller.query.Query;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;


@RunWith(SoundCloudTestRunner.class)
public class TrackingStorageTest extends StorageIntegrationTest {

    private TrackingStorage trackingStorage;

    @Mock private NetworkConnectionHelper networkConnectionHelper;
    @Mock private TrackSourceInfo trackSourceInfo;
    @Mock private TrackingApi eventLoggerApi;
    @Mock private TrackingEvent trackingEvent;

    @Before
    public void setUp() throws Exception {
        trackingStorage = new TrackingStorage(propeller(), networkConnectionHelper);
        database().execSQL("DROP TABLE IF EXISTS " + EVENTS_TABLE);
        database().execSQL(TrackingDbHelper.DATABASE_CREATE_EVENTS_TABLE);
    }

    @Test
    public void shouldInsertPlaybackEvent() throws Exception {
        when(trackingEvent.getUrl()).thenReturn("http://eventlogger.soundcloud.com/audio?keys=values");
        when(trackingEvent.getTimeStamp()).thenReturn(1000L);
        when(trackingEvent.getBackend()).thenReturn("eventlogger");

        trackingStorage.insertEvent(trackingEvent);

        Query events = from(EVENTS_TABLE)
                .whereEq(TrackingDbHelper.TrackingColumns.BACKEND, "eventlogger")
                .whereEq(TrackingDbHelper.TrackingColumns.TIMESTAMP, 1000L)
                .whereEq(TrackingDbHelper.TrackingColumns.URL, "http://eventlogger.soundcloud.com/audio?keys=values");
        assertThat(select(events), counts(1));
    }

    @Test
    public void shouldQueryDatabaseForPendingEventsWithFixedBatchSize() throws UnsupportedEncodingException {
        insertEvents(TrackingStorage.FIXED_BATCH_SIZE + 1);

        final List<TrackingEvent> pendingEvents = trackingStorage.getPendingEvents();

        assertThat(pendingEvents.size(), is(TrackingStorage.FIXED_BATCH_SIZE));
    }

    @Test
    public void shouldQueryDatabaseForAllPendingEventsWhenOnWifi() throws UnsupportedEncodingException {
        insertEvents(TrackingStorage.FIXED_BATCH_SIZE + 1);
        when(networkConnectionHelper.isWifiConnected()).thenReturn(true);

        final List<TrackingEvent> pendingEvents = trackingStorage.getPendingEvents();

        assertThat(pendingEvents.size(), is(TrackingStorage.FIXED_BATCH_SIZE + 1));
    }

    @Test
    public void shouldQueryDatabaseForPendingEventsByBackend() throws UnsupportedEncodingException {
        trackingStorage.insertEvent(new TrackingEvent(1000L, "eventlogger", "url"));
        trackingStorage.insertEvent(new TrackingEvent(2000L, "play_counts", "url"));
        assertThat(select(from(EVENTS_TABLE)), counts(2));

        List<TrackingEvent> playCountEvents = trackingStorage.getPendingEventsForBackend("play_counts");
        assertThat(playCountEvents.size(), is(1));
        assertThat(playCountEvents.get(0).getBackend(), is("play_counts"));
    }

    @Test
    public void shouldQueryDatabaseForAllEvents() throws UnsupportedEncodingException {
        trackingStorage.insertEvent(new TrackingEvent(1000L, "eventlogger", "url1"));
        trackingStorage.insertEvent(new TrackingEvent(2000L, "play_counts", "url2"));
        assertThat(select(from(EVENTS_TABLE)), counts(2));

        List<TrackingEvent> events = trackingStorage.getPendingEvents();
        assertThat(events.size(), is(2));
        assertThat(events.get(0).getTimeStamp(), is(1000L));
        assertThat(events.get(0).getBackend(), is("eventlogger"));
        assertThat(events.get(0).getUrl(), is("url1"));
        assertThat(events.get(1).getTimeStamp(), is(2000L));
        assertThat(events.get(1).getBackend(), is("play_counts"));
        assertThat(events.get(1).getUrl(), is("url2"));
    }

    @Test
    public void shouldDeleteEvents() throws Exception {
        insertEvents(2);
        List<TrackingEvent> events = trackingStorage.getPendingEvents();
        assertThat(events.size(), is(2));

        trackingStorage.deleteEvents(events);

        assertThat(select(from(EVENTS_TABLE)), counts(0));
    }

    private List<TrackingEvent> insertEvents(int numEvents) throws UnsupportedEncodingException {
        List<TrackingEvent> events = new ArrayList<TrackingEvent>(numEvents);
        for (int i = 0; i < numEvents; i++) {
            final TrackingEvent event = new TrackingEvent((long) i, "backend", "url");
            trackingStorage.insertEvent(event);
            events.add(event);
        }
        assertThat(select(from(EVENTS_TABLE)), counts(numEvents));
        return events;
    }
}
