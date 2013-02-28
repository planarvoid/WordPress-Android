package com.soundcloud.android.tracking.eventlogger;

import android.database.Cursor;
import com.soundcloud.android.model.Track;
import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.robolectric.TestHelper;
import org.junit.Test;
import org.junit.runner.RunWith;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.tracking.eventlogger.PlayEventTracker.TrackingEvents;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

@RunWith(DefaultTestRunner.class)
public class PlayEventTrackerTest {

    @Test
    public void shouldInsertTrackingEventsIntoDatabase() throws Exception {
        PlayEventTrackingApi api = mock(PlayEventTrackingApi.class);

        PlayEventTracker tracker = new PlayEventTracker(DefaultTestRunner.application, api);
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
        PlayEventTrackingApi api = mock(PlayEventTrackingApi.class);

        when(api.pushToRemote(any(Cursor.class))).thenReturn(new String[] {"1"} );

        PlayEventTracker tracker = new PlayEventTracker(DefaultTestRunner.application, api);
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
        PlayEventTrackingApi api = mock(PlayEventTrackingApi.class);
        PlayEventTracker tracker = new PlayEventTracker(DefaultTestRunner.application, api);
        tracker.trackEvent(new Track(), Action.PLAY, 1l, "originUrl", "level");

        TestHelper.simulateOffline();
        expect(tracker.flushPlaybackTrackingEvents()).toBeTrue();
        verifyZeroInteractions(api);

        TestHelper.simulateOnline();

        when(api.pushToRemote(any(Cursor.class))).thenReturn(new String[]{"1"});
        expect(tracker.flushPlaybackTrackingEvents()).toBeTrue();
    }
}
