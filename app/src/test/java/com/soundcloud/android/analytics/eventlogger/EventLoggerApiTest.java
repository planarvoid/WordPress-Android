package com.soundcloud.android.analytics.eventlogger;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.matchers.SoundCloudMatchers.urlEqualTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.utils.DeviceHelper;
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

    private final String goodUrl = "http://some_url.com";
    private final String badUrl = "http://some_bad_url.com";

    private Pair<Long, String> event1 = new Pair(1L, goodUrl);
    private Pair<Long, String> event2 = new Pair(2L, badUrl);

    private EventLoggerApi eventLoggerApi;

    @Mock
    private HttpURLConnection connection;
    @Mock
    private HttpURLConnection badConnection;
    @Mock
    private HttpURLConnectionFactory httpURLConnectionFactory;
    @Mock
    private DeviceHelper deviceHelper;

    @Before
    public void setUp() throws Exception {
        when(deviceHelper.getUserAgent()).thenReturn("SoundCloud-Android/1.2.3 (Android 4.1.1; Samsung GT-I9082)");
        when(deviceHelper.getUniqueDeviceID()).thenReturn("9876");

        eventLoggerApi = new EventLoggerApi("1", deviceHelper, httpURLConnectionFactory);
        when(connection.getResponseCode()).thenReturn(HttpStatus.SC_OK);
        when(badConnection.getResponseCode()).thenReturn(HttpStatus.SC_FORBIDDEN);

    }

    @Test
    public void providesPlaybackEventUrl() throws UnsupportedEncodingException {

        MatrixCursor trackingData = getEventLoggerStorageCursor();

        trackingData.addRow(new Object[]{
                1L,
                1000L,
                EventLoggerEventTypes.PLAYBACK.getPath(),
                "key1=1&key2=2"
        });
        trackingData.moveToNext();
        assertThat(eventLoggerApi.buildUrl(trackingData), is(urlEqualTo("http://eventlogger.soundcloud.com/audio?client_id=1&anonymous_id=9876&key1=1&key2=2")));

    }

    @Test
    public void providesPlaybackPerformanceUrl() throws UnsupportedEncodingException {

        MatrixCursor trackingData = getEventLoggerStorageCursor();

        trackingData.addRow(new Object[]{
                3L,
                3000L,
                EventLoggerEventTypes.PLAYBACK_PERFORMANCE.getPath(),
                "key5=5&key6=6"
        });

        trackingData.moveToNext();
        assertThat(eventLoggerApi.buildUrl(trackingData), is(urlEqualTo("http://eventlogger.soundcloud.com/audio_performance?key5=5&key6=6&client_id=1&anonymous_id=9876")));
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
        when(deviceHelper.getUserAgent()).thenReturn("id");

        eventLoggerApi.pushToRemote(Lists.newArrayList(event1));
        verify(connection).setRequestMethod("HEAD");
        verify(connection).setRequestProperty("User-Agent", "SoundCloud-Android/1.2.3 (Android 4.1.1; Samsung GT-I9082)");
        verify(connection).setConnectTimeout(EventLoggerApi.CONNECT_TIMEOUT);
        verify(connection).setReadTimeout(EventLoggerApi.READ_TIMEOUT);
    }

    @Test
    public void shouldDisconectConnection() throws Exception {
        when(httpURLConnectionFactory.create(goodUrl)).thenReturn(connection);
        eventLoggerApi.pushToRemote(Lists.newArrayList(event1));
        verify(connection).disconnect();
    }

    private MatrixCursor getEventLoggerStorageCursor() {
        return new MatrixCursor(new String[]{
                EventLoggerDbHelper.TrackingEvents._ID,
                EventLoggerDbHelper.TrackingEvents.TIMESTAMP,
                EventLoggerDbHelper.TrackingEvents.PATH,
                EventLoggerDbHelper.TrackingEvents.PARAMS
        });
    }
}
