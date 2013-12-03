package com.soundcloud.android.tracking.eventlogger;

import static org.mockito.Mockito.mock;

import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.xtremelabs.robolectric.util.DatabaseConfig;
import com.xtremelabs.robolectric.util.SQLiteMap;
import org.junit.Before;
import org.junit.runner.RunWith;

@RunWith(DefaultTestRunner.class)
@DatabaseConfig.UsingDatabaseMap(PlayEventTrackerTest.PlayEventFileDatabaseMap.class)
public class PlayEventTrackerTest {
    PlayEventTracker tracker;
    PlayEventTrackingApi api;

    final String trackingParams1 = "tracking=params";
    final String trackingParams2 = "tracking=params2";

    @Before
    public void before() {
        api = mock(PlayEventTrackingApi.class);
//        tracker = new PlayEventTracker(DefaultTestRunner.application, api);
//        tracker.getTrackingDbHelper().execute(new PlayEventTrackingDbHelper.ExecuteBlock() {
//            @Override
//            public void call(SQLiteDatabase database) {
//                database.execSQL("DROP TABLE IF EXISTS " + PlayEventTrackingDbHelper.EVENTS_TABLE);
//            }
//        });
    }

//    @Test
//    public void shouldInsertTrackingEventsIntoDatabase() throws Exception {
//
//        Track track = new Track();
//        track.duration = 123;
//        track.setId(10);
//
//        tracker.trackEvent(new PlaybackEventData(track, Action.PLAY, 1l, trackingParams1));
//        tracker.trackEvent(new PlaybackEventData(track, Action.STOP, 2l, trackingParams2));
//
//        Cursor cursor = tracker.eventsCursor();
//
//        expect(cursor).toHaveCount(2);
//        expect(cursor).toHaveNext();
//
//        expect(cursor).toHaveColumn(TrackingEvents.SOUND_DURATION, 123);
//        expect(cursor).toHaveColumn(TrackingEvents.SOUND_URN, "soundcloud:sounds:10");
//        expect(cursor).toHaveColumn(TrackingEvents.USER_URN, "soundcloud:users:1");
//        expect(cursor).toHaveColumn(TrackingEvents.ACTION, "play");
//        expect(cursor).toHaveColumn(TrackingEvents.SOURCE_INFO, "tracking=params");
//        expect(cursor).toHaveColumn(TrackingEvents.TIMESTAMP);
//
//        expect(cursor).toHaveNext();
//        expect(cursor).toHaveColumn(TrackingEvents.ACTION, "stop");
//        expect(cursor).toHaveColumn(TrackingEvents.USER_URN, "soundcloud:users:2");
//        expect(cursor).toHaveColumn(TrackingEvents.SOURCE_INFO, "tracking=params2");
//        expect(cursor).not.toHaveNext();
//    }
//
//    @Test
//    public void shouldFlushEventsToApi() throws Exception {
//        when(api.pushToRemote(anyList())).thenReturn(new String[]{"1"});
//
//        tracker.trackEvent(new PlaybackEventData(new Track(), Action.PLAY, 1l, trackingParams1));
//        tracker.trackEvent(new PlaybackEventData(new Track(), Action.STOP, 2l, trackingParams2));
//
//        final EventLoggerHandler handler = tracker.getHandler();
//        handler.sendMessage(handler.obtainMessage(PlayEventTracker.FLUSH_TOKEN));
//
//        Cursor cursor = tracker.eventsCursor();
//        expect(cursor).toHaveCount(1);
//        expect(cursor).toHaveNext();
//        expect(cursor).toHaveColumn(TrackingEvents.ACTION, "stop");
//        expect(cursor).not.toHaveNext();
//    }
//
//    @Test
//    public void shouldNotFlushIfNoActiveNetwork() throws Exception {
//        tracker.trackEvent(new PlaybackEventData(new Track(), Action.PLAY, 1l, trackingParams1));
//
//        TestHelper.simulateOffline();
//        final EventLoggerHandler handler = tracker.getHandler();
//        handler.sendMessage(handler.obtainMessage(PlayEventTracker.FLUSH_TOKEN));
//
//        verifyZeroInteractions(api);
//
//        TestHelper.simulateOnline();
//
//        when(api.pushToRemote(anyList())).thenReturn(new String[]{"1"});
//        handler.sendMessage(handler.obtainMessage(PlayEventTracker.FLUSH_TOKEN));
//    }



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
