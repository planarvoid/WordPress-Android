package com.soundcloud.android.analytics.localytics;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;

import com.localytics.android.LocalyticsSession;
import com.soundcloud.android.api.legacy.model.PublicApiUser;
import com.soundcloud.android.events.CurrentUserChangedEvent;
import com.soundcloud.android.events.PlayControlEvent;
import com.soundcloud.android.events.ScreenEvent;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.tobedevoured.modelcitizen.CreateModelException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import java.util.HashMap;
import java.util.Map;

@RunWith(SoundCloudTestRunner.class)
public class LocalyticsAnalyticsProviderTest {

    private LocalyticsAnalyticsProvider localyticsProvider;

    @Mock private LocalyticsSession localyticsSession;

    @Before
    public void setUp() throws CreateModelException {
        localyticsProvider = new LocalyticsAnalyticsProvider(localyticsSession, null);
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
        localyticsProvider = new LocalyticsAnalyticsProvider(localyticsSession, null, 123L);
        verify(localyticsSession).setCustomerId("123");
    }

    @Test
    public void shouldSetCustomerIdToNullIfTheUserIsNotLoggedInWhenConstructed() throws Exception {
        localyticsProvider = new LocalyticsAnalyticsProvider(localyticsSession, null, -1);
        verify(localyticsSession).setCustomerId(null);
    }

    @Test
    public void shouldSetCustomerIdToUserIdWhenUserIsUpdated() {
        PublicApiUser user = new PublicApiUser(123L);
        CurrentUserChangedEvent userEvent = CurrentUserChangedEvent.forUserUpdated(user);
        localyticsProvider.handleCurrentUserChangedEvent(userEvent);
        verify(localyticsSession).setCustomerId(Long.toString(123L));
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
    public void shouldSendPageviewEventWithScreenTracking() {
        Map<String, String> attributes = new HashMap<String, String>();
        attributes.put("context", "main:explore");

        localyticsProvider.handleTrackingEvent(ScreenEvent.create("main:explore"));

        verify(localyticsSession).tagEvent(eq(LocalyticsEvents.PAGEVIEW), eq(attributes));
    }

    @Test
    public void shouldTrackPlayControlEvent() {
        PlayControlEvent event = PlayControlEvent.play(PlayControlEvent.SOURCE_FULL_PLAYER);
        localyticsProvider.handlePlayControlEvent(event);

        verify(localyticsSession).tagEvent(eq(LocalyticsEvents.PLAY_CONTROLS), eq(event.getAttributes()));
    }

}
