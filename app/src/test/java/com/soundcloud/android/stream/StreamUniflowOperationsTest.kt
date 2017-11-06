package com.soundcloud.android.stream

import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import com.soundcloud.android.Consts
import com.soundcloud.android.ads.StreamAdsController
import com.soundcloud.android.facebookinvites.FacebookInvitesOperations
import com.soundcloud.android.sync.NewSyncOperations
import com.soundcloud.android.sync.SyncResult
import com.soundcloud.android.sync.Syncable
import com.soundcloud.android.testsupport.fixtures.ModelFixtures
import com.soundcloud.android.testsupport.fixtures.PlayableFixtures
import com.soundcloud.android.upsell.InlineUpsellOperations
import com.soundcloud.java.optional.Optional
import com.soundcloud.java.strings.Strings
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import java.util.Date

@RunWith(MockitoJUnitRunner::class)
class StreamUniflowOperationsTest {

    private lateinit var streamOperations: StreamUniflowOperations

    @Mock private lateinit var streamStorage: StreamStorage
    @Mock private lateinit var removeStalePromotedItemsCommand: RemoveStalePromotedItemsCommand
    @Mock private lateinit var streamAdsController: StreamAdsController
    @Mock private lateinit var upsellOperations: InlineUpsellOperations
    @Mock private lateinit var streamEntityToItemTransformer: StreamEntityToItemTransformer
    @Mock private lateinit var syncOperations: NewSyncOperations
    @Mock private lateinit var facebookInvitesOperations: FacebookInvitesOperations

    @Before
    fun setUp() {
        whenever(removeStalePromotedItemsCommand.toCompletable()).thenReturn(Completable.complete())
        whenever(syncOperations.lazySyncIfStale(Syncable.SOUNDSTREAM)).thenReturn(Single.just(SyncResult.synced()))
        whenever(syncOperations.failSafeSync(Syncable.SOUNDSTREAM)).thenReturn(Single.just(SyncResult.synced()))
        streamOperations = StreamUniflowOperations(streamStorage,
                                                   syncOperations,
                                                   removeStalePromotedItemsCommand,
                                                   streamEntityToItemTransformer,
                                                   upsellOperations,
                                                   streamAdsController,
                                                   facebookInvitesOperations,
                                                   Schedulers.trampoline())
    }

    @Test
    fun `loads stream items from storage`() {
        val tracks = initStorage()

        val testObserver = streamOperations.initialStreamItems().test()

        testObserver.assertValue(tracks)
    }

    @Test
    fun `removes promoted items and syncs`() {
        var promotedItemsRemoved = false
        var streamItemsSynced = false
        whenever(removeStalePromotedItemsCommand.toCompletable()).thenReturn(Completable.complete().doOnComplete { promotedItemsRemoved = true })
        whenever(syncOperations.lazySyncIfStale(Syncable.SOUNDSTREAM)).thenReturn(Single.just(SyncResult.synced()).doOnSuccess { streamItemsSynced = true })
        initStorage()

        streamOperations.initialStreamItems().test()

        assertThat(promotedItemsRemoved).isTrue()
        assertThat(streamItemsSynced).isTrue()
    }

    @Test
    fun `inserts ads after stream is loaded`() {
        initStorage()

        streamOperations.initialStreamItems().test()

        verify(streamAdsController).insertAds()
    }

    @Test
    fun `adds upsellable item`() {
        val tracks = initStorageWithHighTierTrack()
        whenever(upsellOperations.shouldDisplayInStream()).thenReturn(true)

        val testObserver = streamOperations.initialStreamItems().test()

        testObserver.assertValue { it[0] == tracks[0] }
        testObserver.assertValue { it[1] == StreamItem.Upsell }
    }

    @Test
    fun `loads updated stream items from storage`() {
        val tracks = initStorage()

        val testObserver = streamOperations.updatedStreamItems().test()

        testObserver.assertValue(tracks)
    }

