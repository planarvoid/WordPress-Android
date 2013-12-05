package com.soundcloud.android.tracking.eventlogger;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.events.PlaybackEventData;
import com.soundcloud.android.model.ClientUri;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import com.tobedevoured.modelcitizen.CreateModelException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import android.content.ContentValues;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Pair;

import java.io.UnsupportedEncodingException;
import java.util.List;


@RunWith(SoundCloudTestRunner.class)
public class EventLoggerStorageTest {

    EventLoggerStorage eventLoggerStorage;
    @Mock
    EventLoggerDbHelper eventLoggerDbHelper;
    @Mock
    SQLiteDatabase sqLiteDatabase;
    @Mock
    EventLoggerApi eventLoggerApi;

    final String trackingParams1 = "tracking=params";

    @Before
    public void setUp() throws Exception {
        eventLoggerStorage = new EventLoggerStorage(eventLoggerDbHelper);
        when(eventLoggerDbHelper.getWritableDatabase()).thenReturn(sqLiteDatabase);
        when(eventLoggerDbHelper.getReadableDatabase()).thenReturn(sqLiteDatabase);
    }

    @Test
    public void shouldInsertPlaybackEvent() throws CreateModelException {
        Track track = TestHelper.getModelFactory().createModel(Track.class);
        final PlaybackEventData playbackEventData = new PlaybackEventData(track, Action.PLAY, 1l, trackingParams1);

        eventLoggerStorage.insertEvent(playbackEventData);

        ArgumentCaptor<ContentValues> valuesArgumentCaptor = ArgumentCaptor.forClass(ContentValues.class);
        verify(sqLiteDatabase).insertOrThrow(eq(EventLoggerDbHelper.EVENTS_TABLE), isNull(String.class), valuesArgumentCaptor.capture());

        ContentValues values = valuesArgumentCaptor.getValue();
        expect(values.get(EventLoggerDbHelper.TrackingEvents.ACTION)).toEqual(Action.PLAY.toApiName());
        expect(values.get(EventLoggerDbHelper.TrackingEvents.SOUND_URN)).toEqual(ClientUri.forTrack(track.getId()).toString());
        expect(values.get(EventLoggerDbHelper.TrackingEvents.TIMESTAMP)).toEqual(playbackEventData.getTimeStamp());
        expect(values.get(EventLoggerDbHelper.TrackingEvents.SOUND_DURATION)).toEqual(track.duration);
        expect(values.get(EventLoggerDbHelper.TrackingEvents.USER_URN)).toEqual(EventLoggerStorage.buildUserUrn(playbackEventData.getUserId()));
        expect(values.get(EventLoggerDbHelper.TrackingEvents.SOURCE_INFO)).toEqual(playbackEventData.getEventLoggerParams());
    }

    @Test
    public void shouldQueryDatabaseForUnPushedEvents(){
        eventLoggerStorage.getUnpushedEvents(eventLoggerApi);
        verify(sqLiteDatabase).query(EventLoggerDbHelper.EVENTS_TABLE, null, null, null, null, null,
                EventLoggerDbHelper.TrackingEvents.TIMESTAMP + " DESC", String.valueOf(EventLoggerHandler.BATCH_SIZE));
    }

    @Test
    public void shouldReturnUnpushedEvents() throws UnsupportedEncodingException {
        MatrixCursor cursor = new MatrixCursor(new String[]{EventLoggerDbHelper.TrackingEvents._ID});
        cursor.addRow(new Object[]{123L});

        when(eventLoggerApi.buildUrl(cursor)).thenReturn("url");
        when(sqLiteDatabase.query(EventLoggerDbHelper.EVENTS_TABLE, null, null, null, null, null,
                EventLoggerDbHelper.TrackingEvents.TIMESTAMP + " DESC", String.valueOf(EventLoggerHandler.BATCH_SIZE))).thenReturn(cursor);

        List<Pair<Long,String>> ret = eventLoggerStorage.getUnpushedEvents(eventLoggerApi);
        expect(ret.size()).toBe(1);
        expect(ret.get(0)).toEqual(new Pair<Long, String>(123L, "url"));
    }

    @Test
    public void shouldDeleteEvents() throws Exception {
        final String[] submittedIds = {"id1", "id2"};
        eventLoggerStorage.deleteEventsById(submittedIds);
        verify(sqLiteDatabase).delete(EventLoggerDbHelper.EVENTS_TABLE, "_id IN (?,?)" ,submittedIds);
    }
}
