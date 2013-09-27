package com.soundcloud.android.tracking.eventlogger;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.model.Track;
import com.soundcloud.android.model.TrackTest;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import com.soundcloud.android.service.playback.CloudPlaybackService;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import scala.actors.threadpool.Arrays;

import android.content.Intent;
import android.database.Cursor;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Ignore // TODO: fix the problem with Robolectric's multi-database handling
@RunWith(DefaultTestRunner.class)
public class PlayEventTrackerIntegrationTest {

    private static Track currentTrack, nextTrack;

    private CloudPlaybackService service;
    private PlayEventTracker tracker;

    @BeforeClass
    public static void setupFixtures() throws IOException {
        currentTrack = TestHelper.readJson(Track.class, TrackTest.class, "track.json");
        currentTrack.setId(1);
        nextTrack = TestHelper.readJson(Track.class, TrackTest.class, "track.json");
        nextTrack.setId(2);
    }

    @Before
    public void setup() {
        TestHelper.setUserId(1);
        List<Track> tracks = Arrays.asList(new Track[] { currentTrack, nextTrack });

//        SoundCloudDB.insertCollection(DefaultTestRunner.application.getContentResolver(),
//                tracks,
//                Content.ME_LIKES.uri,
//                1);
//
        expect(Content.ME_LIKES).toHaveCount(2);

        service = new CloudPlaybackService();
        service.onCreate();

        service.getPlayQueueManager().loadUri(Content.ME_LIKES.uri, 0, currentTrack, new PlaySourceTrackingInfo("asdf"));

        tracker = service.getPlayEventTracker();
    }

    @After
    public void reset() {
        service.onStartCommand(new Intent(CloudPlaybackService.Actions.RESET_ALL), 0, 0);
    }

    @Test
    public void shouldTrackPlayEventForFirstTrack() {
        TestHelper.setUserId(456);

        startPlaybackService(CloudPlaybackService.Actions.PLAY_ACTION, currentTrack);

        Cursor cursor = tracker.eventsCursor();
        expect(cursor.getCount()).toBe(1);

        assertTrackingDataFor(currentTrack, "play", cursor);

        String userUrn = cursor.getString(cursor.getColumnIndex(PlayEventTracker.TrackingEvents.USER_URN));
        expect(userUrn).toEqual("soundcloud:users:456");
    }

    @Test
    public void shouldTrackPlayEventForLoggedOutUser() {
        TestHelper.setUserId(-1);

        startPlaybackService(CloudPlaybackService.Actions.PLAY_ACTION, currentTrack);

        Cursor cursor = tracker.eventsCursor();
        expect(cursor.getCount()).toBe(1);

        assertTrackingDataFor(currentTrack, "play", cursor);
        String userUrn = cursor.getString(cursor.getColumnIndex(PlayEventTracker.TrackingEvents.USER_URN));
        expect(userUrn).toMatch("^anonymous:.*");

        String uuid = userUrn.replaceAll("anonymous:", "");
        UUID.fromString(uuid); // this will throw if the UUID is invalid
    }

    @Test
    public void shouldTrackPauseEvent() {
        startPlaybackService(CloudPlaybackService.Actions.PLAY_ACTION, currentTrack);
        startPlaybackService(CloudPlaybackService.Actions.PAUSE_ACTION, currentTrack);

        Cursor cursor = tracker.eventsCursor();
        expect(cursor.getCount()).toBe(2);

        assertTrackingDataFor(currentTrack, "play", cursor);
        assertTrackingDataFor(currentTrack, "stop", cursor);
    }

    @Test
    public void shouldTrackPlayEventForNextTrackAndStopEventForPreviousTrack() {
        startPlaybackService(CloudPlaybackService.Actions.PLAY_ACTION, currentTrack);
        startPlaybackService(CloudPlaybackService.Actions.PLAY_ACTION, nextTrack);

        Cursor cursor = tracker.eventsCursor();
        expect(cursor.getCount()).toBe(3);

        assertTrackingDataFor(currentTrack, "play", cursor);
        assertTrackingDataFor(currentTrack, "stop", cursor);
        assertTrackingDataFor(nextTrack, "play", cursor);
    }

