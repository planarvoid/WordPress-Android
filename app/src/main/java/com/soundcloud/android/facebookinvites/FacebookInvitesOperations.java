package com.soundcloud.android.facebookinvites;

import static com.soundcloud.android.rx.RxUtils.IS_TRUE;

import com.soundcloud.android.facebookapi.FacebookApiHelper;
import com.soundcloud.android.profile.LastPostedTrack;
import com.soundcloud.android.profile.MyProfileOperations;
import com.soundcloud.android.rx.RxJava;
import com.soundcloud.android.stream.StreamItem;
import com.soundcloud.android.utils.ConnectionHelper;
import com.soundcloud.android.utils.CurrentDateProvider;
import com.soundcloud.android.utils.DateProvider;
import io.reactivex.Maybe;
import rx.Observable;
import rx.functions.Func1;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;

public class FacebookInvitesOperations {

    public static final long CLICK_INTERVAL_MS = TimeUnit.DAYS.toMillis(14); // 14 days between clicks
    public static final long DISMISS_INTERVAL_MS = TimeUnit.DAYS.toMillis(60); // 60 days after 2nd dismiss
    public static final long LAST_POST_INTERVAL_MS = TimeUnit.HOURS.toMillis(72); // up to 72h after post
    public static final long CREATOR_DISMISS_INTERVAL_MS = TimeUnit.HOURS.toMillis(72); // 72h after dismiss
    public static final long CREATOR_DISMISS_FOR_LISTENERS_INTERVAL_MS = TimeUnit.HOURS.toMillis(24); // 24h after creator dismiss for listeners :facepalm:
    public static final int SHOW_AFTER_OPENS_COUNT = 5;
    public static final int REST_AFTER_DISMISS_COUNT = 2;

    private final FacebookInvitesStorage facebookInvitesStorage;
    private final FacebookApiHelper facebookApiHelper;
    private final ConnectionHelper connectionHelper;
    private final DateProvider dateProvider;
    private final MyProfileOperations myProfileOperations;

    private final Func1<LastPostedTrack, Observable<StreamItem>> toCreatorInvitesItem =
            track -> {
                if (isPostRecentlyCreated(track)) {
                    return Observable.just(StreamItem.forFacebookCreatorInvites(track.urn(),
                                                                                track.permalinkUrl()));
                } else {
                    return Observable.empty();
                }
            };

    @Inject
    public FacebookInvitesOperations(FacebookInvitesStorage facebookInvitesStorage,
                                     FacebookApiHelper facebookApiHelper,
                                     ConnectionHelper connectionHelper,
                                     CurrentDateProvider dateProvider,
                                     MyProfileOperations myProfileOperations) {
        this.facebookInvitesStorage = facebookInvitesStorage;
        this.facebookApiHelper = facebookApiHelper;
        this.connectionHelper = connectionHelper;
        this.dateProvider = dateProvider;
        this.myProfileOperations = myProfileOperations;
    }

    public Maybe<StreamItem> creatorInvites() {
        final Observable<StreamItem> observable = canShowForCreators()
                .filter(IS_TRUE)
                .flatMap(o -> myProfileOperations.lastPublicPostedTrack()
                                                 .flatMap(toCreatorInvitesItem)
                                                 .onErrorResumeNext(Observable.empty()));
        return RxJava.toV2Observable(observable).firstElement();
    }

    public Maybe<StreamItem> listenerInvites() {
        final Observable<StreamItem> observable = canShowForListeners()
                .filter(IS_TRUE)
                .flatMap(o -> Observable.just(StreamItem.forFacebookListenerInvites()));
        return RxJava.toV2Observable(observable).firstElement();
    }

    private Observable<Boolean> canShowForCreators() {
        return Observable.fromCallable(() -> canShowAfterLastClick()
                && canShowCreatorsAfterLastCreatorDismiss()
                && facebookApiHelper.canShowAppInviteDialog()
                && connectionHelper.isNetworkConnected());
    }

    Observable<Boolean> canShowForListeners() {
        return Observable.fromCallable(() -> canShowAfterLastClick()
                && canShowAfterLastOpen()
                && canShowAfterLastDismisses()
                && facebookApiHelper.canShowAppInviteDialog()
                && connectionHelper.isNetworkConnected());
    }

    private boolean canShowAfterLastOpen() {
        return facebookInvitesStorage.getTimesAppOpened() >= SHOW_AFTER_OPENS_COUNT;
    }

    private boolean canShowAfterLastClick() {
        return facebookInvitesStorage.getMillisSinceLastClick() >= CLICK_INTERVAL_MS;
    }

    private boolean canShowAfterLastDismisses() {
        int timesDismissed = facebookInvitesStorage.getTimesListenerDismissed();
        long lastDismiss = facebookInvitesStorage.getMillisSinceLastListenerDismiss();
        long lastCreatorsDismiss = facebookInvitesStorage.getMillisSinceLastCreatorDismiss();

        if (lastCreatorsDismiss < CREATOR_DISMISS_FOR_LISTENERS_INTERVAL_MS) {
            return false;                                       // no  - creatorDismissed < 24 hours
        } else if (timesDismissed == 0) {
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

    private boolean canShowCreatorsAfterLastCreatorDismiss() {
        return facebookInvitesStorage.getMillisSinceLastCreatorDismiss() >= CREATOR_DISMISS_INTERVAL_MS;
    }

    private boolean isPostRecentlyCreated(LastPostedTrack track) {
        return getMillisSincePosted(track) < LAST_POST_INTERVAL_MS;
    }

    private long getMillisSincePosted(LastPostedTrack track) {
        return dateProvider.getCurrentTime() - track.createdAt().getTime();
    }

}
