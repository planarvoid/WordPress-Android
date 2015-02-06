package com.soundcloud.android.analytics.localytics;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyMapOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.localytics.android.LocalyticsAmpSession;
import com.localytics.android.LocalyticsSession;
import com.soundcloud.android.api.legacy.model.PublicApiUser;
import com.soundcloud.android.events.AudioAdFailedToBufferEvent;
import com.soundcloud.android.events.BufferUnderrunEvent;
import com.soundcloud.android.events.ConnectionType;
import com.soundcloud.android.events.CurrentUserChangedEvent;
import com.soundcloud.android.events.PlayControlEvent;
import com.soundcloud.android.events.PlaybackPerformanceEvent;
import com.soundcloud.android.events.PlayerType;
import com.soundcloud.android.events.ScreenEvent;
import com.soundcloud.android.events.SkippyInitilizationFailedEvent;
import com.soundcloud.android.events.SkippyInitilizationSucceededEvent;
import com.soundcloud.android.events.SkippyPlayEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlaybackProgress;
import com.soundcloud.android.playback.PlaybackProtocol;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.tobedevoured.modelcitizen.CreateModelException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

@RunWith(SoundCloudTestRunner.class)
public class LocalyticsAnalyticsProviderTest {

    private LocalyticsAnalyticsProvider localyticsProvider;

    @Mock private LocalyticsAmpSession localyticsSession;
    @Mock private ProxyDetector proxyDetector;

    @Before
    public void setUp() throws CreateModelException {
        localyticsProvider = new LocalyticsAnalyticsProvider(localyticsSession, null, 123L, proxyDetector);
    }

    @Test
    public void setsCustomSessionTimeout() {
        expect(LocalyticsSession.getSessionExpiration()).toEqual(60000L);
    }

    @Test
    public void shouldUploadDataWhenFlushing(){
        localyticsProvider.flush();
        verify(localyticsSession).upload();
    }

    @Test
    public void shouldSetCustomerIdWhenConstructed() throws Exception {
        verify(localyticsSession).setCustomerId("123");
    }

    @Test
    public void shouldSetCustomerIdToNullIfTheUserIsNotLoggedInWhenConstructed() throws Exception {
        localyticsProvider = new LocalyticsAnalyticsProvider(localyticsSession, null, -1, proxyDetector);
        verify(localyticsSession).setCustomerId(null);
    }

    @Test
    public void shouldSetCustomerIdToUserIdWhenUserIsUpdated() {
        PublicApiUser user = new PublicApiUser(456L);
        CurrentUserChangedEvent userEvent = CurrentUserChangedEvent.forUserUpdated(user);
        localyticsProvider.handleCurrentUserChangedEvent(userEvent);
        verify(localyticsSession).setCustomerId(Long.toString(456L));
    }

    @Test
    public void shouldSetCustomerIdToNullWhenUserIsRemoved() {
        CurrentUserChangedEvent userEvent = CurrentUserChangedEvent.forLogout();
        localyticsProvider.handleCurrentUserChangedEvent(userEvent);
        verify(localyticsSession).setCustomerId(null);
    }

    @Test
    public void shouldTrackScreenWithGivenName() {
        localyticsProvider.handleTrackingEvent(ScreenEvent.create("main:explore"));
        verify(localyticsSession).tagScreen(eq("main:explore"));
    }

    @Test
    public void shouldTrackAudioAdFailedToBufferEvent() {
        final AudioAdFailedToBufferEvent event = new AudioAdFailedToBufferEvent(Urn.forTrack(123L), new PlaybackProgress(123, 1234), 6);
        localyticsProvider.handleTrackingEvent(event);
        verify(localyticsSession).tagEvent(eq("Ad failed to buffer"), eq(event.getAttributes()));
    }

