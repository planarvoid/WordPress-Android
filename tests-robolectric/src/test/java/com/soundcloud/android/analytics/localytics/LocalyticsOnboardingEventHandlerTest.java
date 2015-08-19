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

    private LocalyticsOnboardingEventHandler handler;

    @Mock
    private LocalyticsSession localyticsSession;

    @Before
    public void setUp() {
        handler = new LocalyticsOnboardingEventHandler(localyticsSession);
    }

    @Test
    public void shouldHandleLoginPromptEvent() {
        OnboardingEvent event = OnboardingEvent.logInPrompt();
        handler.handleEvent(event);
        verify(localyticsSession).tagEvent("Auth prompt", event.getAttributes());
    }

    @Test
    public void shouldHandleAuthCredentialsEvent() {
        OnboardingEvent event = OnboardingEvent.nativeAuthEvent();
        handler.handleEvent(event);
        verify(localyticsSession).tagEvent("Auth credentials", event.getAttributes());
    }

    @Test
    public void shouldHandleAuthCompleteEvent() {
        OnboardingEvent event = OnboardingEvent.authComplete();
        handler.handleEvent(event);
        verify(localyticsSession).tagEvent("Auth complete", event.getAttributes());
    }

    @Test
    public void shouldHandleOnboardingCompleteEvent() {
        OnboardingEvent event = OnboardingEvent.onboardingComplete();
        handler.handleEvent(event);
        verify(localyticsSession).tagEvent("Onboarding complete", event.getAttributes());
    }

    @Test
    public void shouldHandleSignupErrorEvent() {
        OnboardingEvent event = OnboardingEvent.signupServeCaptcha();
        handler.handleEvent(event);
        verify(localyticsSession).tagEvent("Signup Error", event.getAttributes());
    }

}
