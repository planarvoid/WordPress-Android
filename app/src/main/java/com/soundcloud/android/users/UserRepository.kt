package com.soundcloud.android.users

import com.soundcloud.android.ApplicationModule.RX_HIGH_PRIORITY
import com.soundcloud.android.events.EventQueue
import com.soundcloud.android.model.Urn
import com.soundcloud.android.storage.RepositoryMissedSyncEvent
import com.soundcloud.android.sync.SyncInitiator
import com.soundcloud.android.utils.OpenForTesting
import com.soundcloud.propeller.ChangeResult
import com.soundcloud.rx.eventbus.EventBusV2
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.Single
import org.jetbrains.annotations.NotNull
import javax.inject.Inject
import javax.inject.Named

@OpenForTesting
class UserRepository
@Inject
constructor(private val userStorage: UserStorage,
            private val syncInitiator: SyncInitiator,
            @param:Named(RX_HIGH_PRIORITY) private val scheduler: Scheduler,
            private val eventBus: EventBusV2) {

    /***
     * Returns a user from local storage if it exists, and backfills from the api if the user is not found locally
     */
    fun userInfo(userUrn: Urn): Maybe<User> {
        return userStorage.loadUser(userUrn)
                .switchIfEmpty(syncedUserInfo(userUrn))
                .switchIfEmpty(logEmpty())
                .subscribeOn(scheduler)
    }

    /***
     * Returns a list of users from local storage if they exist, and backfills from the api if the users are not found locally.
     * The list might be a subset of the request urns if they are not found even after the server sync.
     */
    fun usersInfo(userUrns: List<Urn>): Single<List<User>> = usersInfoMap(userUrns).map { it.values.toList() }

    fun usersInfoMap(userUrns: List<Urn>): Single<Map<Urn, User>> {
        checkUsersUrn(userUrns)
        return userStorage.loadUserMap(userUrns)
                .flatMap { foundUsers -> syncMissingUsers(userUrns, foundUsers) }
                .doOnSuccess { reportMissingAfterSync(it.size, userUrns.size) }
                .subscribeOn(scheduler)
    }

    private fun checkUsersUrn(userUrns: Collection<Urn>) {
        val urn = userUrns.find { !it.isUser }
        if (urn != null) {
            throw IllegalArgumentException("Trying to sync user without a valid user urn. userUrns = [$userUrns]")
        }
    }

    private fun syncMissingUsers(requiredUsers: List<Urn>, foundUsers: Map<Urn, User>): Single<Map<Urn, User>> {
        if (foundUsers.size == requiredUsers.size) {
            return Single.just(foundUsers)
        } else {
            return doSyncMissingUsers(requiredUsers, foundUsers)
        }
    }

    private fun doSyncMissingUsers(requiredUsers: List<Urn>, foundUsers: Map<Urn, User>): Single<Map<Urn, User>> {
        val missingUserUrns = requiredUsers.minus(foundUsers.keys)
        return syncInitiator.batchSyncUsers(missingUserUrns)
                .flatMap { userStorage.loadUserMap(requiredUsers) }
    }

    /***
     * Syncs a given user then returns the local user after the sync
     */
    fun syncedUserInfo(userUrn: Urn): Maybe<User> {
        return syncInitiator.syncUser(userUrn)
                .flatMapMaybe { localUserInfo(userUrn) }
                .switchIfEmpty(logEmpty())
    }

    /***
     * Returns a local user, then syncs and emits the user again after the sync finishes
     */
    fun localAndSyncedUserInfo(userUrn: Urn): Observable<User> {
        return Maybe.concat(
                localUserInfo(userUrn),
                syncedUserInfo(userUrn)
        ).toObservable()
                .switchIfEmpty { Observable.empty<User>().doOnComplete { logMissing(1) } }
    }

    /***
     * Returns a user from local storage only, or completes without emitting if no user found
     */
    fun localUserInfo(userUrn: Urn): Maybe<User> {
        return userStorage.loadUser(userUrn)
                .subscribeOn(scheduler)
    }

    fun updateFollowersCount(urn: Urn, followersCount: Int): Single<ChangeResult> {
        return userStorage.updateFollowersCount(urn, followersCount)
    }

    private fun <T> logEmpty() = Maybe.empty<T>().doOnComplete { logMissing(1) }

    private fun reportMissingAfterSync(loadedCount: Int, requestedCount: Int) {
        if (requestedCount != loadedCount) {
            logMissing(requestedCount - loadedCount)
        }
    }

    private fun logMissing(missingCount: Int) {
        eventBus.publish(EventQueue.TRACKING, RepositoryMissedSyncEvent.fromUsersMissing(missingCount))
    }
}
