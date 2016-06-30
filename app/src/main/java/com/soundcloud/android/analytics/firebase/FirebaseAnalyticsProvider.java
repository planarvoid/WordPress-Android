package com.soundcloud.android.analytics.firebase;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.analytics.DefaultAnalyticsProvider;
import com.soundcloud.android.events.CurrentUserChangedEvent;
import com.soundcloud.android.events.TrackingEvent;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.properties.ApplicationProperties;

import javax.inject.Inject;
import javax.inject.Provider;

public class FirebaseAnalyticsProvider extends DefaultAnalyticsProvider {

    private final FirebaseAnalytics firebaseAnalytics;
    private final ApplicationProperties applicationProperties;

    @Inject
    public FirebaseAnalyticsProvider(Provider<FirebaseAnalytics> firebaseAnalyticsProvider,
                                     AccountOperations accountOperations,
                                     ApplicationProperties applicationProperties) {
        this.applicationProperties = applicationProperties;
        this.firebaseAnalytics = firebaseAnalyticsProvider.get();
        changeUser(accountOperations.getLoggedInUserUrn());
    }

    @Override
    public void handleCurrentUserChangedEvent(CurrentUserChangedEvent event) {
        int eventKind = event.getKind();
        if (eventKind == CurrentUserChangedEvent.USER_UPDATED) {
            changeUser(event.getCurrentUserUrn());
        }
    }

    @Override
    public void handleTrackingEvent(TrackingEvent event) {
        firebaseAnalytics.logEvent(event.getKind(), event.getAttributesAsBundle());
    }

    private void changeUser(Urn userUrn) {
        if (applicationProperties.isAlphaBuild() && userUrn.isUser() && !userUrn.equals(AccountOperations.ANONYMOUS_USER_URN)) {
            firebaseAnalytics.setUserId(userUrn.toString());
        }
    }
}
