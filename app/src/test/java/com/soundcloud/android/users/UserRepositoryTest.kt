package com.soundcloud.android.users

import com.nhaarman.mockito_kotlin.whenever
import com.soundcloud.android.events.EventQueue
import com.soundcloud.android.model.Urn
import com.soundcloud.android.storage.RepositoryMissedSyncEvent
import com.soundcloud.android.sync.SyncInitiator
import com.soundcloud.android.sync.SyncJobResult
import com.soundcloud.android.sync.Syncable
import com.soundcloud.android.testsupport.fixtures.ModelFixtures
import com.soundcloud.rx.eventbus.TestEventBusV2
import io.reactivex.Maybe
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.SingleSubject
import org.assertj.core.api.Java6Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyZeroInteractions
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class UserRepositoryTest {

    private val userUrn = Urn.forUser(123L)
    private val user = ModelFixtures.userBuilder(false)
            .urn(Urn.forUser(123L)).build()

    private val updatedUser = ModelFixtures.userBuilder(false)
            .urn(Urn.forUser(123L))
            .username("updated-name")
            .build()

    private val otherUserUrn = Urn.forUser(124L)
    private val otherUser = ModelFixtures.userBuilder(false)
            .urn(Urn.forUser(124L))
            .build();

    @Mock private lateinit var userStorage: UserStorage
    @Mock private lateinit var syncInitiator: SyncInitiator

    private lateinit var userRepository: UserRepository
    private val eventBus = TestEventBusV2()

    @Before
    fun setup() {
        userRepository = UserRepository(userStorage, syncInitiator, Schedulers.trampoline(), eventBus)
    }

    @Test
    fun localUserInfoReturnsUserInfoFromStorage() {
        whenever(userStorage.loadUser(userUrn)).thenReturn(Maybe.just(updatedUser))

        userRepository.localUserInfo(userUrn)
                .test()
                .assertValue(updatedUser)

        verifyZeroInteractions(syncInitiator)
    }

    @Test
    fun userInfoReturnsUserInfoFromStorage() {
        whenever(userStorage.loadUser(userUrn)).thenReturn(Maybe.just(user))
        whenever(syncInitiator.syncUser(userUrn)).thenReturn(Single.never())

        userRepository.userInfo(userUrn)
                .test()
                .assertValue(user)
        eventBus.verifyNoEventsOn(EventQueue.TRACKING)
    }

    @Test
    fun userInfoReturnsUserInfoFromSyncerIfStorageEmpty() {
        val subject = SingleSubject.create<SyncJobResult>()
        whenever(userStorage.loadUser(userUrn)).thenReturn(Maybe.empty(), Maybe.just(updatedUser))
        whenever(syncInitiator.syncUser(userUrn)).thenReturn(subject)

        val testObserver = userRepository.userInfo(userUrn)
                .test()
                .assertNoValues()

        subject.onSuccess(SyncJobResult.success(Syncable.USERS.name, true))

        testObserver.assertValue(updatedUser)
    }

    @Test
    fun userInfoEmptySendsTrackingEvent() {
        val subject = SingleSubject.create<SyncJobResult>()
        whenever(userStorage.loadUser(userUrn)).thenReturn(Maybe.empty())
        whenever(syncInitiator.syncUser(userUrn)).thenReturn(subject)
        subject.onSuccess(SyncJobResult.success(Syncable.USERS.name, true))

        userRepository.userInfo(userUrn)
                .test()
                .assertNoValues()

        val trackingEvent = eventBus.lastEventOn(EventQueue.TRACKING)
        assertThat((trackingEvent as RepositoryMissedSyncEvent).usersMissing).isEqualTo(1)
    }

    @Test
    fun syncedUserInfoReturnsUserInfoFromStorageAfterSync() {
        val subject = SingleSubject.create<SyncJobResult>()
        whenever(userStorage.loadUser(userUrn)).thenReturn(Maybe.just(updatedUser))
        whenever(syncInitiator.syncUser(userUrn)).thenReturn(subject)

        val testObserver = userRepository.syncedUserInfo(userUrn).test()
                .assertNoValues()

        subject.onSuccess(SyncJobResult.success(Syncable.USERS.name, true))

        testObserver.assertValue(updatedUser)
        eventBus.verifyNoEventsOn(EventQueue.TRACKING)
    }

    @Test
    fun localAndSyncedUserInfoReturnsUserInfoFromStorage() {
        whenever(syncInitiator.syncUser(userUrn)).thenReturn(Single.never())
        whenever(userStorage.loadUser(userUrn)).thenReturn(Maybe.just(user))

        userRepository.localAndSyncedUserInfo(userUrn)
                .test()
                .assertValue(user)
    }

    @Test
    fun localAndSyncedUserInfoReturnsUserInfoAgainFromStorageAfterSync() {
        val subject = SingleSubject.create<SyncJobResult>()
        whenever(userStorage.loadUser(userUrn)).thenReturn(Maybe.just(user), Maybe.just(updatedUser))
        whenever(syncInitiator.syncUser(userUrn)).thenReturn(subject)

        val testObserver = userRepository.localAndSyncedUserInfo(userUrn)
                .test()
                .assertValue(user)

        subject.onSuccess(SyncJobResult.success(Syncable.USERS.name, true))

        testObserver.assertValues(user, updatedUser)
    }

    @Test
    fun localAndSyncedUserInfoReturnsDoesNotEmitMissingUser() {
        val subject = SingleSubject.create<SyncJobResult>()
        whenever(userStorage.loadUser(userUrn)).thenReturn(Maybe.empty(), Maybe.just(updatedUser))
        whenever(syncInitiator.syncUser(userUrn)).thenReturn(subject)

        val testObserver = userRepository.localAndSyncedUserInfo(userUrn)
                .test()
                .assertNoValues()

        subject.onSuccess(SyncJobResult.success(Syncable.USERS.name, true))

        testObserver.assertValue(updatedUser)
    }

    @Test
    fun loadMultipleUsersLocallyDoesNotTriggerSyncIfAllAreFound() {
        val urns = listOf(userUrn, otherUserUrn)
        val users = listOf(user, otherUser)

        whenever(userStorage.loadUsers(urns)).thenReturn(Single.just(users))

        userRepository.usersInfo(urns)
                .test()
                .assertValue(users)

        verifyZeroInteractions(syncInitiator)
    }

    @Test
    fun loadMultipleUsersSyncsTheMissingLocally() {
        val urns = listOf(userUrn, otherUserUrn)
        val users = listOf(user, otherUser)

        whenever(userStorage.loadUsers(urns)).thenReturn(Single.just(listOf(user)), Single.just(users))

        val syncJobResultObservable = Single.just(SyncJobResult.success(Syncable.USERS.name, true))
        whenever(syncInitiator.batchSyncUsers(ArgumentMatchers.anyList())).thenReturn(syncJobResultObservable)

        userRepository.usersInfo(urns)
                .test()
                .assertValue(users)

        verify(syncInitiator).batchSyncUsers(listOf(otherUserUrn))
    }

}
