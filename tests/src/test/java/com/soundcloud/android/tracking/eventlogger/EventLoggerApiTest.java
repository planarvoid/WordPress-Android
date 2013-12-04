package com.soundcloud.android.tracking.eventlogger;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.utils.ScTextUtils;
import org.junit.Test;
import org.junit.runner.RunWith;

import android.database.MatrixCursor;

import java.io.UnsupportedEncodingException;

@RunWith(SoundCloudTestRunner.class)
public class EventLoggerApiTest {

    @Test
    public void shouldCreateCorrectTrackingUrls() throws UnsupportedEncodingException {
        EventLoggerApi api = new EventLoggerApi("1", "9876");

        MatrixCursor trackingData = new MatrixCursor(new String[]{
                EventLoggerDbHelper.TrackingEvents._ID,
                EventLoggerDbHelper.TrackingEvents.ACTION,
                EventLoggerDbHelper.TrackingEvents.TIMESTAMP,
                EventLoggerDbHelper.TrackingEvents.SOUND_URN,
                EventLoggerDbHelper.TrackingEvents.USER_URN,
                EventLoggerDbHelper.TrackingEvents.SOUND_DURATION,
                EventLoggerDbHelper.TrackingEvents.SOURCE_INFO
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
        expect(api.buildUrl(trackingData)).toEqual("http://eventlogger.soundcloud.com/audio?client_id=1&ts=1000&action=play&user=soundcloud%3Ausers%3A1&sound=soundcloud%3Asounds%3A1&duration=500&anonymous_id=9876&context=stream&exploreVersion=123");
        trackingData.moveToNext();
        expect(api.buildUrl(trackingData)).toEqual("http://eventlogger.soundcloud.com/audio?client_id=1&ts=2000&action=stop&user=soundcloud%3Ausers%3A1&sound=soundcloud%3Asounds%3A1&duration=500&anonymous_id=9876&context=stream&exploreVersion=123");
        trackingData.moveToNext();
        expect(api.buildUrl(trackingData)).toEqual("http://eventlogger.soundcloud.com/audio?client_id=1&ts=3000&action=play&user=soundcloud%3Ausers%3A2&sound=soundcloud%3Asounds%3A2&duration=100&anonymous_id=9876");
    }


}
