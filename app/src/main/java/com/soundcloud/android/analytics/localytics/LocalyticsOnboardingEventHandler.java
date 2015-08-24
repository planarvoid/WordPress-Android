package com.soundcloud.android.analytics.localytics;

import static com.soundcloud.android.analytics.localytics.LocalyticsEvents.Onboarding;

import com.localytics.android.LocalyticsSession;
import com.soundcloud.android.events.OnboardingEvent;
import com.soundcloud.android.utils.Log;
import com.soundcloud.java.objects.MoreObjects;

import java.util.Map;

public class LocalyticsOnboardingEventHandler {
    private static final String TAG = "LocalyticsOnboardingEH";
    private final LocalyticsSession localyticsSession;

    LocalyticsOnboardingEventHandler(LocalyticsSession localyticsSession) {
        this.localyticsSession = localyticsSession;
    }

    public void handleEvent(OnboardingEvent sourceEvent) {
        handleEvent(sourceEvent.getKind(), sourceEvent.getAttributes());
    }

    private void handleEvent(int sourceEventType, Map<String, String> attributes) {
        switch (sourceEventType) {
            case OnboardingEvent.AUTH_PROMPT:
                sendEvent(Onboarding.AUTH_PROMPT, attributes);
                break;
            case OnboardingEvent.AUTH_CREDENTIALS:
                sendEvent(Onboarding.AUTH_CREDENTIALS, attributes);
                break;
            case OnboardingEvent.AUTH_COMPLETE:
                sendEvent(Onboarding.AUTH_COMPLETE, attributes);
                break;
            case OnboardingEvent.ONBOARDING_COMPLETE:
                sendEvent(Onboarding.ONBOARDING_COMPLETE, attributes);
                break;
            case OnboardingEvent.SIGNUP_ERROR:
                sendEvent(Onboarding.SIGNUP_ERROR, attributes);
                break;
            default:
                throw new IllegalArgumentException("Onboarding Event type is invalid");
        }
    }

    private void sendEvent(String event, Map<String, String> attributes) {
        logAttributes(event, attributes);
        localyticsSession.tagEvent(event, attributes);
    }

    private void logAttributes(String tagName, Map<String, String> eventAttributes) {
        if (android.util.Log.isLoggable(TAG, android.util.Log.DEBUG)) {
            final MoreObjects.ToStringHelper toStringHelper = MoreObjects.toStringHelper(tagName + " with EventAttributes");
            for (String key : eventAttributes.keySet()) {
                toStringHelper.add(key, eventAttributes.get(key));
            }
            Log.d(TAG, toStringHelper.toString());
        }
    }
}
