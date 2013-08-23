package com.soundcloud.android.analytics;

import com.localytics.android.LocalyticsSession;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import static org.mockito.Mockito.verify;

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
    public void shouldOpenSessionAndUpload(){
        localyticsProvider.openSession();
        verify(localyticsSession).open();
        verify(localyticsSession).upload();
    }

    @Test
    public void shouldCloseSessionAndUpload(){
        localyticsProvider.closeSession();
        verify(localyticsSession).close();
        verify(localyticsSession).upload();
    }

}
