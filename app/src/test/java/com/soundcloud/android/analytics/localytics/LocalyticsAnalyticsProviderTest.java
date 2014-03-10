package com.soundcloud.android.analytics.localytics;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;

import com.localytics.android.LocalyticsSession;
import com.soundcloud.android.events.CurrentUserChangedEvent;
import com.soundcloud.android.model.User;
import com.soundcloud.android.playback.service.PlaybackStateProvider;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.tobedevoured.modelcitizen.CreateModelException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

@RunWith(SoundCloudTestRunner.class)
public class LocalyticsAnalyticsProviderTest {

    private LocalyticsAnalyticsProvider localyticsProvider;

    @Mock
    private LocalyticsSession localyticsSession;


    @Before
    public void setUp() throws CreateModelException {
        localyticsProvider = new LocalyticsAnalyticsProvider(localyticsSession, null);
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
        User user = new User(123L);
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
        localyticsProvider.handleScreenEvent("main:explore");
        verify(localyticsSession).tagScreen(eq("main:explore"));
    }


}
