package com.soundcloud.android.users

import com.soundcloud.android.model.Urn
import com.soundcloud.android.utils.OpenForTesting
import com.soundcloud.java.collections.Lists
import io.reactivex.Single
import java.util.Date
import javax.inject.Inject

@OpenForTesting
class FollowingStorage
@Inject
constructor(private val database: FollowingDatabase) {
    private val batchSize = 500

    fun clear() {
        database.cleanUp()
    }

    fun deleteFollowingsById(itemDeletions: List<Urn>) {
        if (!itemDeletions.isEmpty()) {
            val batches = Lists.partition(itemDeletions, batchSize)
            for (idBatch in batches) {
                database.execute(FOLLOWING_FACTORY.deleteIn(idBatch.toTypedArray()))
            }
        }
    }

    fun insertFollowedUserIds(userIds: List<Urn>) {
        val insertRow = FollowingModel.InsertRow(database.writableDatabase(), FOLLOWING_FACTORY)
        userIds.forEachIndexed { index, userId ->
            insertRow.bind(userId, index.toLong())
            database.insert(FollowingModel.TABLE_NAME, insertRow)
        }
    }

    //TODO: this is business logic and belongs in the syncer. Let's move this once we migrate
    // affiliations to api-mobile and rewrite the syncer
    fun updateFollowingFromPendingState(followedUser: Urn) {
        val following = syncLoadFollowedUser(followedUser)
        if (following?.addedAt != null) {
            // following is pending addition; clear the timestamp
            val clearAddedAt = FollowingModel.ClearAddedAt(database.writableDatabase(), FOLLOWING_FACTORY)
            clearAddedAt.bind(followedUser)
            database.updateOrDelete(FollowingModel.TABLE_NAME, clearAddedAt)
        } else if (following?.removedAt != null) {
            // following is pending removal; delete it
            val deleteByTargetId = FollowingModel.DeleteByTargetId(database.writableDatabase(), FOLLOWING_FACTORY)
            deleteByTargetId.bind(followedUser)
            database.updateOrDelete(FollowingModel.TABLE_NAME, deleteByTargetId)
        }
    }

    fun selectAllUrns(): Set<Urn> = database.executeQuery(FOLLOWING_FACTORY.selectAll(), FOLLOWING_FACTORY.selectAllMapper()).map { it.userUrn }.toSet()

    fun loadFollowedUserIds(): Set<Urn> = database.executeQuery(FOLLOWING_FACTORY.loadFollowedUserIds(), FOLLOWING_FACTORY.loadFollowedUserIdsMapper()).toSet()

    fun followedUsers(limit: Int, fromPosition: Long): Single<List<Following>> = database.executeAsyncQuery(FOLLOWING_FACTORY.selectOrdered(fromPosition, limit.toLong()),
                                                                                                            FOLLOWING_MAPPER).map { it.toList() }

    fun syncLoadFollowedUser(urn: Urn): Following? = database.executeQuery(FOLLOWING_FACTORY.selectById(urn), FOLLOWING_MAPPER).firstOrNull()

    fun followedUserUrns(limit: Int, fromPosition: Long): Single<List<Urn>> {
        return database.executeAsyncQuery(FOLLOWING_FACTORY.selectOrdered(fromPosition, limit.toLong()), FOLLOWING_FACTORY.selectOrderedMapper()).map { it.map { it.userUrn } }
    }

    fun followings(): Single<List<Following>> = database.executeAsyncQuery(FOLLOWING_FACTORY.loadFollowed(), FOLLOWING_MAPPER)

    fun hasStaleFollowings(): Boolean {
        val staleCount = database.executeSelectItemQuery(FOLLOWING_FACTORY.selectStaleCount(), FOLLOWING_FACTORY.selectStaleCountMapper())
        return staleCount != null && staleCount > 0L
    }

    fun loadFollowings(): Single<List<Following>> = database.executeAsyncQuery(FOLLOWING_FACTORY.selectAll(), FOLLOWING_FACTORY.selectAllMapper())

    fun loadStaleFollowings(): List<Following> = database.executeQuery(FOLLOWING_FACTORY.selectStale(), FOLLOWING_FACTORY.selectStaleMapper()).toList()

    fun isFollowing(targetUrn: Urn): Single<Boolean> {
        return database.executeAsyncSelectItemQuery(FOLLOWING_FACTORY.selectActiveFollowingCount(targetUrn), FOLLOWING_FACTORY.selectActiveFollowingCountMapper())
                .map { it == 1L }
                .toSingle(false)
    }

    fun insertFollowing(userUrn: Urn, following: Boolean): Single<Long> {
        return Single.fromCallable {
            syncInsertFollowing(userUrn, following)
        }
    }

    fun syncInsertFollowing(userUrn: Urn, following: Boolean): Long {
        val now = Date()
        val addedAt = if (following) now else null
        val removedAt = if (!following) now else null
        val insertOrReplaceFromToggle = FollowingModel.InsertOrReplaceFromToggle(database.writableDatabase(), FOLLOWING_FACTORY)
        insertOrReplaceFromToggle.bind(userUrn, addedAt, removedAt)
        return database.insert(FollowingModel.TABLE_NAME, insertOrReplaceFromToggle)
    }
}
