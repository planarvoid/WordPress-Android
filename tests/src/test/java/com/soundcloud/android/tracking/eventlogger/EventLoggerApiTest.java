package com.soundcloud.android.tracking.eventlogger;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import com.soundcloud.android.api.http.HttpURLConnectionFactory;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.utils.ScTextUtils;
import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.database.MatrixCursor;
import android.util.Pair;

import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;

@RunWith(SoundCloudTestRunner.class)
public class EventLoggerApiTest {

    EventLoggerApi eventLoggerApi;
    @Mock
    HttpURLConnection connection;
    @Mock
    HttpURLConnection badConnection;
    @Mock
    HttpURLConnectionFactory httpURLConnectionFactory;

    private final String goodUrl = "http://some_url.com";
    private final String badUrl = "http://some_bad_url.com";

    Pair<Long, String> event1 = new Pair(1L, goodUrl);
    Pair<Long, String> event2 = new Pair(2L, badUrl);

    @Before
    public void setUp() throws Exception {
        eventLoggerApi = new EventLoggerApi("1", "9876", httpURLConnectionFactory);
        when(connection.getResponseCode()).thenReturn(HttpStatus.SC_OK);
        when(badConnection.getResponseCode()).thenReturn(HttpStatus.SC_FORBIDDEN);
    }

    @Test
    public void shouldCreateCorrectTrackingUrls() throws UnsupportedEncodingException {

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
        expect(eventLoggerApi.buildUrl(trackingData)).toEqual("http://eventlogger.soundcloud.com/audio?client_id=1&ts=1000&action=play&user=soundcloud%3Ausers%3A1&sound=soundcloud%3Asounds%3A1&duration=500&anonymous_id=9876&context=stream&exploreVersion=123");
        trackingData.moveToNext();
        expect(eventLoggerApi.buildUrl(trackingData)).toEqual("http://eventlogger.soundcloud.com/audio?client_id=1&ts=2000&action=stop&user=soundcloud%3Ausers%3A1&sound=soundcloud%3Asounds%3A1&duration=500&anonymous_id=9876&context=stream&exploreVersion=123");
        trackingData.moveToNext();
        expect(eventLoggerApi.buildUrl(trackingData)).toEqual("http://eventlogger.soundcloud.com/audio?client_id=1&ts=3000&action=play&user=soundcloud%3Ausers%3A2&sound=soundcloud%3Asounds%3A2&duration=100&anonymous_id=9876");
    }

    @Test
    public void shouldReturnOnlySuccesses() throws Exception {
        when(httpURLConnectionFactory.create(goodUrl)).thenReturn(connection);
        when(httpURLConnectionFactory.create(badUrl)).thenReturn(badConnection);

        String[] successes = eventLoggerApi.pushToRemote(Lists.newArrayList(event1, event2));
        expect(successes.length).toEqual(1);
        expect(successes[0]).toEqual("1");
    }

    @Test
    public void shouldSetConnectionParams() throws Exception {
        when(httpURLConnectionFactory.create(goodUrl)).thenReturn(connection);

        eventLoggerApi.pushToRemote(Lists.newArrayList(event1));
        verify(connection).setRequestMethod("HEAD");
        verify(connection).setConnectTimeout(EventLoggerApi.CONNECT_TIMEOUT);
        verify(connection).setReadTimeout(EventLoggerApi.READ_TIMEOUT);
    }

    @Test
    public void shouldDisconectConnection() throws Exception {
        when(httpURLConnectionFactory.create(goodUrl)).thenReturn(connection);
        eventLoggerApi.pushToRemote(Lists.newArrayList(event1));
        verify(connection).disconnect();
    }
}
