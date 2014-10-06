package com.soundcloud.android.analytics;

import static com.soundcloud.android.analytics.TrackingDbHelper.EVENTS_TABLE;
import static com.soundcloud.propeller.query.Query.from;
import static com.soundcloud.propeller.test.matchers.QueryMatchers.counts;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

import com.soundcloud.android.playback.service.TrackSourceInfo;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
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
    @Mock private TrackingRecord trackingRecord;

    @Before
    public void setUp() throws Exception {
        trackingStorage = new TrackingStorage(propeller(), networkConnectionHelper);
        database().execSQL("DROP TABLE IF EXISTS " + EVENTS_TABLE);
        database().execSQL(TrackingDbHelper.DATABASE_CREATE_EVENTS_TABLE);
    }

    @Test
    public void shouldInsertPlaybackEvent() throws Exception {
        when(trackingRecord.getUrl()).thenReturn("http://eventlogger.soundcloud.com/audio?keys=values");
        when(trackingRecord.getTimeStamp()).thenReturn(1000L);
        when(trackingRecord.getBackend()).thenReturn("eventlogger");

        trackingStorage.insertEvent(trackingRecord);

        Query events = from(EVENTS_TABLE)
                .whereEq(TrackingDbHelper.TrackingColumns.BACKEND, "eventlogger")
                .whereEq(TrackingDbHelper.TrackingColumns.TIMESTAMP, 1000L)
                .whereEq(TrackingDbHelper.TrackingColumns.URL, "http://eventlogger.soundcloud.com/audio?keys=values");
        assertThat(select(events), counts(1));
    }

    @Test
    public void shouldQueryDatabaseForPendingEventsWithFixedBatchSize() throws UnsupportedEncodingException {
        insertEvents(TrackingStorage.FIXED_BATCH_SIZE + 1);

        final List<TrackingRecord> pendingEvents = trackingStorage.getPendingEvents();

        assertThat(pendingEvents.size(), is(TrackingStorage.FIXED_BATCH_SIZE));
    }

    @Test
    public void shouldQueryDatabaseForAllPendingEventsWhenOnWifi() throws UnsupportedEncodingException {
        insertEvents(TrackingStorage.FIXED_BATCH_SIZE + 1);
        when(networkConnectionHelper.isWifiConnected()).thenReturn(true);

        final List<TrackingRecord> pendingEvents = trackingStorage.getPendingEvents();

        assertThat(pendingEvents.size(), is(TrackingStorage.FIXED_BATCH_SIZE + 1));
    }

    @Test
    public void shouldQueryDatabaseForPendingEventsByBackend() throws UnsupportedEncodingException {
        trackingStorage.insertEvent(new TrackingRecord(1000L, "eventlogger", "url"));
        trackingStorage.insertEvent(new TrackingRecord(2000L, "play_counts", "url"));
        assertThat(select(from(EVENTS_TABLE)), counts(2));

        List<TrackingRecord> playCountEvents = trackingStorage.getPendingEventsForBackend("play_counts");
        assertThat(playCountEvents.size(), is(1));
        assertThat(playCountEvents.get(0).getBackend(), is("play_counts"));
    }

    @Test
    public void shouldQueryDatabaseForAllEvents() throws UnsupportedEncodingException {
        trackingStorage.insertEvent(new TrackingRecord(1000L, "eventlogger", "url1"));
        trackingStorage.insertEvent(new TrackingRecord(2000L, "play_counts", "url2"));
        assertThat(select(from(EVENTS_TABLE)), counts(2));

        List<TrackingRecord> events = trackingStorage.getPendingEvents();
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
        List<TrackingRecord> events = trackingStorage.getPendingEvents();
        assertThat(events.size(), is(2));

        trackingStorage.deleteEvents(events);

        assertThat(select(from(EVENTS_TABLE)), counts(0));
    }

    private List<TrackingRecord> insertEvents(int numEvents) throws UnsupportedEncodingException {
        List<TrackingRecord> events = new ArrayList<TrackingRecord>(numEvents);
        for (int i = 0; i < numEvents; i++) {
            final TrackingRecord event = new TrackingRecord((long) i, "backend", "url");
            trackingStorage.insertEvent(event);
            events.add(event);
        }
        assertThat(select(from(EVENTS_TABLE)), counts(numEvents));
        return events;
    }
}
