package com.soundcloud.android.analytics;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.analytics.TrackingDbHelper.EVENTS_TABLE;
import static com.soundcloud.propeller.query.Query.from;
import static com.soundcloud.propeller.test.matchers.QueryMatchers.counts;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.when;

import com.soundcloud.android.playback.TrackSourceInfo;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.testsupport.StorageIntegrationTest;
import com.soundcloud.android.utils.NetworkConnectionHelper;
import com.soundcloud.propeller.ChangeResult;
import com.soundcloud.propeller.PropellerDatabase;
import com.soundcloud.propeller.PropellerWriteException;
import com.soundcloud.propeller.query.Query;
import com.soundcloud.propeller.query.Where;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.io.UnsupportedEncodingException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;


@RunWith(SoundCloudTestRunner.class)
public class TrackingStorageTest extends StorageIntegrationTest {

    private TrackingStorage trackingStorage;

    @Mock private NetworkConnectionHelper networkConnectionHelper;
    @Mock private TrackSourceInfo trackSourceInfo;
    @Mock private TrackingApi trackingApi;
    @Mock private TrackingRecord trackingRecord;

    @Before
    public void setUp() throws Exception {
        trackingStorage = new TrackingStorage(propeller(), networkConnectionHelper);
        database().execSQL("DROP TABLE IF EXISTS " + EVENTS_TABLE.name());
        database().execSQL(TrackingDbHelper.DATABASE_CREATE_EVENTS_TABLE);
    }

    @Test
    public void shouldInsertPlaybackEvent() throws Exception {
        when(trackingRecord.getData()).thenReturn("http://eventlogger.soundcloud.com/audio?keys=values");
        when(trackingRecord.getTimeStamp()).thenReturn(1000L);
        when(trackingRecord.getBackend()).thenReturn("eventlogger");

        trackingStorage.insertEvent(trackingRecord);

        Query events = from(EVENTS_TABLE.name())
                .whereEq(TrackingDbHelper.TrackingColumns.BACKEND, "eventlogger")
                .whereEq(TrackingDbHelper.TrackingColumns.TIMESTAMP, 1000L)
                .whereEq(TrackingDbHelper.TrackingColumns.DATA, "http://eventlogger.soundcloud.com/audio?keys=values");
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
        assertThat(select(from(EVENTS_TABLE.name())), counts(2));

        List<TrackingRecord> playCountEvents = trackingStorage.getPendingEventsForBackend("play_counts");
        assertThat(playCountEvents.size(), is(1));
        assertThat(playCountEvents.get(0).getBackend(), is("play_counts"));
    }

    @Test
    public void shouldQueryDatabaseForAllEvents() throws UnsupportedEncodingException {
        trackingStorage.insertEvent(new TrackingRecord(1000L, "eventlogger", "url1"));
        trackingStorage.insertEvent(new TrackingRecord(2000L, "play_counts", "url2"));
        assertThat(select(from(EVENTS_TABLE.name())), counts(2));

        List<TrackingRecord> events = trackingStorage.getPendingEvents();
        assertThat(events.size(), is(2));
        assertThat(events.get(0).getTimeStamp(), is(1000L));
        assertThat(events.get(0).getBackend(), is("eventlogger"));
        assertThat(events.get(0).getData(), is("url1"));
        assertThat(events.get(1).getTimeStamp(), is(2000L));
        assertThat(events.get(1).getBackend(), is("play_counts"));
        assertThat(events.get(1).getData(), is("url2"));
    }

    @Test
    public void shouldDeleteEventsWithCountLessThanBatch() throws Exception {
        assertBatchDelete(TrackingStorage.FIXED_BATCH_SIZE - 1);
    }

    @Test
    public void shouldDeleteEventsWithCountEqualToBatch() throws Exception {
        assertBatchDelete(TrackingStorage.FIXED_BATCH_SIZE);
    }

    @Test
    public void shouldDeleteEventsWithCountLargerThanBatch() throws Exception {
        assertBatchDelete(TrackingStorage.FIXED_BATCH_SIZE + 1);
    }

    private void assertBatchDelete(int batchSize) throws UnsupportedEncodingException {
        when(networkConnectionHelper.isWifiConnected()).thenReturn(true); // allows > fixed batch size events
        insertEvents(batchSize);
        List<TrackingRecord> events = trackingStorage.getPendingEvents();
        assertThat(events.size(), is(batchSize));

        expect(trackingStorage.deleteEvents(events).getNumRowsAffected()).toEqual(batchSize);

        assertThat(select(from(EVENTS_TABLE.name())), counts(0));
    }

    @Test
    public void shouldReturnAffectedRowsWhenFailingToDeleteSecondBatch() throws Exception {
        final int batchSize = TrackingStorage.FIXED_BATCH_SIZE + 1;
        final PropellerDatabase propeller = Mockito.mock(PropellerDatabase.class);
        final ChangeResult failResult = Mockito.mock(ChangeResult.class);
        when(failResult.getFailure()).thenReturn(new PropellerWriteException("error", new SQLException()));
        when(propeller.delete(same(EVENTS_TABLE), any(Where.class))).thenReturn(new ChangeResult(TrackingStorage.FIXED_BATCH_SIZE), failResult);
        when(networkConnectionHelper.isWifiConnected()).thenReturn(true);

        trackingStorage = new TrackingStorage(propeller, networkConnectionHelper);
        List<TrackingRecord> events = createEvents(batchSize);

        final ChangeResult changeResult = trackingStorage.deleteEvents(events);

        expect(changeResult.getNumRowsAffected()).toEqual(TrackingStorage.FIXED_BATCH_SIZE);
        expect(changeResult.success()).toBeFalse();
    }

    private List<TrackingRecord> insertEvents(int numEvents) throws UnsupportedEncodingException {
        List<TrackingRecord> events = new ArrayList<>(numEvents);
        for (int i = 0; i < numEvents; i++) {
            final TrackingRecord event = new TrackingRecord((long) i, "backend", "url");
            trackingStorage.insertEvent(event);
            events.add(event);
        }
        assertThat(select(from(EVENTS_TABLE.name())), counts(numEvents));
        return events;
    }

    private List<TrackingRecord> createEvents(int numEvents) throws UnsupportedEncodingException {
        List<TrackingRecord> events = new ArrayList<>(numEvents);
        for (int i = 0; i < numEvents; i++) {
            final TrackingRecord event = new TrackingRecord((long) i, "backend", "url");
            events.add(event);
        }
        return events;
    }
}
