package com.soundcloud.android.users;

import static com.soundcloud.android.rx.RxUtils.continueWith;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.sync.SyncInitiator;
import com.soundcloud.java.collections.PropertySet;
import rx.Observable;
import rx.Scheduler;
import rx.functions.Func1;

import javax.inject.Inject;
import javax.inject.Named;

public class UserRepository {

    private final UserStorage userStorage;
    private final SyncInitiator syncInitiator;
    private final Scheduler scheduler;

    private static final Func1<PropertySet, Boolean> IS_NOT_EMPTY = new Func1<PropertySet, Boolean>() {
        @Override
        public Boolean call(PropertySet user) {
            return !user.isEmpty();
        }
    };

    @Inject
    public UserRepository(UserStorage userStorage, SyncInitiator syncInitiator, @Named(ApplicationModule.HIGH_PRIORITY) Scheduler scheduler) {
        this.userStorage = userStorage;
        this.syncInitiator = syncInitiator;
        this.scheduler = scheduler;
    }

    /***
     * Returns a user from local storage if it exists, and backfills from the api if the user is not found locally
     */
    public Observable<PropertySet> userInfo(Urn userUrn) {
        return Observable
                .concat(
                        userStorage.loadUser(userUrn),
                        syncedUserInfo(userUrn)
                )
                .first()
                .subscribeOn(scheduler);
    }

    /***
     * Syncs a given user then returns the local user after the sync
     */
    public Observable<PropertySet> syncedUserInfo(Urn userUrn) {
        return syncInitiator.syncUser(userUrn).flatMap(continueWith(localUserInfo(userUrn)));
    }

    /***
     * Returns a local user, then syncs and emits the user again after the sync finishes
     */
    public Observable<PropertySet> localAndSyncedUserInfo(Urn userUrn) {
        return Observable.concat(
                localUserInfo(userUrn),
                syncedUserInfo(userUrn)
        );
    }

    /***
     * Returns a user from local storage only, or completes without emitting if no user found
     */
    public Observable<PropertySet> localUserInfo(Urn userUrn) {
        return userStorage.loadUser(userUrn).filter(IS_NOT_EMPTY).subscribeOn(scheduler);
    }

}
