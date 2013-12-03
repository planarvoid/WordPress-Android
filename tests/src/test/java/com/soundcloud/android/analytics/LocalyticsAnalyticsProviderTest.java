package com.soundcloud.android.analytics;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;

import com.localytics.android.LocalyticsSession;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
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
    public void setUp(){
        localyticsProvider = new LocalyticsAnalyticsProvider(localyticsSession);
    }

    @Test
    public void shouldOpenSession(){
        localyticsProvider.openSession();
        verify(localyticsSession).open();
    }

    @Test
    public void shouldUploadDataWhenClosingSession(){
        localyticsProvider.closeSession();
        verify(localyticsSession).upload();
    }

    @Test
    public void shouldCloseSession(){
        localyticsProvider.closeSession();
        verify(localyticsSession).close();
    }

    @Test
    public void shouldTrackScreenWithGivenName() {
        localyticsProvider.trackScreen("main:explore");
        verify(localyticsSession).tagScreen(eq("main:explore"));
    }
}
