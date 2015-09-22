package com.soundcloud.android.analytics.appboy;

import static com.soundcloud.java.checks.Preconditions.checkArgument;

import com.appboy.Appboy;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.analytics.AnalyticsProvider;
import com.soundcloud.android.events.ActivityLifeCycleEvent;
import com.soundcloud.android.events.CurrentUserChangedEvent;
import com.soundcloud.android.events.OnboardingEvent;
import com.soundcloud.android.events.PlaybackErrorEvent;
import com.soundcloud.android.events.PlaybackPerformanceEvent;
import com.soundcloud.android.events.TrackingEvent;
import com.soundcloud.android.events.UserSessionEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.users.UserProperty;
import com.soundcloud.android.utils.Log;
import com.soundcloud.java.collections.PropertySet;

import android.app.Activity;

import javax.inject.Inject;

public class AppboyAnalyticsProvider implements AnalyticsProvider {

    public static final String TAG = "AppboyProvider";
    private final Appboy session;

    @Inject
    public AppboyAnalyticsProvider(Appboy session, AccountOperations accountOperations) {
        Log.d(TAG, "initialized");
        this.session = session;
        changeUser(accountOperations.getLoggedInUserUrn());
    }

    @Override
    public void flush() {
        Log.d(TAG, "flushed");
        session.requestImmediateDataFlush();
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
        if (event.getKind() == ActivityLifeCycleEvent.ON_START_EVENT) {
            openSession(event.getActivity());
        } else if (event.getKind() == ActivityLifeCycleEvent.ON_STOP_EVENT) {
            closeSession(event.getActivity());
        }
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
        // No-op
    }

    public void openSession(Activity activity) {
        Log.d(TAG, "openSession (" + activity.getClass().getSimpleName() + ")");
        session.openSession(activity);
    }

    public void closeSession(Activity activity) {
        Log.d(TAG, "closeSession (" + activity.getClass().getSimpleName() + ")");
        session.closeSession(activity);
    }

    private void changeUser(Urn userUrn) {
        checkArgument(userUrn.isUser());

        String urnString = userUrn.toString();

        if (!urnString.equals(session.getCurrentUser().getUserId())) {
            Log.d(TAG, "changeUser to " + urnString);
            session.changeUser(urnString);
        }
    }
}
