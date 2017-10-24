package com.soundcloud.android.users

import com.nhaarman.mockito_kotlin.whenever
import com.soundcloud.android.model.Urn
import com.soundcloud.android.testsupport.UserFixtures
import com.soundcloud.android.testsupport.fixtures.ModelFixtures
import io.reactivex.Maybe
import io.reactivex.Single
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
@Suppress("IllegalIdentifier")
class UserItemRepositoryTest {
    private val userUrn = Urn.forUser(1L)

    @Mock private lateinit var userRepository: UserRepository
    @Mock private lateinit var followingStorage: FollowingStorage
    private lateinit var userItemRepository: UserItemRepository
    @Before
    fun setUp() {
        userItemRepository = UserItemRepository(userRepository, followingStorage, ModelFixtures.entityItemCreator())
    }

    @Test
    fun `loads followed local user item`() {
        whenever(userRepository.localUserInfo(userUrn)).thenReturn(Maybe.just(UserFixtures.user(userUrn)))
        whenever(followingStorage.isFollowing(userUrn)).thenReturn(Single.just(true))

        val observer = userItemRepository.localUserItem(userUrn).test()

        observer.assertValue { it.urn == userUrn }
        observer.assertValue { it.isFollowedByMe }
    }

    @Test
    fun `empty local user item when user missing`() {
        whenever(userRepository.localUserInfo(userUrn)).thenReturn(Maybe.empty())
        whenever(followingStorage.isFollowing(userUrn)).thenReturn(Single.just(true))

        val observer = userItemRepository.localUserItem(userUrn).test()

        observer.assertValueCount(0)
    }

    @Test
    fun `loads followed user item`() {
        whenever(userRepository.userInfo(userUrn)).thenReturn(Maybe.just(UserFixtures.user(userUrn)))
        whenever(followingStorage.isFollowing(userUrn)).thenReturn(Single.just(true))

        val observer = userItemRepository.userItem(userUrn).test()

        observer.assertValue { it.urn == userUrn }
        observer.assertValue { it.isFollowedByMe }
    }

    @Test
    fun `empty user item when user missing`() {
        whenever(userRepository.userInfo(userUrn)).thenReturn(Maybe.empty())
        whenever(followingStorage.isFollowing(userUrn)).thenReturn(Single.just(true))

        val observer = userItemRepository.userItem(userUrn).test()

        observer.assertValueCount(0)
    }

    @Test
    fun `converts followed api user to user item`() {
        val apiUser = UserFixtures.apiUser()
        whenever(followingStorage.isFollowing(apiUser.urn)).thenReturn(Single.just(true))

        val observer = userItemRepository.userItem(apiUser).test()

        observer.assertValue { it.isFollowedByMe }
        observer.assertValue { it.user() == User.fromApiUser(apiUser) }
    }

    @Test
    fun `converts not followed api user to user item`() {
        val apiUser = UserFixtures.apiUser()
        whenever(followingStorage.isFollowing(apiUser.urn)).thenReturn(Single.just(false))

        val observer = userItemRepository.userItem(apiUser).test()

        observer.assertValue { !it.isFollowedByMe }
        observer.assertValue { it.user() == User.fromApiUser(apiUser) }
    }

    @Test
    fun `converts list of api users to user items`() {
        val followedApiUser = UserFixtures.apiUser()
        val notFollowedApiUser = UserFixtures.apiUser()

        whenever(followingStorage.loadFollowings()).thenReturn(Single.just(listOf(Following(followedApiUser.urn, 0))))

        val observer = userItemRepository.userItems(listOf(followedApiUser, notFollowedApiUser)).test()


        observer.assertValue { it.size == 2 }
        observer.assertValue { it[0].isFollowedByMe }
        observer.assertValue { it[0].user() == User.fromApiUser(followedApiUser) }
        observer.assertValue { !it[1].isFollowedByMe }
        observer.assertValue { it[1].user() == User.fromApiUser(notFollowedApiUser) }
    }


    @Test
    fun `load user item map from urns`() {
        val followedUser = ModelFixtures.user()
        val notFollowedUser = ModelFixtures.user()
        val userUrns = listOf(followedUser.urn(), notFollowedUser.urn())

        whenever(followingStorage.loadFollowings()).thenReturn(Single.just(listOf(Following(followedUser.urn(), 0))))
        whenever(userRepository.usersInfo(userUrns)).thenReturn(Single.just(listOf(followedUser, notFollowedUser)))

        val observer = userItemRepository.userItemsMap(userUrns).test()

        observer.assertValue { it.size == 2 }
        observer.assertValue { it[followedUser.urn()]?.isFollowedByMe == true }
        observer.assertValue { it[followedUser.urn()]?.user() == followedUser }
        observer.assertValue { it[notFollowedUser.urn()]?.isFollowedByMe == false }
        observer.assertValue { it[notFollowedUser.urn()]?.user() == notFollowedUser }
    }
}
