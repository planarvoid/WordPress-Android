package com.soundcloud.android.sync.affiliations

import android.app.Notification
import android.app.NotificationManager
import com.nhaarman.mockito_kotlin.eq
import com.nhaarman.mockito_kotlin.whenever
import com.soundcloud.android.NotificationConstants
import com.soundcloud.android.api.ApiClient
import com.soundcloud.android.api.ApiEndpoints
import com.soundcloud.android.api.ApiRequestException
import com.soundcloud.android.api.ApiResponse
import com.soundcloud.android.api.TestApiResponses
import com.soundcloud.android.api.json.JacksonJsonTransformer
import com.soundcloud.android.api.model.ModelCollection
import com.soundcloud.android.associations.FollowingOperations
import com.soundcloud.android.model.Urn
import com.soundcloud.android.navigation.NavigationExecutor
import com.soundcloud.android.testsupport.AndroidUnitTest
import com.soundcloud.android.testsupport.UserFixtures
import com.soundcloud.android.testsupport.matchers.RequestMatchers.isApiRequestTo
import com.soundcloud.android.users.Following
import com.soundcloud.android.users.FollowingStorage
import com.soundcloud.android.users.UserStorage
import com.soundcloud.http.HttpStatus
import com.soundcloud.java.collections.Lists
import com.soundcloud.java.optional.Optional
import com.soundcloud.java.reflect.TypeToken
import io.reactivex.Maybe
import io.reactivex.subjects.CompletableSubject
import org.assertj.core.api.Java6Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.hamcrest.MockitoHamcrest.argThat
import java.util.Arrays
import java.util.Date
import java.util.HashSet

class MyFollowingsSyncerTest : AndroidUnitTest() {

    private val USER_1 = Urn.forUser(1)
    private val USER_2 = Urn.forUser(2)
    private val USERNAME_1 = "User1"
    private val USERNAME_2 = "User2"
    private val AGE_RESTRICTED_BODY = "{\"error_key\":\"age_restricted\",\"age\":19}"
    private val AGE_UNKNOWN_BODY = "{\"error_key\":\"age_unknown\"}"
    private val BLOCKED_BODY = "{\"error_key\":\"blocked\"}"
    private val UNKNOWN_BODY = "{\"error_key\":\"unknown\"}"

    private lateinit var myFollowingsSyncer: MyFollowingsSyncer

    @Mock private lateinit var notificationBuilder: MyFollowingsSyncer.FollowErrorNotificationBuilder
    @Mock private lateinit var apiClient: ApiClient
    @Mock private lateinit var followingOperations: FollowingOperations
    @Mock private lateinit var notificationManger: NotificationManager
    @Mock private lateinit var navigationExecutor: NavigationExecutor
    @Mock private lateinit var followingStorage: FollowingStorage
    @Mock private lateinit var userStorage: UserStorage
    private val jsonTransformer = JacksonJsonTransformer()
    private lateinit var followingStatusPublishSubject: CompletableSubject

    @Before
    @Throws(Exception::class)
    fun setUp() {
        myFollowingsSyncer = MyFollowingsSyncer(notificationBuilder,
                                                apiClient,
                                                followingOperations,
                                                notificationManger,
                                                jsonTransformer,
                                                followingStorage,
                                                userStorage)
    }

    @Test
    @Throws(Exception::class)
    fun pushesFollowings() {
        whenever(followingStorage.hasStaleFollowings()).thenReturn(true)
        val user1 = UserFixtures.userBuilder().urn(USER_1).username(USERNAME_1).build()
        val user2 = UserFixtures.userBuilder().urn(USER_2).username(USERNAME_2).build()
        whenever(userStorage.loadUser(USER_1)).thenReturn(Maybe.just(user1))
        whenever(userStorage.loadUser(USER_2)).thenReturn(Maybe.just(user2))
        whenever(followingStorage.loadStaleFollowings()).thenReturn(
                Arrays.asList(
                        getNewFollowingAddition(USER_1),
                        getNewFollowingRemoval(USER_2)
                )
        )

        mockApiFollowingAddition(USER_1, TestApiResponses.ok())
        mockApiFollowingRemoval(USER_2, TestApiResponses.ok())
        mockApiFollowingsResponse(
                Arrays.asList(
                        getApiFollowing(USER_1),
                        getApiFollowing(USER_2)
                )
        )

        assertThat(myFollowingsSyncer.call()).isTrue()

        verify(followingStorage).updateFollowingFromPendingState(USER_1)
        verify(followingStorage).updateFollowingFromPendingState(USER_2)
    }

