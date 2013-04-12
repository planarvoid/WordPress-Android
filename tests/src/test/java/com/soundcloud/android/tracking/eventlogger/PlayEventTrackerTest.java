package com.soundcloud.android.tracking.eventlogger;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.tracking.eventlogger.PlayEventTracker.TrackingEvents;
import static org.mockito.Mockito.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.model.Track;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import com.xtremelabs.robolectric.util.DatabaseConfig;
import com.xtremelabs.robolectric.util.SQLiteMap;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

@RunWith(DefaultTestRunner.class)
@DatabaseConfig.UsingDatabaseMap(PlayEventTrackerTest.PlayEventFileDatabaseMap.class)
public class PlayEventTrackerTest {
    PlayEventTracker tracker;
    PlayEventTrackingApi api;

    @Before
    public void before() {
        api = mock(PlayEventTrackingApi.class);
        tracker = new PlayEventTracker(DefaultTestRunner.application, api);
        tracker.getTrackingDbHelper().execute(new PlayEventTracker.TrackingDbHelper.ExecuteBlock() {
            @Override
            public void call(SQLiteDatabase database) {
                database.delete(PlayEventTracker.TrackingDbHelper.EVENTS_TABLE, null, null);
            }
        });
    }

    @Test
    public void shouldInsertTrackingEventsIntoDatabase() throws Exception {

        Track track = new Track();
        track.duration = 123;
        track.id = 10;

        tracker.trackEvent(track, Action.PLAY, 1l, "originUrl", "level");
        tracker.trackEvent(new Track(), Action.STOP, 2l, "originUrl", "level");

        Cursor cursor = tracker.eventsCursor();

        expect(cursor).toHaveCount(2);
        expect(cursor).toHaveNext();

        expect(cursor).toHaveColumn(TrackingEvents.SOUND_DURATION, 123);
        expect(cursor).toHaveColumn(TrackingEvents.SOUND_URN, "soundcloud:sounds:10");
        expect(cursor).toHaveColumn(TrackingEvents.USER_URN, "soundcloud:users:1");
        expect(cursor).toHaveColumn(TrackingEvents.ACTION, "play");
        expect(cursor).toHaveColumn(TrackingEvents.ORIGIN_URL, "originUrl");
        expect(cursor).toHaveColumn(TrackingEvents.LEVEL, "level");
        expect(cursor).toHaveColumn(TrackingEvents.TIMESTAMP);

        expect(cursor).toHaveNext();
        expect(cursor).toHaveColumn(TrackingEvents.ACTION, "stop");
        expect(cursor).toHaveColumn(TrackingEvents.USER_URN, "soundcloud:users:2");

        expect(cursor).not.toHaveNext();
    }

    @Test
    public void shouldFlushEventsToApi() throws Exception {
        when(api.pushToRemote(anyList())).thenReturn(new String[]{"1"});

        tracker.trackEvent(new Track(), Action.PLAY, 1l, "originUrl", "level");
        tracker.trackEvent(new Track(), Action.STOP, 2l, "originUrl", "level");

        expect(tracker.flushPlaybackTrackingEvents()).toBeTrue();

        Cursor cursor = tracker.eventsCursor();

        expect(cursor).toHaveCount(1);
        expect(cursor).toHaveNext();
        expect(cursor).toHaveColumn(TrackingEvents.ACTION, "stop");
        expect(cursor).not.toHaveNext();
    }

    @Test
    public void shouldNotFlushIfNoActiveNetwork() throws Exception {
        tracker.trackEvent(new Track(), Action.PLAY, 1l, "originUrl", "level");

        TestHelper.simulateOffline();
        expect(tracker.flushPlaybackTrackingEvents()).toBeTrue();
        verifyZeroInteractions(api);

        TestHelper.simulateOnline();

        when(api.pushToRemote(anyList())).thenReturn(new String[]{"1"});
        expect(tracker.flushPlaybackTrackingEvents()).toBeTrue();
    }

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
