package com.soundcloud.android.analytics.localytics;

import static org.mockito.Mockito.verify;

import com.localytics.android.LocalyticsSession;
import com.soundcloud.android.events.OnboardingEvent;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import java.io.File;

@RunWith(SoundCloudTestRunner.class)
public class LocalyticsOnboardingEventHandlerTest {

    private LocalyticsOnboardingEventHandler LocalyticsOnboardingEventHandler;

    @Mock
    private LocalyticsSession localyticsSession;

    @Before
    public void setUp() throws Exception {
        LocalyticsOnboardingEventHandler = new LocalyticsOnboardingEventHandler(localyticsSession);
    }

    @Test
    public void shouldHandleLoginPromptEvent() throws Exception {
        OnboardingEvent event = OnboardingEvent.logInPrompt();
        LocalyticsOnboardingEventHandler.handleEvent(event);
        verify(localyticsSession).tagEvent("Auth prompt", event.getAttributes());
    }

    @Test
    public void shouldHandleAuthCredentialsEvent() throws Exception {
        OnboardingEvent event = OnboardingEvent.nativeAuthEvent();
        LocalyticsOnboardingEventHandler.handleEvent(event);
        verify(localyticsSession).tagEvent("Auth credentials", event.getAttributes());
    }

    @Test
    public void shouldHandleConfirmTermsEvent() throws Exception {
        OnboardingEvent event = OnboardingEvent.termsAccepted();
        LocalyticsOnboardingEventHandler.handleEvent(event);
        verify(localyticsSession).tagEvent("Confirm terms", event.getAttributes());
    }

    @Test
    public void shouldHandleAuthCompleteEvent() throws Exception {
        OnboardingEvent event = OnboardingEvent.authComplete();
        LocalyticsOnboardingEventHandler.handleEvent(event);
        verify(localyticsSession).tagEvent("Auth complete", event.getAttributes());
    }

    @Test
    public void shouldHandleSaveUserInfoEvent() throws Exception {
        OnboardingEvent event = OnboardingEvent.savedUserInfo("Skrillex", new File("asdf"));
        LocalyticsOnboardingEventHandler.handleEvent(event);
        verify(localyticsSession).tagEvent("Save user info", event.getAttributes());
    }

    @Test
    public void shouldHandleSkipUserInfoEvent() throws Exception {
        OnboardingEvent event = OnboardingEvent.skippedUserInfo();
        LocalyticsOnboardingEventHandler.handleEvent(event);
        verify(localyticsSession).tagEvent("Skip user info", event.getAttributes());
    }

    @Test
    public void shouldHandleOnboardingCompleteEvent() throws Exception {
        OnboardingEvent event = OnboardingEvent.onboardingComplete();
        LocalyticsOnboardingEventHandler.handleEvent(event);
        verify(localyticsSession).tagEvent("Onboarding complete", event.getAttributes());
    }




}
