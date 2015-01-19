package com.soundcloud.android.analytics.localytics;


import static org.mockito.Mockito.verify;

import com.localytics.android.LocalyticsAmpSession;
import com.soundcloud.android.events.UserSessionEvent;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

@RunWith(SoundCloudTestRunner.class)
public class LocalyticsAnalyticsProviderSessionHandlingTest {

    private LocalyticsAnalyticsProvider localyticsProvider;

    @Mock private LocalyticsAmpSession localyticsSession;

    @Before
    public void setUp() throws Exception {
        localyticsProvider = new LocalyticsAnalyticsProvider(localyticsSession);
    }

    @After
    public void tearDown(){
        LocalyticsAnalyticsProvider.ACTIVITY_SESSION_OPEN.set(false);
    }

    @Test
    public void openSession(){
        localyticsProvider.handleUserSessionEvent(UserSessionEvent.OPENED);
        verify(localyticsSession).open();
    }

    @Test
    public void closeSession(){
        localyticsProvider.handleUserSessionEvent(UserSessionEvent.CLOSED);
        verify(localyticsSession).close();
    }
}
