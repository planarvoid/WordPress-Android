package com.soundcloud.android.profile;


import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.analytics.EventTracker;
import com.soundcloud.android.analytics.ReferringEventProvider;
import com.soundcloud.android.events.ScreenEvent;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.rx.observers.LambdaSubscriber;
import rx.Observable;
import rx.Subscription;

import javax.inject.Inject;

class FollowersPresenter {

    interface FollowersView {
        Urn getUserUrn();

        Observable<Void> enterScreen();

        void visitFollowersScreenForCurrentUser(Screen trackingScreen);

        void visitFollowersScreenForOtherUser(Screen trackingScreen);
    }

    private final EventTracker eventTracker;
    private final ReferringEventProvider referringEventProvider;
    private final AccountOperations accountOperations;
    private Subscription subscription;

    @Inject
    FollowersPresenter(EventTracker eventTracker, ReferringEventProvider referringEventProvider, AccountOperations accountOperations) {
        this.eventTracker = eventTracker;
        this.referringEventProvider = referringEventProvider;
        this.accountOperations = accountOperations;
    }

    void attachView(FollowersView followersView) {
        final Urn userUrn = followersView.getUserUrn();
        subscription = followersView.enterScreen()
                                    .subscribe(LambdaSubscriber.onNext(event -> eventTracker.trackScreen(ScreenEvent.create(getTrackingScreen(userUrn), userUrn),
                                                                                                         referringEventProvider.getReferringEvent())));
    }

    void detachView() {
        subscription.unsubscribe();
    }

    void visitFollowersScreen(FollowersView followersView) {
        final Urn userUrn = followersView.getUserUrn();
        final Screen trackingScreen = getTrackingScreen(userUrn);

        if (isLoggedInUser(userUrn)) {
            followersView.visitFollowersScreenForCurrentUser(trackingScreen);
        } else {
            followersView.visitFollowersScreenForOtherUser(trackingScreen);
        }
    }

    private boolean isLoggedInUser(Urn userUrn) {
        return accountOperations.isLoggedInUser(userUrn);
    }

    private Screen getTrackingScreen(Urn userUrn) {
        return isLoggedInUser(userUrn) ? Screen.YOUR_FOLLOWERS : Screen.USER_FOLLOWERS;
    }

}
