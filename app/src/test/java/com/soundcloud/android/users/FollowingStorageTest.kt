package com.soundcloud.android.users

import com.soundcloud.android.model.Urn
import com.soundcloud.android.testsupport.StorageIntegrationTest
import io.reactivex.observers.TestObserver
import io.reactivex.schedulers.Schedulers
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.robolectric.RuntimeEnvironment
import java.util.Date

class FollowingStorageTest : StorageIntegrationTest() {

    private val urnSubscriber = TestObserver<List<Urn>>()

    private lateinit var storage: FollowingStorage
    private lateinit var database: FollowingDatabase
    private val followingUrn: Urn = Urn.forUser(1L)
    private val followingAndFollowerUrn: Urn= Urn.forUser(2L)

    @Mock private lateinit var followingDatabase: FollowingDatabase

    @Before
    @Throws(Exception::class)
    fun setUp() {
        database = FollowingDatabase(FollowingOpenHelper(RuntimeEnvironment.application), Schedulers.trampoline())
        storage = FollowingStorage(database)
    }

    @Test
    fun shouldClearTable() {
        storage.insertFollowing(followingUrn, true).test()
        storage.followings().test().assertValue { it.isNotEmpty() }

        storage.clear()

        storage.followings().test().assertValue { it.isEmpty() }
    }

    @Test
    fun shouldLoadIdsOfFollowingUsers() {
        storage.insertFollowedUserIds(listOf(followingUrn, followingAndFollowerUrn))

        Assertions.assertThat(storage.loadFollowedUserIds()).containsOnly(followingUrn, followingAndFollowerUrn)
    }

    @Test
    fun shouldLoadIdsOfFollowingUsersUnlessPendingAddition() {
        storage.insertFollowedUserIds(listOf(followingUrn))
        Assertions.assertThat(storage.loadFollowedUserIds()).containsOnly(
                followingUrn
        )
    }

    @Test
    fun shouldLoadIdsOfFollowingUsersUnlessPendingRemoval() {
        storage.insertFollowedUserIds(listOf(followingAndFollowerUrn))
        markFollowingAsRemoved(followingAndFollowerUrn)
        Assertions.assertThat(storage.loadFollowedUserIds()).isEmpty()
    }

    @Test
    fun loadFollowingsLoadsAllFollowings() {
        val urn1 = Urn.forUser(1L)
        val urn2 = Urn.forUser(2L)
        val urn3 = Urn.forUser(3L)
        val followedUserUrns = listOf(urn1, urn2, urn3)
        storage.insertFollowedUserIds(followedUserUrns)
        markFollowingAsRemoved(urn1)
        val subscriber = storage.followedUsers(3, 0).test()

        subscriber.assertValues(listOf(Following(urn2, 1), Following(urn3, 2)))
    }

    @Test
    fun loadFollowingsAdheresToLimit() {
        storage.syncInsertFollowing(followingAndFollowerUrn, true)

        val subscriber = storage.followedUsers(1, 0).test()

        subscriber.values()[0].assertContainsExactly(followingAndFollowerUrn, position = 0, added = true)
    }

    @Test
    fun loadFollowingsAdheresToPosition() {
        val userUrn = Urn.forUser(3L)
        storage.insertFollowedUserIds(listOf(Urn.forUser(1L), Urn.forUser(2L), userUrn))
        val subscriber = storage.followedUsers(2, 2).test()

        subscriber.values()[0].assertContainsExactly(userUrn, position = 2)
    }

    @Test
    fun loadFollowingsUrnsLoadsAllFollowings() {
        val userIds = listOf(Urn.forUser(1L), Urn.forUser(2L))
        storage.insertFollowedUserIds(userIds)

        storage.followedUserUrns(3, 0).subscribe(urnSubscriber)

        urnSubscriber.assertValues(userIds)
    }

    @Test
    fun loadFollowingsUrnsAdheresToLimit() {
        storage.insertFollowedUserIds(listOf(followingAndFollowerUrn))
        storage.followedUserUrns(1, 0).subscribe(urnSubscriber)
        urnSubscriber.assertValues(listOf(followingAndFollowerUrn))
    }

    @Test
    fun loadFollowingsUrnsAdheresToPosition() {
        storage.insertFollowedUserIds(listOf(Urn.forUser(1L), Urn.forUser(2L), Urn.forUser(3L)))
        storage.followedUserUrns(2, 2).subscribe(urnSubscriber)
        urnSubscriber.assertValues(listOf(Urn.forUser(3L)))
    }

