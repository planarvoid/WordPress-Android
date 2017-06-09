package com.soundcloud.android.users;

import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.sync.SyncInitiator;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Scheduler;

import javax.inject.Inject;
import javax.inject.Named;

public class UserRepository {

    private final UserStorage userStorage;
    private final SyncInitiator syncInitiator;
    private final Scheduler scheduler;

    @Inject
    public UserRepository(UserStorage userStorage,
                          SyncInitiator syncInitiator,
                          @Named(ApplicationModule.RX_HIGH_PRIORITY) Scheduler scheduler) {
        this.userStorage = userStorage;
        this.syncInitiator = syncInitiator;
        this.scheduler = scheduler;
    }

    /***
     * Returns a user from local storage if it exists, and backfills from the api if the user is not found locally
     */
    public Maybe<User> userInfo(Urn userUrn) {
        return Maybe
                .concat(
                        userStorage.loadUser(userUrn),
                        syncedUserInfo(userUrn)
                )
                .firstElement()
                .subscribeOn(scheduler);
    }

    /***
     * Syncs a given user then returns the local user after the sync
     */
    public Maybe<User> syncedUserInfo(Urn userUrn) {
        return syncInitiator.syncUser(userUrn).flatMapMaybe(o -> localUserInfo(userUrn));
    }

    /***
     * Returns a local user, then syncs and emits the user again after the sync finishes
     */
    public Observable<User> localAndSyncedUserInfo(Urn userUrn) {
        return Maybe.concat(
                localUserInfo(userUrn),
                syncedUserInfo(userUrn)
        ).toObservable();
    }

    /***
     * Returns a user from local storage only, or completes without emitting if no user found
     */
    public Maybe<User> localUserInfo(Urn userUrn) {
        return userStorage.loadUser(userUrn).subscribeOn(scheduler);
    }

}
