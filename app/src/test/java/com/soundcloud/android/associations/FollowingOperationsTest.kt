package com.soundcloud.android.associations

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.eq
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import com.soundcloud.android.events.EventQueue
import com.soundcloud.android.events.FollowingStatusEvent
import com.soundcloud.android.sync.NewSyncOperations
import com.soundcloud.android.sync.SyncResult
import com.soundcloud.android.sync.Syncable
import com.soundcloud.android.testsupport.fixtures.ModelFixtures
import com.soundcloud.android.users.FollowingStorage
import com.soundcloud.android.users.User
import com.soundcloud.android.users.UserItemRepository
import com.soundcloud.android.users.UserRepository
import com.soundcloud.propeller.ChangeResult
import com.soundcloud.rx.eventbus.EventBusV2
import com.soundcloud.rx.eventbus.TestEventBusV2
import io.reactivex.Maybe
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import org.assertj.core.api.Assertions
import org.assertj.core.api.Java6Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers
import org.mockito.Captor
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.junit.MockitoJUnitRunner

@Suppress("IllegalIdentifier")
@RunWith(MockitoJUnitRunner::class)
class FollowingOperationsTest {

    private val followerCount = 2

    private lateinit var operations: FollowingOperations

    @Mock private lateinit var syncOperations: NewSyncOperations
    @Mock private lateinit var mockEventBus: EventBusV2
    @Mock private lateinit var userRepository: UserRepository
    @Mock private lateinit var userItemRepository: UserItemRepository
    @Mock private lateinit var followingStorage: FollowingStorage
    @Captor private lateinit var followingChangedEventArgumentCaptor: ArgumentCaptor<FollowingStatusEvent>
    private var userInfoSubscribed: Boolean = false
    private var insertFollowingSubscribed: Boolean = false
    private var syncSubscribed: Boolean = false

    private val eventBus = TestEventBusV2()
    private val scheduler = Schedulers.trampoline()
    private val userBuilder = ModelFixtures.userBuilder()
    private val targetUrn = userBuilder.build().urn()

    @Before
    @Throws(Exception::class)
    fun setUp() {
        userInfoSubscribed = false
        insertFollowingSubscribed = false
        syncSubscribed = false
        operations = FollowingOperations(eventBus,
                                         scheduler,
                                         userRepository,
                                         userItemRepository,
                                         syncOperations,
                                         followingStorage)
    }

    @Test
    fun toggleFollowingStoresThenSyncThenEmitsChangeSet() {
        val ops = FollowingOperations(mockEventBus,
                                      scheduler,
                                      userRepository,
                                      userItemRepository,
                                      syncOperations,
                                      followingStorage)

        setupFollowings(userBuilder.build(), true, true)

        ops.toggleFollowing(targetUrn, true).test()

        val inOrder = Mockito.inOrder(syncOperations, mockEventBus)
        inOrder.verify(syncOperations).failSafeSync(Syncable.MY_FOLLOWINGS)
        inOrder.verify(mockEventBus).publish(ArgumentMatchers.eq(EventQueue.FOLLOWING_CHANGED), followingChangedEventArgumentCaptor.capture())

        val event = followingChangedEventArgumentCaptor.value
        assertThat(event).isEqualTo(FollowingStatusEvent.createFollowed(targetUrn, followerCount))
        assertThat(syncSubscribed).isTrue()
    }

    @Test
    fun onUserFollowedEmitsFollowedUser() {
        val testObserver = operations.populatedOnUserFollowed().test()

        val event = FollowingStatusEvent.createFollowed(targetUrn, followerCount)
        val following = ModelFixtures.userItem()
        whenever(userItemRepository.userItem(targetUrn)).thenReturn(Maybe.just(following))

        eventBus.publish(EventQueue.FOLLOWING_CHANGED, event)

        testObserver.assertValues(following)
    }

    @Test
    fun onUserUnFollowedEmitsUrnOfUnfollowedUser() {
        val subscriber = operations.onUserUnfollowed().test()

        val event = FollowingStatusEvent.createUnfollowed(targetUrn, followerCount)
        eventBus.publish(EventQueue.FOLLOWING_CHANGED, event)

        subscriber.assertValues(targetUrn)
    }

