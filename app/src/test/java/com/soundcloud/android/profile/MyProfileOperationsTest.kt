package com.soundcloud.android.profile

import com.nhaarman.mockito_kotlin.whenever
import com.soundcloud.android.model.Urn
import com.soundcloud.android.sync.SyncInitiatorBridge
import com.soundcloud.android.testsupport.fixtures.ModelFixtures
import com.soundcloud.android.testsupport.fixtures.TestSyncJobResults
import com.soundcloud.android.tracks.Track
import com.soundcloud.android.tracks.TrackRepository
import com.soundcloud.android.users.UserAssociation
import com.soundcloud.android.users.UserAssociationStorage
import com.soundcloud.java.optional.Optional
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.junit.MockitoJUnitRunner
import rx.observers.TestSubscriber
import java.util.Arrays
import java.util.Date

@RunWith(MockitoJUnitRunner::class)
class MyProfileOperationsTest {
    private lateinit var operations: MyProfileOperations

    @Mock private lateinit var postStorage: PostsStorage
    @Mock private lateinit var syncInitiatorBridge: SyncInitiatorBridge
    @Mock private lateinit var userAssociationStorage: UserAssociationStorage
    @Mock private lateinit var trackRepository: TrackRepository

    private val scheduler = Schedulers.trampoline()
    private lateinit var subscriber: TestSubscriber<List<Following>>

    @Before
    @Throws(Exception::class)
    fun setUp() {
        operations = MyProfileOperations(
                postStorage,
                syncInitiatorBridge,
                userAssociationStorage,
                scheduler,
                trackRepository)

        subscriber = TestSubscriber()
    }

    @Test
    fun shouldLoadLastPublicPostedTrack() {
        val urn1 = Urn.forTrack(1L)
        val urn2 = Urn.forTrack(2L)

        val date1 = Date(100)
        val date2 = Date(300)

        val tracks = listOf(urn2, urn1)

        val otherTrack = trackFromUrn(urn1)
        val lastPostedPublicTrack = trackFromUrn(urn2)

        whenever(trackRepository.fromUrns(tracks)).thenReturn(Single.just(mapOf<Urn, Track>(urn2 to lastPostedPublicTrack, urn1 to otherTrack)))

        whenever(postStorage.loadPostedTracksSortedByDateDesc()).thenReturn(Single.just(listOf(urn2 to date2, urn1 to date1)))

        operations.lastPublicPostedTrack()
                .test()
                .assertValue { it.createdAt() == date2 }
                .assertValue { it.urn() == urn2 }
                .assertValue { it.permalinkUrl() == lastPostedPublicTrack.permalinkUrl() }
    }

    @Test
    fun shouldIgnorePrivateTrack() {
        val urn1 = Urn.forTrack(1L)
        val urn2 = Urn.forTrack(2L)

        val date1 = Date(100)
        val date2 = Date(300)

        val tracks = listOf(urn2, urn1)

        val publicTrack = trackFromUrn(urn1)
        val privateTrack = trackFromUrn(urn2, private = true)

        whenever(trackRepository.fromUrns(tracks)).thenReturn(Single.just(mapOf<Urn, Track>(urn2 to privateTrack, urn1 to publicTrack)))

        whenever(postStorage.loadPostedTracksSortedByDateDesc()).thenReturn(Single.just(listOf(urn2 to date2, urn1 to date1)))

        operations.lastPublicPostedTrack()
                .test()
                .assertValue { it.createdAt() == date1 }
                .assertValue { it.urn() == urn1 }
                .assertValue { it.permalinkUrl() == publicTrack.permalinkUrl() }
    }

    @Test
    fun shouldIgnoreTrackMissingInRepo() {
        val urn1 = Urn.forTrack(1L)
        val urn2 = Urn.forTrack(2L)

        val date1 = Date(100)
        val date2 = Date(300)

        val tracks = listOf(urn2, urn1)

        val availableTrack = trackFromUrn(urn1)

        whenever(trackRepository.fromUrns(tracks)).thenReturn(Single.just(mapOf<Urn, Track>(urn1 to availableTrack)))

        whenever(postStorage.loadPostedTracksSortedByDateDesc()).thenReturn(Single.just(listOf(urn2 to date2, urn1 to date1)))

        operations.lastPublicPostedTrack()
                .test()
                .assertValue { it.createdAt() == date1 }
                .assertValue { it.urn() == urn1 }
                .assertValue { it.permalinkUrl() == availableTrack.permalinkUrl() }
    }

    private fun trackFromUrn(urn: Urn, private: Boolean = false) = ModelFixtures.trackBuilder().urn(urn).isPrivate(private).build()

    @Test
    fun returnsListOfFollowingsUrns() {
        val userAssociation1 = createUserAssociation(Urn.forUser(123L))
        val userAssociation2 = createUserAssociation(Urn.forUser(124L))
        val followingsUrn = Arrays.asList(userAssociation1, userAssociation2)

        whenever(userAssociationStorage.followedUserAssociations()).thenReturn(Single.just(
                followingsUrn))

        operations.followingsUserAssociations().test().assertComplete().assertValue(followingsUrn)

        verify<SyncInitiatorBridge>(syncInitiatorBridge, never()).refreshFollowings()
    }

    @Test
    fun syncsWhenStoredFollowingsListEmpty() {
        whenever(userAssociationStorage.followedUserAssociations()).thenReturn(Single.just(emptyList<UserAssociation>()))
        whenever(syncInitiatorBridge.refreshFollowings()).thenReturn(Single.just(TestSyncJobResults.successWithChange()))

        operations.followingsUserAssociations().test().assertComplete().assertValue(emptyList())

        verify(syncInitiatorBridge).refreshFollowings()
        verify(userAssociationStorage, times(2)).followedUserAssociations()
    }

    private fun createUserAssociation(urn: Urn): UserAssociation = UserAssociation.create(urn, 0, 1, Optional.absent(), Optional.absent())
}
