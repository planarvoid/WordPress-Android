package com.soundcloud.android.analytics.appboy;

import com.soundcloud.android.R;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.analytics.AnalyticsProvider;
import com.soundcloud.android.events.ActivityLifeCycleEvent;
import com.soundcloud.android.events.AttributionEvent;
import com.soundcloud.android.events.CurrentUserChangedEvent;
import com.soundcloud.android.events.OnboardingEvent;
import com.soundcloud.android.events.PlaybackErrorEvent;
import com.soundcloud.android.events.PlaybackPerformanceEvent;
import com.soundcloud.android.events.PlaybackSessionEvent;
import com.soundcloud.android.events.ScreenEvent;
import com.soundcloud.android.events.SearchEvent;
import com.soundcloud.android.events.TrackingEvent;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.users.UserProperty;
import com.soundcloud.java.collections.PropertySet;

import android.content.Context;

import javax.inject.Inject;

public class AppboyAnalyticsProvider implements AnalyticsProvider {

    private final AppboyWrapper appboy;
    private final AppboyEventHandler eventHandler;

    @Inject
    public AppboyAnalyticsProvider(AppboyWrapper appboy,
                                   AccountOperations accountOperations,
                                   AppboyPlaySessionState appboyPlaySessionState) {
        this.appboy = appboy;
        this.eventHandler = new AppboyEventHandler(appboy, appboyPlaySessionState);
        changeUser(accountOperations.getLoggedInUserUrn());
    }

    @Override
    public void flush() {
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
    public void onAppCreated(Context context) {
        appboy.setAppboyEndpointProvider(context.getString(R.string.com_appboy_server));
    }

    @Override
    public void handleActivityLifeCycleEvent(ActivityLifeCycleEvent event) {
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
            eventHandler.handleEvent((UIEvent) event);
        } else if (event instanceof PlaybackSessionEvent) {
            eventHandler.handleEvent((PlaybackSessionEvent) event);
        } else if (event instanceof ScreenEvent) {
            eventHandler.handleEvent((ScreenEvent) event);
        } else if (event instanceof SearchEvent) {
            eventHandler.handleEvent((SearchEvent) event);
        } else if (event instanceof AttributionEvent) {
            eventHandler.handleEvent((AttributionEvent) event);
        }
    }

    private void changeUser(Urn userUrn) {
        if (userUrn.getNumericId() > 0) {
            appboy.changeUser(userUrn.toString());
        }
    }
}
