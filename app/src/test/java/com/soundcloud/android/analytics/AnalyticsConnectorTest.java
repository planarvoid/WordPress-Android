package com.soundcloud.android.analytics;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.analytics.appboy.AppboyPlaySessionState;
import com.soundcloud.android.analytics.appboy.AppboyWrapper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

@RunWith(MockitoJUnitRunner.class)
public class AnalyticsConnectorTest {

    @Mock private AppboyWrapper appboyWrapper;
    @Mock private AppCompatActivity activity;
    @Mock private AppboyPlaySessionState appboyPlaySessionState;
    @Mock private AccountOperations accountOperations;

    private AnalyticsConnector analyticsConnector;

    @Before
    public void setUp() throws Exception {
        analyticsConnector = new AnalyticsConnector(appboyWrapper, appboyPlaySessionState, accountOperations);
    }

    @Test
    public void shouldOpenAppBoySessionOnStart() {
        analyticsConnector.onStart(activity);

        verify(appboyWrapper).openSession(activity);
    }

    @Test
    public void shouldRequestInAppMessageRefreshOnStart() {
        when(appboyWrapper.openSession(activity)).thenReturn(true);

        analyticsConnector.onStart(activity);

        verify(appboyWrapper).requestInAppMessageRefresh();
    }

    @Test
    public void shouldFlagSessionAsNotPlayedOnStart() {
        when(appboyWrapper.openSession(activity)).thenReturn(true);

        analyticsConnector.onStart(activity);

        verify(appboyPlaySessionState).resetSessionPlayed();
    }

    @Test
    public void shouldNotRequestInAppMessagesInOnStartWhenOpeningSessionIsNotNew() {
        when(appboyWrapper.openSession(activity)).thenReturn(false);

        analyticsConnector.onStart(activity);

        verify(appboyWrapper, never()).requestInAppMessageRefresh();
    }

    @Test
    public void shouldNotResetPlaySessionStateOnStartWhenOpeningSessionIsNotNew() {
        when(appboyWrapper.openSession(activity)).thenReturn(false);

        analyticsConnector.onStart(activity);

        verify(appboyPlaySessionState, never()).resetSessionPlayed();
    }

    @Test
    public void shouldEndureListeningToInAppMessagedInOnCreate() {
        analyticsConnector.onCreate(activity, new Bundle());

        verify(appboyWrapper).ensureSubscribedToInAppMessageEvents(activity);
    }

    @Test
    public void shouldRegisterInAppMessageManagerOnResume() {
        analyticsConnector.onResume(activity);

        verify(appboyWrapper).registerInAppMessageManager(activity);
    }

    @Test
    public void shouldUnregisterInAppMessagesOnPause() {
        analyticsConnector.onPause(activity);

        verify(appboyWrapper).unregisterInAppMessageManager(activity);
    }

    @Test
    public void shouldCloseAppBoySessionOnStop() {
        analyticsConnector.onStop(activity);

        verify(appboyWrapper).closeSession(activity);
    }

    @Test
    public void shouldNotRegisterForInAppMessagesForCrawlerUsers() {
        when(accountOperations.isCrawler()).thenReturn(true);

        analyticsConnector.onCreate(activity, new Bundle());
        analyticsConnector.onResume(activity);

        verify(appboyWrapper, never()).ensureSubscribedToInAppMessageEvents(activity);
        verify(appboyWrapper, never()).registerInAppMessageManager(activity);
    }

}