    @Test
    fun loadsStaleFollowingsWhenPendingAddition() {
        val userUrn = Urn.forUser(3L)
        database.insertFollowingPendingAddition(userUrn, 123)

        val followings = storage.loadStaleFollowings()

        followings.assertContainsExactly(userUrn = userUrn, added = true)
    }

    @Test
    fun loadsStaleFollowingsWhenPendingRemoval() {
        storage.insertFollowing(followingUrn, false)
        database.insertFollowingPendingRemoval(followingUrn, 123)

        val followings = storage.loadStaleFollowings()

        followings.assertContainsExactly(userUrn = followingUrn, removed = true)

        Assertions.assertThat(followings).containsExactly(
                Following(userUrn = followingUrn, position = 0L, removedAt = Date(123)))
    }

    @Test
    fun hasStaleFollowingsIsFalseWhenAddedAtOrRemovedAtNotSet() {
        Assertions.assertThat(storage.hasStaleFollowings()).isFalse()
    }

    @Test
    fun hasStaleFollowingsIsTrueIfAddedAtIsSet() {
        markFollowingAsAdded(followingUrn)

        Assertions.assertThat(storage.hasStaleFollowings()).isTrue()
    }

    @Test
    fun hasStaleFollowingsIsTrueIfRemovedAtIsSet() {
        markFollowingAsRemoved(followingUrn)

        Assertions.assertThat(storage.hasStaleFollowings()).isTrue()
    }

    @Test
    fun shouldDeleteFollowingsById() {
        val userUrn = Urn.forUser(22L)
        storage.syncInsertFollowing(userUrn = userUrn, following = true)

        val following = storage.syncLoadFollowedUser(userUrn)
        following!!.let {
            Assertions.assertThat(it.userUrn).isEqualTo(userUrn)
            Assertions.assertThat(it.addedAt).isNotNull()
            Assertions.assertThat(it.removedAt).isNull()
        }

        storage.deleteFollowingsById(listOf(userUrn))

        Assertions.assertThat(storage.syncLoadFollowedUser(userUrn)).isNull()
    }

    @Test
    fun shouldResetPendingFollowingAddition() {
        markFollowingAsAdded(followingUrn)
        val addedAtBefore = storage.syncLoadFollowedUser(followingUrn)?.addedAt
        addedAtBefore!!.let {
            Assertions.assertThat(it > Date(0)).isTrue()
        }

        storage.updateFollowingFromPendingState(followingUrn)

        val addedAtAfter = storage.syncLoadFollowedUser(followingUrn)?.addedAt
        Assertions.assertThat(addedAtAfter).isNull()
    }

    @Test
    fun shouldDeletePendingFollowingRemoval() {
        storage.insertFollowedUserIds(listOf(followingUrn))

        markFollowingAsRemoved(followingUrn)

        Assertions.assertThat(storage.loadStaleFollowings().size).isEqualTo(1)

        storage.updateFollowingFromPendingState(followingUrn)

        Assertions.assertThat(storage.loadStaleFollowings().size).isEqualTo(0)
    }

    @Test
    fun shouldNotUpdateFollowingFromPendingStateIfNotFound() {
        storage.updateFollowingFromPendingState(Urn.forUser(99999))
    }

    @Test
    fun isFollowingAfterUpdate() {
        val userUrn = Urn.forUser(123L)
        storage.isFollowing(userUrn).test().assertValue { !it }

        storage.insertFollowing(userUrn, true).test()

        storage.isFollowing(userUrn).test().assertValue { it }
    }

    private fun markFollowingAsAdded(followingUrn: Urn) {
        storage.syncInsertFollowing(followingUrn, true)
    }

    private fun markFollowingAsRemoved(followingUrn: Urn) {
        storage.syncInsertFollowing(followingUrn, false)
    }

    private fun List<Following>.assertContainsExactly(userUrn: Urn, added: Boolean = false, removed: Boolean = false, position: Int = 0) {
        Assertions.assertThat(this.size).isEqualTo(1)
        Assertions.assertThat(this[0].userUrn).isEqualTo(userUrn)
        Assertions.assertThat(this[0].position).isEqualTo(position.toLong())
        if (added) {
            Assertions.assertThat(this[0].addedAt).isNotNull()
        } else {
            Assertions.assertThat(this[0].addedAt).isNull()
        }
        if (removed) {
            Assertions.assertThat(this[0].removedAt).isNotNull()
        } else {
            Assertions.assertThat(this[0].removedAt).isNull()
        }
    }
}
