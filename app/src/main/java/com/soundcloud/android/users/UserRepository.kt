package com.soundcloud.android.users

import com.soundcloud.android.ApplicationModule.RX_HIGH_PRIORITY
import com.soundcloud.android.model.Urn
import com.soundcloud.android.sync.SyncInitiator
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.Single

import javax.inject.Inject
import javax.inject.Named

open class UserRepository
@Inject
constructor(private val userStorage: UserStorage,
            private val syncInitiator: SyncInitiator,
            @param:Named(RX_HIGH_PRIORITY) private val scheduler: Scheduler) {

    /***
     * Returns a user from local storage if it exists, and backfills from the api if the user is not found locally
     */
    open fun userInfo(userUrn: Urn): Maybe<User> {
        return userStorage.loadUser(userUrn)
                .switchIfEmpty(syncedUserInfo(userUrn))
                .subscribeOn(scheduler)
    }

    /***
     * Returns a list of users from local storage if they exist, and backfills from the api if the users are not found locally.
     * The list might be a subset of the request urns if they are not found even after the server sync.
     */
    open fun usersInfo(userUrns: List<Urn>): Single<List<User>> {
        return userStorage.loadUsers(userUrns)
                .flatMap { foundUsers -> syncMissingUsers(userUrns, foundUsers) }
                .subscribeOn(scheduler)
    }

    private fun syncMissingUsers(requiredUsers: List<Urn>, foundUsers: List<User>): Single<List<User>> {
        if (foundUsers.size == requiredUsers.size) {
            return Single.just(foundUsers)
        } else {
            return doSyncMissingUsers(requiredUsers, foundUsers)
        }
    }

    private fun doSyncMissingUsers(requiredUsers: List<Urn>, foundUsers: List<User>): Single<List<User>> {
        val missingUserUrns = requiredUsers.minus(foundUsers.map { it.urn() })
        return syncInitiator.batchSyncUsers(missingUserUrns)
                .flatMap { userStorage.loadUsers(requiredUsers) }
    }

    /***
     * Syncs a given user then returns the local user after the sync
     */
    open fun syncedUserInfo(userUrn: Urn): Maybe<User> {
        return syncInitiator.syncUser(userUrn)
                .flatMapMaybe { localUserInfo(userUrn) }
    }

    /***
     * Returns a local user, then syncs and emits the user again after the sync finishes
     */
    open fun localAndSyncedUserInfo(userUrn: Urn): Observable<User> {
        return Maybe.concat(
                localUserInfo(userUrn),
                syncedUserInfo(userUrn)
        ).toObservable()
    }

    /***
     * Returns a user from local storage only, or completes without emitting if no user found
     */
    open fun localUserInfo(userUrn: Urn): Maybe<User> {
        return userStorage.loadUser(userUrn)
                .subscribeOn(scheduler)
    }

}
