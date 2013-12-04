package com.soundcloud.android.tracking.eventlogger;

import com.xtremelabs.robolectric.util.DatabaseConfig;
import com.xtremelabs.robolectric.util.SQLiteMap;


@DatabaseConfig.UsingDatabaseMap(EventLoggerStorageTest.PlayEventFileDatabaseMap.class)
public class EventLoggerStorageTest {

//    SQLiteDatabase database = Mockito.mock(SQLiteDatabase.class);
//    captor.getValue().call(database);
//
//    ArgumentCaptor<ContentValues> valuesArgumentCaptor = ArgumentCaptor.forClass(ContentValues.class);
//    verify(database).insertOrThrow(eq(EventLoggerDbHelper.EVENTS_TABLE), isNull(String.class), valuesArgumentCaptor.capture());
//
//    ContentValues values = valuesArgumentCaptor.getValue();
//    expect(values.get(EventLoggerDbHelper.TrackingEvents.ACTION)).toEqual(Action.PLAY.toApiName());
//    expect(values.get(EventLoggerDbHelper.TrackingEvents.SOUND_URN)).toEqual(ClientUri.forTrack(track.getId()).toString());
//    expect(values.get(EventLoggerDbHelper.TrackingEvents.TIMESTAMP)).toEqual(playbackEventData1.getTimeStamp());
//    expect(values.get(EventLoggerDbHelper.TrackingEvents.SOUND_DURATION)).toEqual(track.duration);
//    expect(values.get(EventLoggerDbHelper.TrackingEvents.USER_URN)).toEqual(EventLoggerHandler.buildUserUrn(playbackEventData1.getUserId()));
//    expect(values.get(EventLoggerDbHelper.TrackingEvents.SOURCE_INFO)).toEqual(playbackEventData1.getEventLoggerParams());

    /*
  The play event tracker opens/closes databases during each operation to avoid locking issues, so
  the file database is to prevent data loss from roboelectric's in-memory database shortcomings.
  see : http://stackoverflow.com/questions/7320820/testing-sqlite-database-in-robolectric
   */
    public static class PlayEventFileDatabaseMap extends SQLiteMap {
        @Override
        public String getConnectionString() {
            return "jdbc:sqlite:tests-play-events.sqlite";
        }
    }
}
