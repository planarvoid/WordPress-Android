package com.soundcloud.android.tracking.eventlogger;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.TrackTest;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.provider.DBHelper;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import com.soundcloud.android.service.playback.CloudPlaybackService;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;

import java.io.IOException;
import java.util.UUID;

@RunWith(DefaultTestRunner.class)
public class PlayEventTrackerTest {

    private static Track currentTrack, nextTrack;

    private ContentResolver resolver;
    private CloudPlaybackService service;

    @BeforeClass
    public static void setupFixtures() throws IOException {
        currentTrack = TestHelper.readJson(Track.class, TrackTest.class, "track.json");
        currentTrack.id = 1;
        nextTrack = TestHelper.readJson(Track.class, TrackTest.class, "track.json");
        nextTrack.id = 2;
    }

    @Before
    public void setup() {
        resolver = DefaultTestRunner.application.getContentResolver();
        resolver.delete(Content.TRACKING_EVENTS.uri, null, null);

        service = new CloudPlaybackService();
        service.onCreate();
    }

    @After
    public void reset() {
        service.onStartCommand(new Intent(CloudPlaybackService.RESET_ALL), 0, 0);
    }

    @Test
    public void shouldTrackPlayEventForFirstTrack() {
        DefaultTestRunner.application.setCurrentUserId(456);

        startPlaybackService(CloudPlaybackService.PLAY_ACTION, currentTrack);

        Cursor cursor = resolver.query(Content.TRACKING_EVENTS.uri, null, null, null, null);
        expect(cursor.getCount()).toBe(1);

        assertTrackingDataFor(currentTrack, "play", cursor);

        String userUrn = cursor.getString(cursor.getColumnIndex(DBHelper.TrackingEvents.USER_URN));
        expect(userUrn).toEqual("soundcloud:users:456");
    }

    @Test
    public void shouldTrackPlayEventForLoggedOutUser() {
        startPlaybackService(CloudPlaybackService.PLAY_ACTION, currentTrack);

        Cursor cursor = resolver.query(Content.TRACKING_EVENTS.uri, null, null, null, null);
        expect(cursor.getCount()).toBe(1);

        assertTrackingDataFor(currentTrack, "play", cursor);
        String userUrn = cursor.getString(cursor.getColumnIndex(DBHelper.TrackingEvents.USER_URN));
        expect(userUrn).toMatch("^anonymous:.*");

        String uuid = userUrn.replaceAll("anonymous:", "");
        UUID.fromString(uuid); // this will throw if the UUID is invalid
    }

    @Test
    public void shouldTrackPauseEvent() {
        startPlaybackService(CloudPlaybackService.PLAY_ACTION, currentTrack);
        startPlaybackService(CloudPlaybackService.PAUSE_ACTION, currentTrack);

        Cursor cursor = resolver.query(Content.TRACKING_EVENTS.uri, null, null, null, null);
        expect(cursor.getCount()).toBe(2);

        assertTrackingDataFor(currentTrack, "play", cursor);
        assertTrackingDataFor(currentTrack, "stop", cursor);
    }

    @Test
    public void shouldTrackPlayEventForNextTrackAndStopEventForPreviousTrack() {
        startPlaybackService(CloudPlaybackService.PLAY_ACTION, currentTrack);
        startPlaybackService(CloudPlaybackService.PLAY_ACTION, nextTrack);

        Cursor cursor = resolver.query(Content.TRACKING_EVENTS.uri, null, null, null, null);
        expect(cursor.getCount()).toBe(3);

        assertTrackingDataFor(currentTrack, "play", cursor);
        assertTrackingDataFor(currentTrack, "stop", cursor);
        assertTrackingDataFor(nextTrack, "play", cursor);
    }

    @Test
    public void shouldTrackTogglePause() {
        startPlaybackService(CloudPlaybackService.PLAY_ACTION, currentTrack);
        startPlaybackService(CloudPlaybackService.TOGGLEPAUSE_ACTION, null);
        startPlaybackService(CloudPlaybackService.TOGGLEPAUSE_ACTION, null);

        Cursor cursor = resolver.query(Content.TRACKING_EVENTS.uri, null, null, null, null);
        expect(cursor.getCount()).toBe(3);

        assertTrackingDataFor(currentTrack, "play", cursor);
        assertTrackingDataFor(currentTrack, "stop", cursor);
        assertTrackingDataFor(currentTrack, "play", cursor);
    }

    @Test
    public void shouldNotRecordPlayTwice() {
        startPlaybackService(CloudPlaybackService.PLAY_ACTION, currentTrack);
        startPlaybackService(CloudPlaybackService.PLAY_ACTION, currentTrack);

        Cursor cursor = resolver.query(Content.TRACKING_EVENTS.uri, null, null, null, null);
        expect(cursor.getCount()).toBe(1);

        assertTrackingDataFor(currentTrack, "play", cursor);
    }

    @Test
    public void shouldNotRecordStopTwice() {
        startPlaybackService(CloudPlaybackService.PLAY_ACTION, currentTrack);
        startPlaybackService(CloudPlaybackService.PAUSE_ACTION, currentTrack);
        startPlaybackService(CloudPlaybackService.PAUSE_ACTION, currentTrack);

        Cursor cursor = resolver.query(Content.TRACKING_EVENTS.uri, null, null, null, null);
        expect(cursor.getCount()).toBe(2);

        assertTrackingDataFor(currentTrack, "play", cursor);
        assertTrackingDataFor(currentTrack, "stop", cursor);
    }

    private void assertTrackingDataFor(Track track, String expectedAction, Cursor cursor) {
        cursor.moveToNext();
        long timestamp = cursor.getLong(cursor.getColumnIndex(DBHelper.TrackingEvents.TIMESTAMP));
        expect(timestamp).toBeGreaterThan(1361807068947L); // time this test was written :)

        String action = cursor.getString(cursor.getColumnIndex(DBHelper.TrackingEvents.ACTION));
        expect(action).toEqual(expectedAction);

        String soundUrn = cursor.getString(cursor.getColumnIndex(DBHelper.TrackingEvents.SOUND_URN));
        expect(soundUrn).toEqual("soundcloud:sounds:" + track.id);

        long duration = cursor.getLong(cursor.getColumnIndex(DBHelper.TrackingEvents.SOUND_DURATION));
        expect(duration).toEqual(Long.valueOf(track.duration));
    }

    private CloudPlaybackService startPlaybackService(String action, Track track) {
        Intent intent = new Intent(action);
        if (track != null) {
            intent.putExtra(Track.EXTRA, track);
        }
        service.onStartCommand(intent, 0, 0);

        return service;
    }

}
