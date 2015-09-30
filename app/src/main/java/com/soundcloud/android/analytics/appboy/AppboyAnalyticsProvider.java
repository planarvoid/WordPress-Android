package com.soundcloud.android.analytics.appboy;

import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.analytics.AnalyticsProvider;
import com.soundcloud.android.events.ActivityLifeCycleEvent;
import com.soundcloud.android.events.CurrentUserChangedEvent;
import com.soundcloud.android.events.OnboardingEvent;
import com.soundcloud.android.events.PlaybackErrorEvent;
import com.soundcloud.android.events.PlaybackPerformanceEvent;
import com.soundcloud.android.events.PlaybackSessionEvent;
import com.soundcloud.android.events.TrackingEvent;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.events.UserSessionEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.users.UserProperty;
import com.soundcloud.android.utils.Log;
import com.soundcloud.java.collections.PropertySet;

import android.app.Activity;

import javax.inject.Inject;

public class AppboyAnalyticsProvider implements AnalyticsProvider {

    public static final String TAG = "AppboyProvider";
    private final AppboyWrapper appboy;
    private final AppboyCustomEventHandler customEventHandler;

    @Inject
    public AppboyAnalyticsProvider(AppboyWrapper appboy, AccountOperations accountOperations) {
        Log.d(TAG, "initialized");
        this.appboy = appboy;
        customEventHandler = new AppboyCustomEventHandler(appboy);
        changeUser(accountOperations.getLoggedInUserUrn());
    }

    @Override
    public void flush() {
        Log.d(TAG, "flushed");
        appboy.requestImmediateDataFlush();
    }

    @Override
    public void handleCurrentUserChangedEvent(CurrentUserChangedEvent event) {
        int eventKind = event.getKind();

        if (eventKind == CurrentUserChangedEvent.USER_UPDATED) {
            PropertySet currentUser = event.getCurrentUser();

            if (currentUser != null) {
                changeUser(currentUser.get(UserProperty.URN));
            }
        }
    }

    @Override
    public void handleActivityLifeCycleEvent(ActivityLifeCycleEvent event) {
        switch(event.getKind()) {
            case ActivityLifeCycleEvent.ON_START_EVENT:
                openSession(event.getActivity());
                break;
            case ActivityLifeCycleEvent.ON_RESUME_EVENT:
                registerInAppMessage(event.getActivity());
                break;
            case ActivityLifeCycleEvent.ON_PAUSE_EVENT:
                unregisterInAppMessage(event.getActivity());
                break;
            case ActivityLifeCycleEvent.ON_STOP_EVENT:
                closeSession(event.getActivity());
                break;
            default:
                break;
        }
    }

    private void unregisterInAppMessage(Activity activity) {
        Log.d(TAG, "unregisterInAppMessage (" + activity.getClass().getSimpleName() + ")");
        appboy.unregisterInAppMessageManager(activity);
    }

    private void registerInAppMessage(Activity activity) {
        Log.d(TAG, "registerInAppMessage (" + activity.getClass().getSimpleName() + ")");
        appboy.registerInAppMessageManager(activity);
    }

    @Override
    public void handleUserSessionEvent(UserSessionEvent event) {
        // No-op
    }

    @Override
    public void handlePlaybackPerformanceEvent(PlaybackPerformanceEvent eventData) {
        // No-op
    }

    @Override
    public void handlePlaybackErrorEvent(PlaybackErrorEvent eventData) {
        // No-op
    }

    @Override
    public void handleOnboardingEvent(OnboardingEvent event) {
        // No-op
    }

    @Override
    public void handleTrackingEvent(TrackingEvent event) {
        if (event instanceof UIEvent) {
            customEventHandler.handleEvent((UIEvent) event);
        } else if (event instanceof PlaybackSessionEvent) {
            customEventHandler.handleEvent((PlaybackSessionEvent) event);
        }
    }

    private void openSession(Activity activity) {
        Log.d(TAG, "openSession (" + activity.getClass().getSimpleName() + ")");
        appboy.openSession(activity);
    }

    private void closeSession(Activity activity) {
        Log.d(TAG, "closeSession (" + activity.getClass().getSimpleName() + ")");
        appboy.closeSession(activity);
    }

    private void changeUser(Urn userUrn) {
        if (userUrn.getNumericId() > 0) {
            appboy.changeUser(userUrn.toString());
        }
    }
}
