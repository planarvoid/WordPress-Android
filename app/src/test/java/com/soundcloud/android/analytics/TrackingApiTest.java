package com.soundcloud.android.analytics;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.anyInt;
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

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class TrackingApiTest {

    private final String eventUrl = "http://some_url.com";
    private final TrackingEvent event = new TrackingEvent(1L, 1000L, "backend", eventUrl);

    private TrackingApi trackingApi;

    @Mock private HttpURLConnection connection;
    @Mock private HttpURLConnection badConnection;
    @Mock private TrackingApiConnectionFactory connectionFactory;
    @Mock private DeviceHelper deviceHelper;

    @Before
    public void setup() {
        trackingApi = new TrackingApi(connectionFactory, deviceHelper);
    }

    @Test
    public void shouldReturnOnlySuccesses() throws Exception {
        final String badUrl = "http://some_bad_url.com";
        TrackingEvent failedEvent = new TrackingEvent(2L, 1000L, "backend", badUrl);

        when(connection.getResponseCode()).thenReturn(HttpStatus.SC_OK);
        when(badConnection.getResponseCode()).thenReturn(HttpStatus.SC_FORBIDDEN);

        when(connectionFactory.create(event)).thenReturn(connection);
        when(connectionFactory.create(failedEvent)).thenReturn(badConnection);

        List<TrackingEvent> successes = trackingApi.pushToRemote(Lists.newArrayList(event, failedEvent));
        expect(successes).toNumber(1);
        expect(successes.get(0).getId()).toEqual(1L);
    }

    @Test
    public void shouldSupport202AcceptedStatusAsSuccess() throws IOException {
        when(connection.getResponseCode()).thenReturn(HttpStatus.SC_ACCEPTED);
        when(connectionFactory.create(event)).thenReturn(connection);

        List<TrackingEvent> successes = trackingApi.pushToRemote(Lists.newArrayList(event));
        expect(successes).toNumber(1);
        expect(successes.get(0).getId()).toEqual(1L);
    }

    @Test
    public void shouldSetConnectionParams() throws Exception {
        when(connectionFactory.create(event)).thenReturn(connection);
        when(deviceHelper.getUserAgent()).thenReturn("SoundCloud-Android/1.2.3 (Android 4.1.1; Samsung GT-I9082)");

        trackingApi.pushToRemote(Lists.newArrayList(event));
        verify(connection).setRequestProperty("User-Agent", "SoundCloud-Android/1.2.3 (Android 4.1.1; Samsung GT-I9082)");
        verify(connection).setConnectTimeout(anyInt());
        verify(connection).setReadTimeout(anyInt());
    }

    @Test
    public void shouldDisconectConnection() throws Exception {
        when(connectionFactory.create(event)).thenReturn(connection);
        trackingApi.pushToRemote(Lists.newArrayList(event));
        verify(connection).disconnect();
    }
}
