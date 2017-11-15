package com.soundcloud.android.tracks

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.whenever
import com.soundcloud.android.associations.RepostStatuses
import com.soundcloud.android.associations.RepostsStateProvider
import com.soundcloud.android.likes.LikedStatuses
import com.soundcloud.android.likes.LikesStateProvider
import com.soundcloud.android.model.Urn
import com.soundcloud.android.offline.IOfflinePropertiesProvider
import com.soundcloud.android.offline.OfflineProperties
import com.soundcloud.android.offline.OfflineState
import com.soundcloud.android.playback.PlaySessionStateProvider
import com.soundcloud.android.presentation.EntityItemCreator
import com.soundcloud.android.testsupport.TrackFixtures
import io.reactivex.observers.TestObserver
import io.reactivex.subjects.BehaviorSubject
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class TrackItemRepositoryTest {
    @Mock private lateinit var trackRepository: TrackRepository
    @Mock private lateinit var entityItemCreator: EntityItemCreator
    @Mock private lateinit var likesStateProvider: LikesStateProvider
    @Mock private lateinit var repostsStateProvider: RepostsStateProvider
    @Mock private lateinit var playSessionStateProvider: PlaySessionStateProvider
    @Mock private lateinit var offlinePropertiesProvider: IOfflinePropertiesProvider

    private val urnsToTracks = BehaviorSubject.createDefault(mapOf<Urn, Track>())
    private val likedStatuses = BehaviorSubject.createDefault(LikedStatuses.create(setOf()))
    private val repostStatuses = BehaviorSubject.createDefault(RepostStatuses.create(setOf()))
    private val nowPlayingUrn = BehaviorSubject.createDefault(Urn.NOT_SET)
    private val offlineProperties = BehaviorSubject.createDefault(OfflineProperties(mapOf()))

    private val trackUrn = Urn.forTrack(123L)
    private val trackTitle = "title1"
    private val trackBuilder = TrackFixtures.trackBuilder().urn(trackUrn).userLike(false).userRepost(false).title(trackTitle)

    private lateinit var trackItemRepository: TrackItemRepository

    @Before
    fun setUp() {
        trackItemRepository = TrackItemRepository(trackRepository, entityItemCreator, likesStateProvider, repostsStateProvider, playSessionStateProvider, offlinePropertiesProvider)
        whenever(trackRepository.liveFromUrns(any())).thenReturn(urnsToTracks)
        whenever(likesStateProvider.likedStatuses()).thenReturn(likedStatuses)
        whenever(repostsStateProvider.repostedStatuses()).thenReturn(repostStatuses)
        whenever(playSessionStateProvider.nowPlayingUrn()).thenReturn(nowPlayingUrn)
        whenever(offlinePropertiesProvider.states()).thenReturn(offlineProperties)
    }

    @Test
    fun `liveFromUrns reemits track items on track change`() {
        val testObserver = emitAndCheckInitialLiveTracksFromUrns()

        val title2 = "title2"
        val changedTrack = trackBuilder.title(title2).build()
        urnsToTracks.onNext(mapOf(trackUrn to changedTrack))

        testObserver.apply {
            assertValueCount(2)
            assertValueAt(1) { it.containsKey(trackUrn) }
            assertValueAt(1) { it[trackUrn]?.title() == title2 }
        }
    }

    @Test
    fun `liveFromUrns reemits track items on like status change`() {
        val testObserver = emitAndCheckInitialLiveTracksFromUrns()

        likedStatuses.onNext(LikedStatuses.create(setOf(trackUrn)))

        testObserver.apply {
            assertValueCount(2)
            assertValueAt(1) { it.containsKey(trackUrn) }
            assertValueAt(1) { it[trackUrn]?.isUserLike == true }
        }
    }

    @Test
    fun `liveFromUrns reemits track items on repost status change`() {
        val testObserver = emitAndCheckInitialLiveTracksFromUrns()

        repostStatuses.onNext(RepostStatuses.create(setOf(trackUrn)))

        testObserver.apply {
            assertValueCount(2)
            assertValueAt(1) { it.containsKey(trackUrn) }
            assertValueAt(1) { it[trackUrn]?.isUserRepost == true }
        }
    }

    @Test
    fun `liveFromUrns reemits track items on now playling change`() {
        val testObserver = emitAndCheckInitialLiveTracksFromUrns()

        nowPlayingUrn.onNext(trackUrn)

        testObserver.apply {
            assertValueCount(2)
            assertValueAt(1) { it.containsKey(trackUrn) }
            assertValueAt(1) { it[trackUrn]?.isPlaying == true }
        }
    }

    @Test
    fun `liveFromUrns reemits track items on offline state change`() {
        val testObserver = emitAndCheckInitialLiveTracksFromUrns()

        offlineProperties.onNext(OfflineProperties(offlineEntitiesStates = mapOf(trackUrn to OfflineState.DOWNLOADED)))

        testObserver.apply {
            assertValueCount(2)
            assertValueAt(1) { it.containsKey(trackUrn) }
            assertValueAt(1) { it[trackUrn]?.offlineState() == OfflineState.DOWNLOADED }
        }
    }

    private fun emitAndCheckInitialLiveTracksFromUrns(): TestObserver<Map<Urn, TrackItem>> {
        val track = trackBuilder.build()
        urnsToTracks.onNext(mapOf(trackUrn to track))

        val testObserver = trackItemRepository.liveFromUrns(listOf(trackUrn)).test()
        testObserver.apply {
            assertValueCount(1)
            assertValueAt(0) { it.containsKey(trackUrn) }
            assertValueAt(0) { it[trackUrn]?.title() == trackTitle }
            assertValueAt(0) { it[trackUrn]?.isUserLike == false }
            assertValueAt(0) { it[trackUrn]?.isUserRepost == false }
            assertValueAt(0) { it[trackUrn]?.isPlaying == false }
            assertValueAt(0) { it[trackUrn]?.offlineState() == OfflineState.NOT_OFFLINE }
        }
        return testObserver
    }

    @Test
    fun `liveTrack reemits track item on track change`() {
        val testObserver = emitAndCheckInitialLiveTrack()

        val title2 = "title2"
        val changedTrack = trackBuilder.title(title2).build()
        urnsToTracks.onNext(mapOf(trackUrn to changedTrack))

        testObserver.apply {
            assertValueCount(2)
            assertValueAt(1) { it.urn == trackUrn }
            assertValueAt(1) { it.title() == title2 }
        }
    }

    @Test
    fun `liveTrack reemits track item on like status change`() {
        val testObserver = emitAndCheckInitialLiveTrack()

        likedStatuses.onNext(LikedStatuses.create(setOf(trackUrn)))

        testObserver.apply {
            assertValueCount(2)
            assertValueAt(1) { it.urn == trackUrn }
            assertValueAt(1) { it.isUserLike }
        }
    }

    @Test
    fun `liveTrack reemits track item on repost status change`() {
        val testObserver = emitAndCheckInitialLiveTrack()

        repostStatuses.onNext(RepostStatuses.create(setOf(trackUrn)))

        testObserver.apply {
            assertValueCount(2)
            assertValueAt(1) { it.urn == trackUrn }
            assertValueAt(1) { it.isUserRepost }
        }
    }

    @Test
    fun `liveTrack reemits track item on now playling change`() {
        val testObserver = emitAndCheckInitialLiveTrack()

        nowPlayingUrn.onNext(trackUrn)

        testObserver.apply {
            assertValueCount(2)
            assertValueAt(1) { it.urn == trackUrn }
            assertValueAt(1) { it.isPlaying }
        }
    }

    @Test
    fun `liveTrack reemits track item on offline state change`() {
        val testObserver = emitAndCheckInitialLiveTrack()

        offlineProperties.onNext(OfflineProperties(offlineEntitiesStates = mapOf(trackUrn to OfflineState.DOWNLOADED)))

        testObserver.apply {
            assertValueCount(2)
            assertValueAt(1) { it.urn == trackUrn }
            assertValueAt(1) { it.offlineState() == OfflineState.DOWNLOADED }
        }
    }

    private fun emitAndCheckInitialLiveTrack(): TestObserver<TrackItem> {
        val track = trackBuilder.build()
        urnsToTracks.onNext(mapOf(trackUrn to track))

        val testObserver = trackItemRepository.liveTrack(trackUrn).test()
        testObserver.apply {
            assertValueCount(1)
            assertValueAt(0) { it.urn == trackUrn }
            assertValueAt(0) { it.title() == trackTitle }
            assertValueAt(0) { !it.isUserLike }
            assertValueAt(0) { !it.isUserRepost }
            assertValueAt(0) { !it.isPlaying }
            assertValueAt(0) { it.offlineState() == OfflineState.NOT_OFFLINE }
        }
        return testObserver
    }
}