    @Test
    fun `updates followed user on unfollow`() {
        val currentFollowingState = true
        val newFollowingState = false
        val user = userBuilder.followersCount(1).build()

        setupFollowings(user, newFollowingState, currentFollowingState)

        val test = operations.toggleFollowing(user.urn(), newFollowingState).test()

        test.assertComplete()

        Assertions.assertThat(eventBus.eventsOn(EventQueue.FOLLOWING_CHANGED).size).isEqualTo(1)
        (eventBus.eventsOn(EventQueue.FOLLOWING_CHANGED)[0]).let {
            assertThat(it.urn()).isEqualTo(user.urn())
            assertThat(it.isFollowed).isFalse()
            assertThat(it.followingsCount()).isEqualTo(0)
        }
        verify(userRepository).updateFollowersCount(user.urn(), 0)
        verify(followingStorage).insertFollowing(user.urn(), newFollowingState)
        assertThat(userInfoSubscribed).isTrue()
        assertThat(insertFollowingSubscribed).isTrue()
    }

    @Test
    fun `updates not followed user on follow`() {
        val currentFollowingState = false
        val newFollowingState = true
        val user = userBuilder.followersCount(0).build()

        setupFollowings(user, newFollowingState, currentFollowingState)

        val test = operations.toggleFollowing(user.urn(), newFollowingState).test()

        test.assertComplete()

        Assertions.assertThat(eventBus.eventsOn(EventQueue.FOLLOWING_CHANGED).size).isEqualTo(1)
        (eventBus.eventsOn(EventQueue.FOLLOWING_CHANGED)[0]).let {
            assertThat(it.urn()).isEqualTo(user.urn())
            assertThat(it.isFollowed).isTrue()
            assertThat(it.followingsCount()).isEqualTo(1)
        }
        verify(userRepository).updateFollowersCount(user.urn(), 1)
        verify(followingStorage).insertFollowing(user.urn(), newFollowingState)
        assertThat(userInfoSubscribed).isTrue()
        assertThat(insertFollowingSubscribed).isTrue()
    }

    @Test
    fun `does not change user when trying to follow followed user`() {
        val currentFollowingState = true
        val newFollowingState = true
        val user = userBuilder.followersCount(1).build()

        setupFollowings(user, newFollowingState, currentFollowingState)

        val test = operations.toggleFollowing(user.urn(), newFollowingState).test()

        test.assertComplete()

        Assertions.assertThat(eventBus.eventsOn(EventQueue.FOLLOWING_CHANGED).size).isEqualTo(1)
        (eventBus.eventsOn(EventQueue.FOLLOWING_CHANGED)[0]).let {
            assertThat(it.urn()).isEqualTo(user.urn())
            assertThat(it.isFollowed).isTrue()
            assertThat(it.followingsCount()).isEqualTo(1)
        }
        verify(userRepository).updateFollowersCount(user.urn(), 1)
        verify(followingStorage).insertFollowing(user.urn(), newFollowingState)
        assertThat(userInfoSubscribed).isTrue()
        assertThat(insertFollowingSubscribed).isTrue()
    }

    @Test
    fun `does not change user when trying to unfollow not followed user`() {
        val currentFollowingState = false
        val newFollowingState = false
        val user = userBuilder.followersCount(0).build()

        setupFollowings(user, newFollowingState, currentFollowingState)

        val test = operations.toggleFollowing(user.urn(), newFollowingState).test()

        test.assertComplete()

        Assertions.assertThat(eventBus.eventsOn(EventQueue.FOLLOWING_CHANGED).size).isEqualTo(1)
        (eventBus.eventsOn(EventQueue.FOLLOWING_CHANGED)[0]).let {
            assertThat(it.urn()).isEqualTo(user.urn())
            assertThat(it.isFollowed).isFalse()
            assertThat(it.followingsCount()).isEqualTo(0)
        }
        verify(userRepository).updateFollowersCount(user.urn(), 0)
        verify(followingStorage).insertFollowing(user.urn(), newFollowingState)
        assertThat(userInfoSubscribed).isTrue()
        assertThat(insertFollowingSubscribed).isTrue()
    }

    private fun setupFollowings(user: User, newFollowingState: Boolean, currentFollowingState: Boolean = false) {
        val userUrn = user.urn()
        whenever(userRepository.userInfo(userUrn)).thenReturn(Maybe.just(user).doOnSubscribe { userInfoSubscribed = true })

        whenever(followingStorage.isFollowing(userUrn)).thenReturn(Single.just(currentFollowingState))

        whenever(userRepository.updateFollowersCount(eq(userUrn), any())).thenReturn(Single.just(ChangeResult(1)))
        whenever(followingStorage.insertFollowing(userUrn, newFollowingState)).thenReturn(Single.just(1L).doOnSubscribe { insertFollowingSubscribed = true })

        whenever(syncOperations.failSafeSync(Syncable.MY_FOLLOWINGS)).thenReturn(Single.just(SyncResult.synced()).doOnSubscribe { syncSubscribed = true })
    }
}
