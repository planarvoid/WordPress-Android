package com.soundcloud.android.facebookinvites;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.facebookapi.FacebookApi;
import com.soundcloud.android.facebookapi.FacebookApiHelper;
import com.soundcloud.android.utils.NetworkConnectionHelper;
import com.soundcloud.java.optional.Optional;
import rx.Observable;
import rx.Scheduler;
import rx.functions.Func1;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class FacebookInvitesOperations {

    public static final long CLICK_INTERVAL_MS = TimeUnit.DAYS.toMillis(14); // 14 days between clicks
    public static final long DISMISS_INTERVAL_MS = TimeUnit.DAYS.toMillis(60); // 60 days after 2nd dismiss
    public static final int SHOW_AFTER_OPENS_COUNT = 5;
    public static final int REST_AFTER_DISMISS_COUNT = 2;
    private static final Observable<Optional<FacebookInvitesItem>> NO_INVITES =
            Observable.just(Optional.<FacebookInvitesItem>absent());

    private final FacebookInvitesStorage facebookInvitesStorage;
    private final FacebookApiHelper facebookApiHelper;
    private final NetworkConnectionHelper networkConnectionHelper;
    private final FacebookApi facebookApi;
    private final Scheduler scheduler;

    @Inject
    public FacebookInvitesOperations(FacebookInvitesStorage facebookInvitesStorage,
                                     FacebookApi facebookApi,
                                     FacebookApiHelper facebookApiHelper,
                                     NetworkConnectionHelper networkConnectionHelper,
                                     @Named(ApplicationModule.HIGH_PRIORITY) Scheduler scheduler) {
        this.facebookInvitesStorage = facebookInvitesStorage;
        this.facebookApi = facebookApi;
        this.facebookApiHelper = facebookApiHelper;
        this.networkConnectionHelper = networkConnectionHelper;
        this.scheduler = scheduler;
    }

    public Observable<Optional<FacebookInvitesItem>> loadWithPictures() {
        if (canShow()) {
            return facebookApi
                    .friendPictureUrls()
                    .subscribeOn(scheduler)
                    .map(buildItemWithPictureUrls())
                    .onErrorResumeNext(NO_INVITES);
        } else {
            return NO_INVITES;
        }
    }

    private Func1<List<String>, Optional<FacebookInvitesItem>> buildItemWithPictureUrls() {
        return new Func1<List<String>, Optional<FacebookInvitesItem>>() {
            @Override
            public Optional<FacebookInvitesItem> call(List<String> friendPictureUrls) {
                return Optional.of(new FacebookInvitesItem(friendPictureUrls));
            }
        };
    }

    public boolean canShow() {
        return canShowAfterLastClick()
                && canShowAfterLastOpen()
                && canShowAfterLastDismisses()
                && facebookApiHelper.canShowAppInviteDialog()
                && networkConnectionHelper.isNetworkConnected();
    }

    private boolean canShowAfterLastOpen() {
        return facebookInvitesStorage.getTimesAppOpened() >= SHOW_AFTER_OPENS_COUNT;
    }

    private boolean canShowAfterLastClick() {
        return facebookInvitesStorage.getMillisSinceLastClick() >= CLICK_INTERVAL_MS;
    }

    private boolean canShowAfterLastDismisses() {
        int timesDismissed = facebookInvitesStorage.getTimesDismissed();
        long lastDismiss = facebookInvitesStorage.getMillisSinceLastDismiss();

        if (timesDismissed == 0) {
            return true;                                        // yes - dismissed == 0
        } else if (lastDismiss < CLICK_INTERVAL_MS) {
            return false;                                       // no  - dismissed  > 0 - lastDismiss  < 14 days
        } else if (timesDismissed < REST_AFTER_DISMISS_COUNT) {
            return true;                                        // yes - dismissed  < 2 - lastDismiss >= 14 days
        } else if (lastDismiss < DISMISS_INTERVAL_MS) {
            return false;                                       // no  - dismissed >= 2 - lastDismiss  < 60 days
        } else {
            facebookInvitesStorage.resetDismissed();
            return true;                                        // yes - dismissed >= 2 - lastDismiss >= 60 days
        }
    }

}