    @Test
    @Throws(Exception::class)
    fun storesFollowings() {
        val followings = Arrays.asList(
                getApiFollowing(USER_1),
                getApiFollowing(USER_2)
        )
        mockApiFollowingsResponse(followings)

        assertThat(myFollowingsSyncer.call()).isTrue()

        verify<FollowingStorage>(followingStorage).insertFollowedUserIds(toUserIds(followings))
    }

    @Test
    @Throws(Exception::class)
    fun doesNotStoreUnchangedFollowings() {
        val followings = Arrays.asList(
                getApiFollowing(USER_1),
                getApiFollowing(USER_2)
        )

        mockApiFollowingsResponse(
                followings
        )
        val userIds = toUserIds(followings)
        whenever(followingStorage.loadFollowedUserIds()).thenReturn(HashSet(userIds))

        assertThat(myFollowingsSyncer.call()).isFalse()

        verify(followingStorage, never()).insertFollowedUserIds(ArgumentMatchers.anyList())
    }

    @Test(expected = ApiRequestException::class)
    @Throws(Exception::class)
    fun pushFollowingsThrowsExceptionOn500() {
        whenever(followingStorage.hasStaleFollowings()).thenReturn(true)
        val user = UserFixtures.userBuilder().urn(USER_1).username(USERNAME_1).build()
        whenever(userStorage.loadUser(USER_1)).thenReturn(Maybe.just(user))
        whenever(followingStorage.loadStaleFollowings()).thenReturn(
                listOf(getNewFollowingAddition(USER_1))
        )

        mockApiFollowingAddition(USER_1, TestApiResponses.status(500))
        myFollowingsSyncer.call()
    }

    @Test
    @Throws(Exception::class)
    fun pushFollowingsRevertsAndNotifiesOnUnderage() {
        setupFailedPush(HttpStatus.BAD_REQUEST, Optional.of(AGE_RESTRICTED_BODY))
        val notification = Notification()
        whenever(notificationBuilder.buildUnderAgeNotification(USER_1, 19, USERNAME_1)).thenReturn(notification)

        myFollowingsSyncer.call()

        verify<NotificationManager>(notificationManger).notify(USER_1.toString(),
                                                               NotificationConstants.FOLLOW_BLOCKED_NOTIFICATION_ID,
                                                               notification)

        assertThat(followingStatusPublishSubject.hasObservers()).isTrue()
    }

    @Test
    @Throws(Exception::class)
    fun pushFollowingsRevertsAndNotifiesOnUnknownAge() {
        setupFailedPush(HttpStatus.BAD_REQUEST, Optional.of(AGE_UNKNOWN_BODY))
        val notification = Notification()
        whenever(notificationBuilder.buildUnknownAgeNotification(USER_1, USERNAME_1)).thenReturn(notification)

        myFollowingsSyncer.call()

        verify<NotificationManager>(notificationManger).notify(USER_1.toString(),
                                                               NotificationConstants.FOLLOW_BLOCKED_NOTIFICATION_ID,
                                                               notification)

        assertThat(followingStatusPublishSubject.hasObservers()).isTrue()
    }

    @Test
    @Throws(Exception::class)
    fun pushFollowingsRevertsAndNotifiesOnBlocked() {
        setupFailedPush(HttpStatus.BAD_REQUEST, Optional.of(BLOCKED_BODY))
        val notification = Notification()
        whenever(notificationBuilder.buildBlockedFollowNotification(USER_1, USERNAME_1)).thenReturn(notification)

        myFollowingsSyncer.call()

        verify<NotificationManager>(notificationManger).notify(USER_1.toString(),
                                                               NotificationConstants.FOLLOW_BLOCKED_NOTIFICATION_ID,
                                                               notification)

        assertThat(followingStatusPublishSubject.hasObservers()).isTrue()
    }

