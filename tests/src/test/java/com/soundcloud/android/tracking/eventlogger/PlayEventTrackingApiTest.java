package com.soundcloud.android.tracking.eventlogger;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.robolectric.DefaultTestRunner;
import com.soundcloud.android.utils.ScTextUtils;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.database.MatrixCursor;

import java.io.UnsupportedEncodingException;

@RunWith(DefaultTestRunner.class)
public class PlayEventTrackingApiTest {

    @Test
    public void shouldCreateCorrectTrackingUrls() throws UnsupportedEncodingException {
        PlayEventTrackingApi api = new PlayEventTrackingApi("1");

        MatrixCursor trackingData = new MatrixCursor(new String[]{
                PlayEventTracker.TrackingEvents._ID,
                PlayEventTracker.TrackingEvents.ACTION,
                PlayEventTracker.TrackingEvents.TIMESTAMP,
                PlayEventTracker.TrackingEvents.SOUND_URN,
                PlayEventTracker.TrackingEvents.USER_URN,
                PlayEventTracker.TrackingEvents.SOUND_DURATION,
                PlayEventTracker.TrackingEvents.SOURCE_INFO
        });

        trackingData.addRow(new Object[]{
            1L,
            "play",
            1000L,
            "soundcloud:sounds:1",
            "soundcloud:users:1",
            500L,
            "context=stream&exploreVersion=123"
        });
        trackingData.addRow(new Object[]{
            1L,
            "stop",
            2000L,
            "soundcloud:sounds:1",
            "soundcloud:users:1",
            500L,
            "context=stream&exploreVersion=123"
        });
        trackingData.addRow(new Object[]{
            2L,
            "play",
            3000L,
            "soundcloud:sounds:2",
            "soundcloud:users:2",
            100L,
            ScTextUtils.EMPTY_STRING
        });

        trackingData.moveToNext();
        expect(api.buildUrl(trackingData)).toEqual("http://eventlogger.soundcloud.com/audio?client_id=1&ts=1000&action=play&user=soundcloud%3Ausers%3A1&sound=soundcloud%3Asounds%3A1&duration=500&context=stream&exploreVersion=123");
        trackingData.moveToNext();
        expect(api.buildUrl(trackingData)).toEqual("http://eventlogger.soundcloud.com/audio?client_id=1&ts=2000&action=stop&user=soundcloud%3Ausers%3A1&sound=soundcloud%3Asounds%3A1&duration=500&context=stream&exploreVersion=123");
        trackingData.moveToNext();
        expect(api.buildUrl(trackingData)).toEqual("http://eventlogger.soundcloud.com/audio?client_id=1&ts=3000&action=play&user=soundcloud%3Ausers%3A2&sound=soundcloud%3Asounds%3A2&duration=100");
    }


}