    @Test
    fun `inserts ads after stream is updated`() {
        initStorage()

        streamOperations.updatedStreamItems().test()

        verify(streamAdsController).insertAds()
    }

    @Test
    fun `empty next page if last item empty`() {
        val nextPageItems = streamOperations.nextPageItems(emptyList())

        assertThat(nextPageItems).isNull()
    }

    @Test
    fun `lazy syncs and returns next page`() {
        val createdAt = Date(500L)
        val streamEntity = PlayableFixtures.timelineItem(createdAt)
        val tracks = listOf(ModelFixtures.trackFromStreamEntity(streamEntity))
        val nextPageStreamEntity = PlayableFixtures.timelineItem(Date(300L))
        val nextPageTracks = listOf(ModelFixtures.trackFromStreamEntity(nextPageStreamEntity))
        initStorage(listOf(streamEntity), tracks)

        whenever(streamStorage.timelineItemsBefore(createdAt.time, Consts.LIST_PAGE_SIZE)).thenReturn(Observable.just(nextPageStreamEntity))
        whenever(streamEntityToItemTransformer.apply(listOf(nextPageStreamEntity))).thenReturn(Single.just(nextPageTracks))

        val nextPageItems = streamOperations.nextPageItems(tracks)?.test()
        assertThat(nextPageItems).isNotNull()
        nextPageItems?.apply {
            this.assertValue(nextPageTracks)
        }
    }

    @Test
    fun `initial notification item is facebook creator invites`() {
        val track = PlayableFixtures.expectedLastPostedTrackForPostsScreen()
        whenever(facebookInvitesOperations.creatorInvites()).thenReturn(Maybe.just(StreamItem.FacebookCreatorInvites(track.urn(), Strings.EMPTY)))
        whenever(facebookInvitesOperations.listenerInvites()).thenReturn(Maybe.empty())

        streamOperations.initialNotificationItem().test().apply {
            assertValueCount(1)
            assertValue { it is StreamItem.FacebookCreatorInvites }
            assertValue { (it as StreamItem.FacebookCreatorInvites).trackUrn == track.urn() }
        }
    }

    @Test
    fun `initial notification item is facebook listener invites if creator item is missing`() {
        whenever(facebookInvitesOperations.creatorInvites()).thenReturn(Maybe.empty())
        whenever(facebookInvitesOperations.listenerInvites()).thenReturn(Maybe.just(StreamItem.FacebookListenerInvites(Optional.absent())))

        streamOperations.initialNotificationItem().test().apply {
            assertValueCount(1)
            assertValue { it is StreamItem.FacebookListenerInvites }
        }
    }

    @Test
    fun `initial notification item is absent when both creator and listener items are missing`() {
        whenever(facebookInvitesOperations.creatorInvites()).thenReturn(Maybe.empty())
        whenever(facebookInvitesOperations.listenerInvites()).thenReturn(Maybe.empty())

        streamOperations.initialNotificationItem().test().assertValueCount(0)
    }

    private fun initStorage(): List<StreamItem> {
        val streamEntity = PlayableFixtures.timelineItem(Date(123L))
        val tracks = listOf(ModelFixtures.trackFromStreamEntity(streamEntity))
        initStorage(listOf(streamEntity), tracks)
        return tracks
    }

    private fun initStorageWithHighTierTrack(): List<StreamItem> {
        val streamEntity = PlayableFixtures.timelineItem(Date(123L))
        val tracks = listOf(ModelFixtures.highTierPreviewTrackFromStreamEntity(streamEntity))
        initStorage(listOf(streamEntity), tracks)
        return tracks
    }

    private fun initStorage(streamEntitities: List<StreamEntity>, streamItems: List<StreamItem>) {
        whenever(streamStorage.timelineItems(Consts.LIST_PAGE_SIZE)).thenReturn(Observable.fromIterable(streamEntitities))
        whenever(streamEntityToItemTransformer.apply(streamEntitities)).thenReturn(Single.just(streamItems))
    }
}
