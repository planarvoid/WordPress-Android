package com.soundcloud.android.analytics.eventlogger;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.events.PlaybackEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.playback.service.TrackSourceInfo;
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

    private EventLoggerStorage eventLoggerStorage;
    
    @Mock
    EventLoggerDbHelper eventLoggerDbHelper;
    @Mock
    EventLoggerParamsBuilder eventLoggerParamsBuilder;
    @Mock
    SQLiteDatabase sqLiteDatabase;
    @Mock
    TrackSourceInfo trackSourceInfo;
    @Mock
    EventLoggerApi eventLoggerApi;

    final String trackingParams1 = "tracking=params";

    @Before
    public void setUp() throws Exception {
        eventLoggerStorage = new EventLoggerStorage(eventLoggerDbHelper, eventLoggerParamsBuilder);
        when(eventLoggerDbHelper.getWritableDatabase()).thenReturn(sqLiteDatabase);
        when(eventLoggerDbHelper.getReadableDatabase()).thenReturn(sqLiteDatabase);
        when(eventLoggerParamsBuilder.build(trackSourceInfo)).thenReturn(trackingParams1);

    }

    @Test
    public void shouldInsertPlaybackEvent() throws CreateModelException {
        Track track = TestHelper.getModelFactory().createModel(Track.class);
        final PlaybackEvent playbackEvent = PlaybackEvent.forPlay(track, 1l, trackSourceInfo);

        eventLoggerStorage.insertEvent(playbackEvent);

        ArgumentCaptor<ContentValues> valuesArgumentCaptor = ArgumentCaptor.forClass(ContentValues.class);
        verify(sqLiteDatabase).insertOrThrow(eq(EventLoggerDbHelper.EVENTS_TABLE), isNull(String.class), valuesArgumentCaptor.capture());

        ContentValues values = valuesArgumentCaptor.getValue();
        expect(values.get(EventLoggerDbHelper.TrackingEvents.ACTION)).toEqual(EventLoggerParams.Action.PLAY);
        expect(values.get(EventLoggerDbHelper.TrackingEvents.SOUND_URN)).toEqual(Urn.forTrack(track.getId()).toString());
        expect(values.get(EventLoggerDbHelper.TrackingEvents.TIMESTAMP)).toEqual(playbackEvent.getTimeStamp());
        expect(values.get(EventLoggerDbHelper.TrackingEvents.SOUND_DURATION)).toEqual(track.duration);
        expect(values.get(EventLoggerDbHelper.TrackingEvents.USER_URN)).toEqual(Urn.forUser(playbackEvent.getUserId()).toString());
        expect(values.get(EventLoggerDbHelper.TrackingEvents.SOURCE_INFO)).toEqual(trackingParams1);
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