    @Test
    public void shouldTrackTogglePause() {
        startPlaybackService(CloudPlaybackService.Actions.PLAY_ACTION, currentTrack);
        startPlaybackService(CloudPlaybackService.Actions.TOGGLEPLAYBACK_ACTION, null);
        startPlaybackService(CloudPlaybackService.Actions.TOGGLEPLAYBACK_ACTION, null);

        Cursor cursor = tracker.eventsCursor();
        expect(cursor.getCount()).toBe(3);

        assertTrackingDataFor(currentTrack, "play", cursor);
        assertTrackingDataFor(currentTrack, "stop", cursor);
        assertTrackingDataFor(currentTrack, "play", cursor);
    }

    @Test @Ignore
    public void shouldTrackNextEvent() throws IOException {
        startPlaybackService(CloudPlaybackService.Actions.PLAY_ACTION, null);
        startPlaybackService(CloudPlaybackService.Actions.NEXT_ACTION, null);

        Cursor cursor = tracker.eventsCursor();
        expect(cursor.getCount()).toBe(3);

        assertTrackingDataFor(currentTrack, "play", cursor);
        assertTrackingDataFor(currentTrack, "stop", cursor);
        assertTrackingDataFor(nextTrack, "play", cursor);
    }

    @Test
    public void shouldNotRecordPlayTwice() {
        startPlaybackService(CloudPlaybackService.Actions.PLAY_ACTION, currentTrack);
        startPlaybackService(CloudPlaybackService.Actions.PLAY_ACTION, currentTrack);

        Cursor cursor = tracker.eventsCursor();
        expect(cursor.getCount()).toBe(1);

        assertTrackingDataFor(currentTrack, "play", cursor);
    }

    @Test
    public void shouldNotRecordStopTwice() {
        startPlaybackService(CloudPlaybackService.Actions.PLAY_ACTION, currentTrack);
        startPlaybackService(CloudPlaybackService.Actions.PAUSE_ACTION, currentTrack);
        startPlaybackService(CloudPlaybackService.Actions.PAUSE_ACTION, currentTrack);

        Cursor cursor = tracker.eventsCursor();
        expect(cursor.getCount()).toBe(2);

        assertTrackingDataFor(currentTrack, "play", cursor);
        assertTrackingDataFor(currentTrack, "stop", cursor);
    }

    @Test
    public void pressingNextshouldNotRecordStopAgainAfterPlayPauseCycle() {
        startPlaybackService(CloudPlaybackService.Actions.PLAY_ACTION, currentTrack);
        startPlaybackService(CloudPlaybackService.Actions.PAUSE_ACTION, currentTrack);
        startPlaybackService(CloudPlaybackService.Actions.PAUSE_ACTION, currentTrack);

        Cursor cursor = tracker.eventsCursor();
        expect(cursor.getCount()).toBe(2);

        assertTrackingDataFor(currentTrack, "play", cursor);
        assertTrackingDataFor(currentTrack, "stop", cursor);
    }

    private void assertTrackingDataFor(Track track, String expectedAction, Cursor cursor) {
        cursor.moveToNext();
        long timestamp = cursor.getLong(cursor.getColumnIndex(PlayEventTracker.TrackingEvents.TIMESTAMP));
        expect(timestamp).toBeGreaterThan(1361807068947L); // time this test was written :)

        String action = cursor.getString(cursor.getColumnIndex(PlayEventTracker.TrackingEvents.ACTION));
        expect(action).toEqual(expectedAction);

        String soundUrn = cursor.getString(cursor.getColumnIndex(PlayEventTracker.TrackingEvents.SOUND_URN));
        expect(soundUrn).toEqual("soundcloud:sounds:" + track.getId());

        long duration = cursor.getLong(cursor.getColumnIndex(PlayEventTracker.TrackingEvents.SOUND_DURATION));
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
