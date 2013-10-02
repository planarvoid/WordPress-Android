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
                database.execSQL("DROP TABLE IF EXISTS " + PlayEventTracker.TrackingDbHelper.EVENTS_TABLE);
            }
        });
    }

    @Test
    public void shouldInsertTrackingEventsIntoDatabase() throws Exception {

        Track track = new Track();
        track.duration = 123;
        track.setId(10);

        final PlaySourceInfo playSourceInfo1 = new PlaySourceInfo("origin-url", 123L, "explore-tag", "version_1");
        final TrackSourceInfo trackSourceInfo = TrackSourceInfo.auto();
        tracker.trackEvent(track, Action.PLAY, 1l, playSourceInfo1, trackSourceInfo);

        final PlaySourceInfo sourceTrackingInfo2 = new PlaySourceInfo("origin-url2", 456L, "explore-tag2", "version_2");
        final TrackSourceInfo trackSourceInfo2 = TrackSourceInfo.fromRecommender("version_3");
        tracker.trackEvent(track, Action.STOP, 2l, sourceTrackingInfo2, trackSourceInfo2);

        Cursor cursor = tracker.eventsCursor();

        expect(cursor).toHaveCount(2);
        expect(cursor).toHaveNext();

        expect(cursor).toHaveColumn(TrackingEvents.SOUND_DURATION, 123);
        expect(cursor).toHaveColumn(TrackingEvents.SOUND_URN, "soundcloud:sounds:10");
        expect(cursor).toHaveColumn(TrackingEvents.USER_URN, "soundcloud:users:1");
        expect(cursor).toHaveColumn(TrackingEvents.ACTION, "play");
        expect(cursor).toHaveColumn(TrackingEvents.SOURCE_INFO, "context=origin-url&exploreTag=explore-tag&trigger=auto");
        expect(cursor).toHaveColumn(TrackingEvents.TIMESTAMP);

        expect(cursor).toHaveNext();
        expect(cursor).toHaveColumn(TrackingEvents.ACTION, "stop");
        expect(cursor).toHaveColumn(TrackingEvents.USER_URN, "soundcloud:users:2");
        expect(cursor).toHaveColumn(TrackingEvents.SOURCE_INFO, "context=origin-url2&exploreTag=explore-tag2&trigger=auto&source=recommender&source_version=version_3");
        expect(cursor).not.toHaveNext();
    }

    @Test
    public void shouldFlushEventsToApi() throws Exception {
        when(api.pushToRemote(anyList())).thenReturn(new String[]{"1"});

        tracker.trackEvent(new Track(), Action.PLAY, 1l, new PlaySourceInfo("originUrl", 1L), TrackSourceInfo.auto());
        tracker.trackEvent(new Track(), Action.STOP, 2l, new PlaySourceInfo("originUrl", 1L), TrackSourceInfo.auto());

        expect(tracker.flushPlaybackTrackingEvents()).toBeTrue();

        Cursor cursor = tracker.eventsCursor();
        expect(cursor).toHaveCount(1);
        expect(cursor).toHaveNext();
        expect(cursor).toHaveColumn(TrackingEvents.ACTION, "stop");
        expect(cursor).not.toHaveNext();
    }

    @Test
    public void shouldNotFlushIfNoActiveNetwork() throws Exception {
        tracker.trackEvent(new Track(), Action.PLAY, 1l, new PlaySourceInfo("origin-url", 123L, "explore-tag", "version_1"), TrackSourceInfo.auto());

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
