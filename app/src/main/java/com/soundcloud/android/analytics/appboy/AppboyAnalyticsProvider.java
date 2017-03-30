package com.soundcloud.android.analytics.appboy;

import com.soundcloud.android.R;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.analytics.DefaultAnalyticsProvider;
import com.soundcloud.android.events.AttributionEvent;
import com.soundcloud.android.events.CurrentUserChangedEvent;
import com.soundcloud.android.events.TrackingEvent;
import com.soundcloud.android.events.OfflineInteractionEvent;
import com.soundcloud.android.events.PlaybackSessionEvent;
import com.soundcloud.android.events.ScreenEvent;
import com.soundcloud.android.events.SearchEvent;
import com.soundcloud.android.events.UIEvent;
import com.soundcloud.android.model.Urn;

import android.content.Context;

import javax.inject.Inject;

public class AppboyAnalyticsProvider extends DefaultAnalyticsProvider {

    private final AppboyWrapper appboy;
    private final AppboyEventHandler eventHandler;

    @Inject
    AppboyAnalyticsProvider(AppboyWrapper appboy,
                            AppboyEventHandler eventHandler,
                            AccountOperations accountOperations) {
        this.appboy = appboy;
        this.eventHandler = eventHandler;
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
            changeUser(event.getCurrentUserUrn());
        }
    }

    @Override
    public void onAppCreated(Context context) {
        appboy.setAppboyEndpointProvider(context.getString(R.string.com_appboy_server));
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
        } else if (event instanceof OfflineInteractionEvent) {
            eventHandler.handleEvent((OfflineInteractionEvent) event);
        }
    }

    private void changeUser(Urn userUrn) {
        if (userUrn.isUser() && !userUrn.equals(AccountOperations.ANONYMOUS_USER_URN)) {
            appboy.changeUser(userUrn.toString());
        }
    }
}
