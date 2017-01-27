package com.soundcloud.android.users;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.sync.SyncInitiator;
import rx.Observable;
import rx.Scheduler;

import javax.inject.Inject;
import javax.inject.Named;

public class UserRepository {

    private final UserStorage userStorage;
    private final SyncInitiator syncInitiator;
    private final Scheduler scheduler;

    @Inject
    public UserRepository(UserStorage userStorage,
                          SyncInitiator syncInitiator,
                          @Named(ApplicationModule.HIGH_PRIORITY) Scheduler scheduler) {
        this.userStorage = userStorage;
        this.syncInitiator = syncInitiator;
        this.scheduler = scheduler;
    }

    /***
     * Returns a user from local storage if it exists, and backfills from the api if the user is not found locally
     */
    public Observable<User> userInfo(Urn userUrn) {
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
    public Observable<User> syncedUserInfo(Urn userUrn) {
        return syncInitiator.syncUser(userUrn).flatMap(o -> localUserInfo(userUrn));
    }

    /***
     * Returns a local user, then syncs and emits the user again after the sync finishes
     */
    public Observable<User> localAndSyncedUserInfo(Urn userUrn) {
        return Observable.concat(
                localUserInfo(userUrn),
                syncedUserInfo(userUrn)
        );
    }

    /***
     * Returns a user from local storage only, or completes without emitting if no user found
     */
    public Observable<User> localUserInfo(Urn userUrn) {
        return userStorage.loadUser(userUrn).subscribeOn(scheduler);
    }

}