    @Test
    @Throws(Exception::class)
    fun pushFollowingsRevertsOnUnknownError() {
        setupFailedPush(HttpStatus.BAD_REQUEST, Optional.of(UNKNOWN_BODY))

        myFollowingsSyncer.call()

        assertThat(followingStatusPublishSubject.hasObservers()).isTrue()

        verify<NotificationManager>(notificationManger, never()).notify(anyString(),
                                                                        anyInt(),
                                                                        ArgumentMatchers.any(Notification::class.java))
    }

    @Test
    @Throws(Exception::class)
    fun pushFollowingsRevertsOnForbiddenError() {
        setupFailedPush(HttpStatus.FORBIDDEN)

        myFollowingsSyncer.call()

        assertThat(followingStatusPublishSubject.hasObservers()).isTrue()

        verify<NotificationManager>(notificationManger, never()).notify(anyString(),
                                                                        anyInt(),
                                                                        ArgumentMatchers.any(Notification::class.java))
    }

    @Test
    @Throws(Exception::class)
    fun pushFollowingsRevertsOnNotFoundError() {
        setupFailedPush(HttpStatus.NOT_FOUND)

        myFollowingsSyncer.call()

        assertThat(followingStatusPublishSubject.hasObservers()).isTrue()

        verify<NotificationManager>(notificationManger, never()).notify(anyString(),
                                                                        anyInt(),
                                                                        ArgumentMatchers.any(Notification::class.java))
    }

    @Throws(Exception::class)
    private fun setupFailedPush(status: Int, body: Optional<String> = Optional.absent()) {
        mockApiFollowingsResponse(emptyList())
        whenever(followingStorage.loadFollowedUserIds()).thenReturn(emptySet())
        whenever(followingStorage.hasStaleFollowings()).thenReturn(true)

        val user = UserFixtures.userBuilder().urn(USER_1).username(USERNAME_1).build()
        whenever(userStorage.loadUser(USER_1)).thenReturn(Maybe.just(user))
        whenever(followingStorage.loadStaleFollowings()).thenReturn(
                listOf(getNewFollowingAddition(USER_1))
        )

        followingStatusPublishSubject = CompletableSubject.create()
        whenever(followingOperations.toggleFollowing(USER_1, false)).thenReturn(followingStatusPublishSubject)

        if (body.isPresent) {
            mockApiFollowingAddition(USER_1, TestApiResponses.status(status, body.get()))
        } else {
            mockApiFollowingAddition(USER_1, TestApiResponses.status(status))
        }
    }

    private fun toUserIds(followings: List<ApiFollowing>): List<Urn> {
        return Lists.transform(followings, ApiFollowing.TO_USER_URNS)
    }

    private fun getApiFollowing(targetUrn: Urn): ApiFollowing {
        return ApiFollowing.create(Urn.forUser(123), Date(), targetUrn)
    }

    @Throws(Exception::class)
    private fun mockApiFollowingAddition(userUrn: Urn, apiResponse: ApiResponse) {
        whenever(apiClient.fetchResponse(argThat(isApiRequestTo("POST", ApiEndpoints.USER_FOLLOWS.path(userUrn)))))
                .thenReturn(apiResponse)
    }

    @Throws(Exception::class)
    private fun mockApiFollowingRemoval(userUrn: Urn, apiResponse: ApiResponse) {
        whenever(apiClient.fetchResponse(argThat(isApiRequestTo("DELETE", ApiEndpoints.USER_FOLLOWS.path(userUrn)))))
                .thenReturn(apiResponse)
    }

    @Throws(Exception::class)
    private fun mockApiFollowingsResponse(collection: List<ApiFollowing>) {
        whenever(apiClient.fetchMappedResponse(argThat(isApiRequestTo("GET", ApiEndpoints.MY_FOLLOWINGS.path())),
                                                         eq(typeToken))).thenReturn(ModelCollection(collection))
    }

    private fun getNewFollowingAddition(urn: Urn): Following {
        return Following(urn, -1L, Date(), null)
    }

    private fun getNewFollowingRemoval(urn: Urn): Following {
        return Following(urn, -1L, null, Date())
    }

    object typeToken : TypeToken<ModelCollection<ApiFollowing>>()
}
