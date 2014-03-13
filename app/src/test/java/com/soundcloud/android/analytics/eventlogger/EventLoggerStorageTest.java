package com.soundcloud.android.analytics.eventlogger;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.events.PlaybackEvent;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.playback.service.TrackSourceInfo;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;

import android.content.ContentValues;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Pair;

import java.io.UnsupportedEncodingException;
import java.util.List;


@RunWith(SoundCloudTestRunner.class)
public class EventLoggerStorageTest {
    private final String params = "keys=values";

    private PlaybackEvent playbackEvent;
    private EventLoggerStorage eventLoggerStorage;
    
    @Mock
    private EventLoggerDbHelper eventLoggerDbHelper;
    @Mock
    private EventLoggerParamsBuilder eventLoggerParamsBuilder;
    @Mock
    private SQLiteDatabase sqLiteDatabase;
    @Mock
    private TrackSourceInfo trackSourceInfo;
    @Mock
    private EventLoggerApi eventLoggerApi;

    @Before
    public void setUp() throws Exception {
        eventLoggerStorage = new EventLoggerStorage(eventLoggerDbHelper, eventLoggerParamsBuilder);
        playbackEvent = PlaybackEvent.forPlay(Mockito.mock(Track.class), 123L, Mockito.mock(TrackSourceInfo.class));
        when(eventLoggerDbHelper.getWritableDatabase()).thenReturn(sqLiteDatabase);
        when(eventLoggerDbHelper.getReadableDatabase()).thenReturn(sqLiteDatabase);
        when(eventLoggerParamsBuilder.buildFromPlaybackEvent(playbackEvent)).thenReturn(params);

    }

    @Test
    public void shouldInsertPlaybackEvent() throws Exception {
        eventLoggerStorage.insertEvent(playbackEvent);

        ArgumentCaptor<ContentValues> valuesArgumentCaptor = ArgumentCaptor.forClass(ContentValues.class);
        verify(sqLiteDatabase).insertOrThrow(eq(EventLoggerDbHelper.EVENTS_TABLE), isNull(String.class), valuesArgumentCaptor.capture());

        ContentValues values = valuesArgumentCaptor.getValue();
        expect(values.get(EventLoggerDbHelper.TrackingEvents.PATH)).toEqual(EventLoggerEventTypes.PLAYBACK.getPath());
        expect(values.get(EventLoggerDbHelper.TrackingEvents.TIMESTAMP)).toEqual(playbackEvent.getTimeStamp());
        expect(values.get(EventLoggerDbHelper.TrackingEvents.PARAMS)).toEqual("keys=values");
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
