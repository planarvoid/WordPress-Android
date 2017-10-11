package com.soundcloud.android.associations

import com.soundcloud.android.ApplicationModule
import com.soundcloud.android.Consts
import com.soundcloud.android.events.EventQueue
import com.soundcloud.android.events.EventQueue.FOLLOWING_CHANGED
import com.soundcloud.android.events.FollowingStatusEvent
import com.soundcloud.android.model.Urn
import com.soundcloud.android.sync.NewSyncOperations
import com.soundcloud.android.sync.Syncable
import com.soundcloud.android.users.FollowingStorage
import com.soundcloud.android.users.UserItem
import com.soundcloud.android.users.UserItemRepository
import com.soundcloud.android.users.UserRepository
import com.soundcloud.android.utils.OpenForTesting
import com.soundcloud.propeller.ChangeResult
import com.soundcloud.rx.eventbus.EventBusV2
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.Single
import io.reactivex.functions.BiFunction
import io.reactivex.rxkotlin.Singles
import javax.inject.Inject
import javax.inject.Named

@OpenForTesting
class FollowingOperations
@Inject
internal
constructor(private val eventBus: EventBusV2,
            @param:Named(ApplicationModule.RX_HIGH_PRIORITY) private val scheduler: Scheduler,
            private val userRepository: UserRepository,
            private val userItemRepository: UserItemRepository,
            private val syncOperations: NewSyncOperations,
            private val followingStorage: FollowingStorage) {

    fun populatedOnUserFollowed(): Observable<UserItem> {
        return onUserFollowed().flatMap { urn ->
            userItemRepository.userItem(urn)
                    .subscribeOn(scheduler)
                    .toObservable()
        }
    }

    fun onUserFollowed(): Observable<Urn> {
        return eventBus.queue(FOLLOWING_CHANGED)
                .filter { it.isFollowed }
                .map { it.urn() }
    }

    fun onUserUnfollowed(): Observable<Urn> {
        return eventBus.queue(FOLLOWING_CHANGED)
                .filter { event -> !event.isFollowed }
                .map { it.urn() }
    }

    fun toggleFollowing(userUrn: Urn, following: Boolean): Completable {
        return updateFollowing(userUrn, following)
                .flatMap { followingCount ->
                    syncOperations.failSafeSync(Syncable.MY_FOLLOWINGS)
                            .map {
                                if (following)
                                    FollowingStatusEvent.createFollowed(userUrn, followingCount)
                                else
                                    FollowingStatusEvent.createUnfollowed(userUrn, followingCount)
                            }
                }
                .doOnSuccess { event -> eventBus.publish(EventQueue.FOLLOWING_CHANGED, event) }
                .subscribeOn(scheduler)
                .toCompletable()
    }

    private fun updateFollowing(userUrn: Urn, following: Boolean): Single<Int> {
        return obtainNewFollowersCount(userUrn, following)
                .flatMap { count ->
                    Single.zip(userRepository.updateFollowersCount(userUrn, count),
                               followingStorage.insertFollowing(userUrn, following),
                               BiFunction<ChangeResult, Long, Int> { _, _ -> count })
                }
    }

    private fun obtainNewFollowersCount(userUrn: Urn, following: Boolean): Single<Int> {
        return Singles.zip(userRepository.userInfo(userUrn).map { it.followersCount() }.toSingle(Consts.NOT_SET),
                           followingStorage.isFollowing(userUrn),
                           { currentFollowersCount: Int, currentFollowingState: Boolean ->
                               if (currentFollowingState == following || currentFollowersCount == Consts.NOT_SET) {
                                   currentFollowersCount
                               } else if (following) {
                                   currentFollowersCount + 1
                               } else {
                                   currentFollowersCount - 1
                               }
                           })
    }
}
