package com.soundcloud.android.analytics.localytics;

import com.localytics.android.LocalyticsSession;
import com.soundcloud.android.events.OnboardingEvent;

import java.util.Map;

public class LocalyticsOnboardingEventHandler {
    private LocalyticsSession mLocalyticsSession;

    LocalyticsOnboardingEventHandler(LocalyticsSession localyticsSession) {
        mLocalyticsSession = localyticsSession;
    }

    public void handleEvent(OnboardingEvent sourceEvent) {
        handleEvent(sourceEvent.getKind(), sourceEvent.getAttributes());
    }

    private void handleEvent(int sourceEventType, Map<String, String> eventAttributes) {
        switch (sourceEventType) {
            case OnboardingEvent.AUTH_PROMPT:
                mLocalyticsSession.tagEvent(LocalyticsEvents.Onboarding.AUTH_PROMPT, eventAttributes);
                break;
            case OnboardingEvent.AUTH_CREDENTIALS:
                mLocalyticsSession.tagEvent(LocalyticsEvents.Onboarding.AUTH_CREDENTIALS, eventAttributes);
                break;
            case OnboardingEvent.CONFIRM_TERMS:
                mLocalyticsSession.tagEvent(LocalyticsEvents.Onboarding.CONFIRM_TERMS, eventAttributes);
                break;
            case OnboardingEvent.AUTH_COMPLETE:
                mLocalyticsSession.tagEvent(LocalyticsEvents.Onboarding.AUTH_COMPLETE, eventAttributes);
                break;
            case OnboardingEvent.SAVE_USER_INFO:
                mLocalyticsSession.tagEvent(LocalyticsEvents.Onboarding.SAVE_USER_INFO, eventAttributes);
                break;
            case OnboardingEvent.SKIP_USER_INFO:
                mLocalyticsSession.tagEvent(LocalyticsEvents.Onboarding.SKIP_USER_INFO, eventAttributes);
                break;
            case OnboardingEvent.ONBOARDING_COMPLETE:
                mLocalyticsSession.tagEvent(LocalyticsEvents.Onboarding.ONBOARDING_COMPLETE, eventAttributes);
                break;
            default:
                throw new IllegalArgumentException("Onboarding Event type is invalid");
        }
    }
}