    @Test
    public void shouldSendPageviewEventWithScreenTracking() {
        Map<String, String> attributes = new HashMap<>();
        attributes.put("context", "main:explore");

        localyticsProvider.handleTrackingEvent(ScreenEvent.create("main:explore"));

        verify(localyticsSession).tagEvent(eq(LocalyticsEvents.PAGEVIEW), eq(attributes));
    }

    @Test
    public void shouldTrackPlayControlEvent() {
        PlayControlEvent event = PlayControlEvent.play(PlayControlEvent.SOURCE_FULL_PLAYER);
        localyticsProvider.handleTrackingEvent(event);

        verify(localyticsSession).tagEvent(eq(LocalyticsEvents.PLAY_CONTROLS), eq(event.getAttributes()));
    }

    @Test
    public void shouldTrackBufferUnderrunEvent() {
        BufferUnderrunEvent event = new BufferUnderrunEvent(ConnectionType.FOUR_G, "player", true);
        localyticsProvider.handleTrackingEvent(event);

        verify(localyticsSession).tagEvent(eq("Buffer Underrun"), eq(event.getAttributes()));
    }

    @Test
    public void shouldTrackSkippyInitilizationError() {
        SkippyInitilizationFailedEvent event = new SkippyInitilizationFailedEvent(new IOException(), "error message", 3, 4);
        localyticsProvider.handleTrackingEvent(event);

        verify(localyticsSession).tagEvent(eq("Skippy Init Error"), eq(event.getAttributes()));
    }

    @Test
    public void shouldTrackSkippyInitilizationSuccess() {
        SkippyInitilizationSucceededEvent event = new SkippyInitilizationSucceededEvent(3, 4);
        localyticsProvider.handleTrackingEvent(event);

        verify(localyticsSession).tagEvent(eq("Skippy Init Success"), eq(event.getAttributes()));
    }

    @Test
    public void shouldTrackSkippyPlayEvent() {
        SkippyPlayEvent event = new SkippyPlayEvent(ConnectionType.FOUR_G, true);
        localyticsProvider.handleTrackingEvent(event);

        verify(localyticsSession).tagEvent(eq("Skippy Play"), eq(event.getAttributes()));
    }

    @Test
    public void shouldTrackProxyUseOnPlaybackPerformanceEvents() {
        PlaybackPerformanceEvent event = PlaybackPerformanceEvent.timeToPlay(
                0, PlaybackProtocol.HTTPS, PlayerType.MEDIA_PLAYER, ConnectionType.UNKNOWN, "http://localhost", Urn.forUser(100L));
        when(proxyDetector.isProxyConfiguredFor(any(URI.class))).thenReturn(true);

        localyticsProvider.handlePlaybackPerformanceEvent(event);

        Map<String, String> tagAttributes = new HashMap<>();
        tagAttributes.put("proxy_configured", "yes");
        verify(localyticsSession).tagEvent(eq("Stream proxy configured"), eq(tagAttributes));
    }

    @Test
    public void shouldNotTrackOtherPerformanceEvents() {
        PlaybackPerformanceEvent event = PlaybackPerformanceEvent.timeToBuffer(
                0, PlaybackProtocol.HTTPS, PlayerType.MEDIA_PLAYER, ConnectionType.UNKNOWN, "http://localhost", Urn.forUser(100L));
        when(proxyDetector.isProxyConfiguredFor(any(URI.class))).thenReturn(false);

        localyticsProvider.handlePlaybackPerformanceEvent(event);

        verify(localyticsSession, never()).tagEvent(anyString(), anyMapOf(String.class, String.class));
    }

    @Test
    public void shouldNotTrackPlaybackPerformanceWithNullHost() {
        PlaybackPerformanceEvent event = PlaybackPerformanceEvent.timeToPlay(
                0, PlaybackProtocol.HTTPS, PlayerType.MEDIA_PLAYER, ConnectionType.UNKNOWN, null, Urn.forUser(100L));

        localyticsProvider.handlePlaybackPerformanceEvent(event);

        verify(localyticsSession, never()).tagEvent(anyString(), anyMapOf(String.class, String.class));
    }

}
