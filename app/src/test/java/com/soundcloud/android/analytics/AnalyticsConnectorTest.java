package com.soundcloud.android.analytics;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.analytics.appboy.AppboyWrapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import android.support.v7.app.AppCompatActivity;

@RunWith(MockitoJUnitRunner.class)
public class AnalyticsConnectorTest {

    @Mock private AppboyWrapper appboyWrapper;
    @Mock private AppCompatActivity activity;

    private AnalyticsConnector analyticsConnector;

    @Before
    public void setUp() throws Exception {
        analyticsConnector = new AnalyticsConnector(appboyWrapper);
    }

    @Test
    public void shouldOpenAppBoySessionAndRequestInAppMessagesInOnStart() {
        when(appboyWrapper.openSession(activity)).thenReturn(true);

        analyticsConnector.onStart(activity);

        verify(appboyWrapper).openSession(activity);
        verify(appboyWrapper).requestInAppMessageRefresh();
    }

    @Test
    public void shouldNotRequestInAppMessagesInOnStartWhenOpeningSessionFailed() {
        when(appboyWrapper.openSession(activity)).thenReturn(false);

        analyticsConnector.onStart(activity);

        verify(appboyWrapper, never()).requestInAppMessageRefresh();
    }

    @Test
    public void shouldRegisterInAppMessageManagerInOnResume() {
        analyticsConnector.onResume(activity);

        verify(appboyWrapper).registerInAppMessageManager(activity);
    }

    @Test
    public void shouldUnregisterInAppMessagesInOnPause() {
        analyticsConnector.onPause(activity);

        verify(appboyWrapper).unregisterInAppMessageManager(activity);
    }

    @Test
    public void shouldCloseAppBoySessionInOnStop() {
        analyticsConnector.onStop(activity);

        verify(appboyWrapper).closeSession(activity);
    }
}
