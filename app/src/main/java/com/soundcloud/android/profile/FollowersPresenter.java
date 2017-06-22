package com.soundcloud.android.profile;


import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.analytics.EventTracker;
import com.soundcloud.android.analytics.ReferringEventProvider;
import com.soundcloud.android.events.ScreenEvent;
import com.soundcloud.android.main.Screen;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.rx.observers.LambdaObserver;
import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;

import javax.inject.Inject;

class FollowersPresenter {

    private final EventTracker eventTracker;
    private final ReferringEventProvider referringEventProvider;
    private final AccountOperations accountOperations;
    private Disposable disposable;

    interface FollowersView {
        Urn getUserUrn();

        Observable<Long> enterScreenTimestamp();

        void visitFollowersScreenForCurrentUser(Screen trackingScreen);

        void visitFollowersScreenForOtherUser(Screen trackingScreen);
    }

    @Inject
    FollowersPresenter(EventTracker eventTracker, ReferringEventProvider referringEventProvider, AccountOperations accountOperations) {
        this.eventTracker = eventTracker;
        this.referringEventProvider = referringEventProvider;
        this.accountOperations = accountOperations;
    }

    void attachView(FollowersView followersView) {
        final Urn userUrn = followersView.getUserUrn();
        disposable = followersView.enterScreenTimestamp()
                                  .subscribeWith(LambdaObserver.onNext(event -> eventTracker.trackScreen(ScreenEvent.create(getTrackingScreen(userUrn), userUrn),
                                                                                                       referringEventProvider.getReferringEvent())));
    }

    void detachView() {
        disposable.dispose();
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
