package com.soundcloud.android.analytics.localytics;

import com.localytics.android.LocalyticsSession;
import com.soundcloud.android.events.OnboardingEvent;

import java.util.Map;

public class LocalyticsOnboardingEventHandler {
    private LocalyticsSession localyticsSession;

    LocalyticsOnboardingEventHandler(LocalyticsSession localyticsSession) {
        this.localyticsSession = localyticsSession;
    }

    public void handleEvent(OnboardingEvent sourceEvent) {
        handleEvent(sourceEvent.getKind(), sourceEvent.getAttributes());
    }

    private void handleEvent(int sourceEventType, Map<String, String> eventAttributes) {
        switch (sourceEventType) {
            case OnboardingEvent.AUTH_PROMPT:
                localyticsSession.tagEvent(LocalyticsEvents.Onboarding.AUTH_PROMPT, eventAttributes);
                break;
            case OnboardingEvent.AUTH_CREDENTIALS:
                localyticsSession.tagEvent(LocalyticsEvents.Onboarding.AUTH_CREDENTIALS, eventAttributes);
                break;
            case OnboardingEvent.CONFIRM_TERMS:
                localyticsSession.tagEvent(LocalyticsEvents.Onboarding.CONFIRM_TERMS, eventAttributes);
                break;
            case OnboardingEvent.AUTH_COMPLETE:
                localyticsSession.tagEvent(LocalyticsEvents.Onboarding.AUTH_COMPLETE, eventAttributes);
                break;
            case OnboardingEvent.USER_INFO:
                localyticsSession.tagEvent(LocalyticsEvents.Onboarding.USER_INFO, eventAttributes);
                break;
            case OnboardingEvent.ONBOARDING_COMPLETE:
                localyticsSession.tagEvent(LocalyticsEvents.Onboarding.ONBOARDING_COMPLETE, eventAttributes);
                break;
            default:
                throw new IllegalArgumentException("Onboarding Event type is invalid");
        }
    }
}
