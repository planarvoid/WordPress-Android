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

class FollowingsPresenter {
    interface FollowingsView {
        Urn getUserUrn();

        Observable<Void> enterScreen();

        void visitFollowingsScreenForCurrentUser(Screen trackingScreen);

        void visitFollowingsScreenForOtherUser(Screen trackingScreen);
    }

    private final EventTracker eventTracker;
    private final ReferringEventProvider referringEventProvider;
    private final AccountOperations accountOperations;
    private Subscription subscription;

    @Inject
    FollowingsPresenter(EventTracker eventTracker, ReferringEventProvider referringEventProvider, AccountOperations accountOperations) {
        this.eventTracker = eventTracker;
        this.referringEventProvider = referringEventProvider;
        this.accountOperations = accountOperations;
    }

    void attachView(FollowingsPresenter.FollowingsView followingsView) {
        final Urn userUrn = followingsView.getUserUrn();
        subscription = followingsView.enterScreen()
                                    .subscribe(LambdaSubscriber.onNext(event -> eventTracker.trackScreen(ScreenEvent.create(getTrackingScreen(userUrn), userUrn),
                                                                                                         referringEventProvider.getReferringEvent())));
    }

    void detachView() {
        subscription.unsubscribe();
    }

    void visitFollowingsScreen(FollowingsPresenter.FollowingsView followingsView) {
        final Urn userUrn = followingsView.getUserUrn();
        final Screen trackingScreen = getTrackingScreen(userUrn);

        if (isLoggedInUser(userUrn)) {
            followingsView.visitFollowingsScreenForCurrentUser(trackingScreen);
        } else {
            followingsView.visitFollowingsScreenForOtherUser(trackingScreen);
        }
    }

    private boolean isLoggedInUser(Urn userUrn) {
        return accountOperations.isLoggedInUser(userUrn);
    }

    private Screen getTrackingScreen(Urn userUrn) {
        return isLoggedInUser(userUrn) ? Screen.YOUR_FOLLOWINGS : Screen.USER_FOLLOWINGS;
    }
}
