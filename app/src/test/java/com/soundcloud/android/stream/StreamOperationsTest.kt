package com.soundcloud.android.stream

import com.nhaarman.mockito_kotlin.whenever
import com.soundcloud.android.ads.StreamAdsController
import com.soundcloud.android.events.EventQueue
import com.soundcloud.android.events.PromotedTrackingEvent
import com.soundcloud.android.facebookinvites.FacebookInvitesOperations
import com.soundcloud.android.model.Urn
import com.soundcloud.android.playback.PlayableWithReposter
import com.soundcloud.android.stream.StreamItem.Kind
import com.soundcloud.android.suggestedcreators.SuggestedCreator
import com.soundcloud.android.suggestedcreators.SuggestedCreatorsOperations
import com.soundcloud.android.sync.SyncInitiator
import com.soundcloud.android.sync.SyncStateStorage
import com.soundcloud.android.sync.Syncable
import com.soundcloud.android.sync.timeline.TimelineOperations
import com.soundcloud.android.sync.timeline.TimelineOperationsTest
import com.soundcloud.android.testsupport.TrackFixtures
import com.soundcloud.android.testsupport.UserFixtures
import com.soundcloud.android.testsupport.fixtures.ModelFixtures
import com.soundcloud.android.testsupport.fixtures.PlayableFixtures
import com.soundcloud.android.testsupport.fixtures.TestSyncJobResults.successWithChange
import com.soundcloud.android.tracks.TrackItem
import com.soundcloud.android.upsell.InlineUpsellOperations
import com.soundcloud.java.collections.Lists
import com.soundcloud.java.optional.Optional
import com.soundcloud.rx.eventbus.TestEventBusV2
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.Observable.just
import io.reactivex.Scheduler
import io.reactivex.Single
import io.reactivex.SingleObserver
import io.reactivex.observers.TestObserver
import org.assertj.core.api.Java6Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mock
import org.mockito.Mockito.inOrder
import org.mockito.Mockito.never
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import rx.subjects.PublishSubject
import java.util.ArrayList
import java.util.Collections.emptyList
import java.util.Date

class StreamOperationsTest : TimelineOperationsTest<StreamEntity, StreamItem, StreamStorage>() {

    private val suggestedCreatorsItem = StreamItem.SuggestedCreators.create(Lists.newArrayList<SuggestedCreator>())
    private lateinit var streamOperations: StreamOperations

    @Mock private lateinit var streamStorage: StreamStorage
    @Mock private lateinit var observer: SingleObserver<List<StreamItem>>
    @Mock private lateinit var removeStalePromotedItemsCommand: RemoveStalePromotedItemsCommand
    @Mock private lateinit var markPromotedItemAsStaleCommand: MarkPromotedItemAsStaleCommand
    @Mock private lateinit var facebookInvitesOperations: FacebookInvitesOperations
    @Mock private lateinit var streamAdsController: StreamAdsController
    @Mock private lateinit var upsellOperations: InlineUpsellOperations
    @Mock private lateinit var suggestedCreatorsOperations: SuggestedCreatorsOperations
    @Mock private lateinit var streamEntityToItemTransformer: StreamEntityToItemTransformer

    private val eventBus = TestEventBusV2()

    private val promoter = UserFixtures.user()
    private val promotedTrack = TrackFixtures.track()
    private val promotedTrackItem = ModelFixtures.promotedTrackItem(promotedTrack, promoter)
    private val promotedStreamTrack = fromPromotedTrackItem(Date(), promotedTrackItem)
    private val upsellableTrack = PlayableFixtures.upsellableTrack()
    private val upsellableStreamTrack = builderFromTrackItem(Date(), upsellableTrack).build()

    @Before
    @Throws(Exception::class)
    fun setUp() {
        whenever(removeStalePromotedItemsCommand.toSingle()).thenReturn(Single.just(emptyList()))
        whenever(facebookInvitesOperations.creatorInvites()).thenReturn(Maybe.empty())
        whenever(facebookInvitesOperations.listenerInvites()).thenReturn(Maybe.empty())
        whenever(suggestedCreatorsOperations.suggestedCreators()).thenReturn(Maybe.empty())
        whenever(streamEntityToItemTransformer.apply(eq(emptyList()))).thenReturn(Single.just(emptyList()))
        this.streamOperations = super.operations as StreamOperations
    }

    override fun buildOperations(storage: StreamStorage,
                                 syncInitiator: SyncInitiator,
                                 scheduler: Scheduler,
                                 syncStateStorage: SyncStateStorage): TimelineOperations<StreamEntity, StreamItem> {
        return StreamOperations(storage, syncInitiator, removeStalePromotedItemsCommand,
                                markPromotedItemAsStaleCommand, eventBus, scheduler, facebookInvitesOperations,
                                streamAdsController, upsellOperations, syncStateStorage,
                                suggestedCreatorsOperations, streamEntityToItemTransformer)
    }

    override fun provideStorageMock(): StreamStorage? {
        return streamStorage
    }

    override fun provideSyncable(): Syncable {
        return Syncable.SOUNDSTREAM
    }

    @Test
    fun streamIsConsideredEmptyWhenOnlyPromotedTrackIsReturnedAndDoesNotSyncAgain() {
        // 1st page comes back blank first, then includes promoted promotedTrack only
        initWithTrack()
        // returning true means successful sync
        whenever(syncInitiator.sync(Syncable.SOUNDSTREAM)).thenReturn(Single.just(successWithChange()))

        streamOperations.initialStreamItems().subscribe(observer)

        val inOrder = inOrder(this.observer, streamStorage, syncInitiator)
        inOrder.verify(streamStorage).timelineItems(TimelineOperationsTest.PAGE_SIZE)
        inOrder.verify(syncInitiator).sync(Syncable.SOUNDSTREAM)
        inOrder.verify(streamStorage).timelineItems(TimelineOperationsTest.PAGE_SIZE)
        inOrder.verify(this.observer).onSuccess(emptyList())
        inOrder.verifyNoMoreInteractions()
    }

    @Test
    fun showsSuggestedCreatorsWhenOnlyPromotedTrackIsReturned() {
        initWithTrack()
        whenever(syncInitiator.sync(Syncable.SOUNDSTREAM)).thenReturn(Single.just(successWithChange()))

        initSuggestedCreatorsItem()

        assertInitialStreamFirstItemKind(Kind.SUGGESTED_CREATORS)
    }

    @Test
    fun showsSuggestedCreatorsWhenNoTracksAreReturned() {
        whenever(streamStorage.timelineItems(TimelineOperationsTest.PAGE_SIZE))
                .thenReturn(Observable.empty())
                .thenReturn(Observable.empty())
        whenever(syncInitiator.sync(Syncable.SOUNDSTREAM)).thenReturn(Single.just(successWithChange()))

        initSuggestedCreatorsItem()

        assertInitialStreamFirstItemKind(Kind.SUGGESTED_CREATORS)
    }

    @Test
    fun showsSuggestedCreatorsInsteadOfOtherNotificationItems() {
        val items = createItems(TimelineOperationsTest.PAGE_SIZE, 123L)
        initWithStorageModel(items)

        initSuggestedCreatorsItem()
        initFacebookCreatorsItem()
        initFacebookListenersItem()

        assertInitialStreamFirstItemKind(Kind.SUGGESTED_CREATORS)
    }

    @Test
    fun showsSuggestedCreatorsInUpdatedItems() {
        initUpdatedTimelineItems()

        initSuggestedCreatorsItem()

        val subscriber = streamOperations.updatedStreamItems().test()

        subscriber.assertValueCount(1)
        val streamItem = subscriber.values().get(0).get(0)
        assertThat(streamItem.kind).isEqualTo(Kind.SUGGESTED_CREATORS)
    }

    @Test
    fun initialStreamDeletesStalePromotedTracksBeforeLoadingStreamItems() {
        val subject = PublishSubject.create<List<Long>>()
        whenever(removeStalePromotedItemsCommand.toObservable(null)).thenReturn(subject)
        whenever(streamStorage.timelineItems(TimelineOperationsTest.PAGE_SIZE)).thenReturn(Observable.create { assertThat(subject.hasObservers()).isTrue() })

        streamOperations.initialStreamItems().subscribe(observer)
        subject.onNext(emptyList())

        verify(streamStorage).timelineItems(TimelineOperationsTest.PAGE_SIZE)
    }

    @Test
    fun publishPromotedImpressionDoesNothingIfThereAreNoPromotedItems() {
        streamOperations.publishPromotedImpression(emptyList<StreamItem>())

        eventBus.verifyNoEventsOn(EventQueue.TRACKING)
        verify(markPromotedItemAsStaleCommand, never()).toSingle(any())
    }

    @Test
    fun publishPromotedImpressionFiresImpressionsOnlyOncePerPromotedItem() {
        whenever(markPromotedItemAsStaleCommand.toSingle("ad:urn:123")).thenReturn(Single.never())
        val itemsWithPromoted = createItems(TimelineOperationsTest.PAGE_SIZE, 123L)
        itemsWithPromoted.add(0, promotedStreamTrack)
        initWithStorageModelOnRepeatAfterSyncing(itemsWithPromoted)
        whenever(syncInitiator.sync(Syncable.SOUNDSTREAM)).thenReturn(Single.just(successWithChange()))
        val subscriber = subscribeToInitialStream()
        val streamItems = subscriber.values()[0]

        //Call impression twice on same stream items
        streamOperations.publishPromotedImpression(streamItems)
        streamOperations.publishPromotedImpression(streamItems)

        assertThat(eventBus.eventsOn(EventQueue.TRACKING).size).isEqualTo(1)
        assertThat(eventBus.lastEventOn(EventQueue.TRACKING)).isInstanceOf(PromotedTrackingEvent::class.java)
        verify(markPromotedItemAsStaleCommand, times(1)).toSingle("ad:urn:123")
    }

    @Test
    fun shouldShowFacebookListenerInvitesAsFirstItem() {
        val items = createItems(TimelineOperationsTest.PAGE_SIZE, 123L)
        initWithStorageModel(items)
        initFacebookListenersItem()

        assertInitialStreamFirstItemKind(Kind.FACEBOOK_LISTENER_INVITES)
    }

    @Test
    fun shouldShowFacebookCreatorInvitesAsFirstItem() {
        val items = createItems(TimelineOperationsTest.PAGE_SIZE, 123L)
        initWithStorageModel(items)
        initFacebookListenersItem()
        initFacebookCreatorsItem()

        assertInitialStreamFirstItemKind(Kind.FACEBOOK_CREATORS)
    }

    @Test
    fun shouldNotShowFacebookInvitesOnEmptyStream() {
        initFacebookListenersItem()
        whenever(streamStorage.timelineItems(TimelineOperationsTest.PAGE_SIZE))
                .thenReturn(Observable.empty())
                .thenReturn(Observable.empty())
        whenever(syncInitiator.sync(Syncable.SOUNDSTREAM)).thenReturn(Single.just(successWithChange()))

        assertInitialStreamEmpty()
    }

    @Test
    fun shouldNotShowFacebookInvitesOnPromotedOnlyStream() {
        initFacebookListenersItem()
        initWithTrack()
        whenever(syncInitiator.sync(Syncable.SOUNDSTREAM)).thenReturn(Single.just(successWithChange()))

        assertInitialStreamEmpty()
    }

    @Test
    fun shouldShowFacebookInvitesAbovePromotedItems() {
        val itemsWithPromoted = createItems(TimelineOperationsTest.PAGE_SIZE, 123L)
        itemsWithPromoted.add(0, promotedStreamTrack)

        initFacebookListenersItem()
        initWithStorageModelOnRepeatAfterSyncing(itemsWithPromoted)
        whenever(syncInitiator.sync(Syncable.SOUNDSTREAM)).thenReturn(Single.just(successWithChange()))

        assertInitialStreamFirstItemKind(Kind.FACEBOOK_LISTENER_INVITES)
    }

    @Test
    fun shouldShowUpsellAfterFirstUpsellableTrack() {
        val upsellableItemIndex = 1
        val streamItems = createItems(TimelineOperationsTest.PAGE_SIZE, 123L)
        streamItems.add(upsellableItemIndex, upsellableStreamTrack)

        val viewModels = viewModelsFromStorageModel(streamItems)

        whenever(upsellOperations.shouldDisplayInStream()).thenReturn(true)
        initWithStorageModel(streamItems, viewModels)

        assertStreamItemAtPosition(Kind.STREAM_UPSELL, upsellableItemIndex + 1)
    }

    @Test
    fun shouldNotShowUpsellWhenNoUpsellableTrackIsPresent() {
        val streamItems = createItems(TimelineOperationsTest.PAGE_SIZE, 123L)

        whenever(upsellOperations.shouldDisplayInStream()).thenReturn(true)
        initWithStorageModel(streamItems)

        assertNoUpsellInStream()
    }

    @Test
    fun shouldNotShowUpsellAfterItWasDismissedByTheUser() {
        val upsellableItemIndex = 1
        val streamItems = createItems(TimelineOperationsTest.PAGE_SIZE, 123L)
        streamItems.add(upsellableItemIndex, upsellableStreamTrack)

        whenever(upsellOperations.shouldDisplayInStream()).thenReturn(false)
        initWithStorageModel(streamItems)

        assertNoUpsellInStream()
    }

    @Test
    fun shouldShowUpsellOnlyAfterFirstUpsellableTrack() {
        val firstUpsellableTrackIndex = 2
        val secondUpsellableTrackIndex = 4
        val streamItems = createItems(TimelineOperationsTest.PAGE_SIZE, 123L)
        streamItems.add(firstUpsellableTrackIndex, upsellableStreamTrack)
        streamItems.add(secondUpsellableTrackIndex, upsellableStreamTrack)

        val streamEntity = streamItems[secondUpsellableTrackIndex + 1]
        val trackUrnAfterSecondUpsellable = streamEntity.urn()
        whenever(upsellOperations.shouldDisplayInStream()).thenReturn(true)
        initWithStorageModel(streamItems)

        assertTrackStreamItemAtPosition(trackUrnAfterSecondUpsellable, secondUpsellableTrackIndex + 2)
    }

    @Test
    fun shouldDisableUpsell() {
        streamOperations.disableUpsell()

        verify(upsellOperations).disableInStream()
    }

    @Test
    fun shouldClearData() {
        streamOperations.clearData()

        verify(upsellOperations).clearData()
    }

    @Test
    fun urnsForPlaybackReturnsUrnsFromStorage() {
        val playableWithReposter = PlayableWithReposter.from(Urn.forTrack(123L))
        whenever(streamStorage.playbackItems()).thenReturn(just(playableWithReposter))

        val subscriber = streamOperations.urnsForPlayback().test()

        subscriber.assertValue(listOf(playableWithReposter))
    }

    @Test
    override fun shouldReturnNewItemsSinceTimestamp() {
        whenever(streamStorage.timelineItemCountSince(123L)).thenReturn(just(3))

        val subscriber = streamOperations.newItemsSince(123L).test()

        subscriber.assertValue(3)
    }

    @Test
    fun shouldUpdateStreamForStartWhenSyncedBefore() {
        val items = createItems(TimelineOperationsTest.PAGE_SIZE, 123L)
        val viewModels = viewModelsFromStorageModel(items)

        whenever(syncStateStorage.hasSyncedBefore(Syncable.SOUNDSTREAM)).thenReturn(true)
        whenever(syncInitiator.sync(Syncable.SOUNDSTREAM)).thenReturn(Single.just(successWithChange()))
        whenever(syncInitiator.sync(Syncable.SOUNDSTREAM, SyncInitiator.ACTION_HARD_REFRESH)).thenReturn(Single.just(successWithChange()))

        initWithStorageModel(items, viewModels)

        val subscriber = streamOperations.updatedTimelineItemsForStart().test()

        subscriber.assertValue(viewModels)
    }

    @Test
    override fun shouldNotUpdateStreamForStartWhenNeverSyncedBefore() {
        whenever(syncStateStorage.hasSyncedBefore(Syncable.SOUNDSTREAM)).thenReturn(false)
        whenever(syncInitiator.sync(Syncable.SOUNDSTREAM, SyncInitiator.ACTION_HARD_REFRESH)).thenReturn(Single.just(successWithChange()))

        val subscriber = streamOperations.updatedTimelineItemsForStart().test()

        subscriber.assertNoValues()
    }

    @Test
    fun shouldInsertAdsOnInitialStreamLoad() {
        initWithStorageModelOnRepeatAfterSyncing(createItems(TimelineOperationsTest.PAGE_SIZE, 123L))
        whenever(syncInitiator.sync(Syncable.SOUNDSTREAM)).thenReturn(Single.just(successWithChange()))

        streamOperations.initialStreamItems().subscribe(observer)

        verify(streamAdsController).insertAds()
    }

    @Test
    fun shouldInsertAdsOnStreamUpdate() {
        initUpdatedTimelineItems()
        streamOperations.updatedStreamItems().subscribe(observer)

        verify(streamAdsController).insertAds()
    }

    override fun createItems(length: Int, lastItemTimestamp: Long): MutableList<StreamEntity> {
        return super.createStorageModels(length, lastItemTimestamp)
    }

    override fun createTimelineItem(timestamp: Long): StreamEntity {
        return PlayableFixtures.timelineItem(Date(timestamp))
    }

    override fun viewModelsFromStorageModel(dataItems: List<StreamEntity>): List<StreamItem> {
        val items = ArrayList<StreamItem>(dataItems.size)
        for (source in dataItems) {
            if (source.urn() == promotedTrackItem.urn) {
                items.add(TrackStreamItem.create(promotedTrackItem, Date(), source.avatarUrlTemplate()))
            } else if (source.urn() == upsellableTrack.urn) {
                items.add(TrackStreamItem.create(upsellableTrack, Date(), source.avatarUrlTemplate()))
            } else {
                items.add(ModelFixtures.trackFromStreamEntity(source))
            }
        }
        return items
    }

    private fun assertInitialStreamEmpty() {
        val subscriber = subscribeToInitialStream()
        subscriber.assertValue(emptyList())
    }

    private fun assertInitialStreamFirstItemKind(kind: Kind) {
        assertStreamItemAtPosition(kind, 0)
    }

    private fun assertTrackStreamItemAtPosition(urn: Urn, index: Int) {
        val subscriber = subscribeToInitialStream()
        val firstItem = subscriber.values()[0][index] as TrackStreamItem
        assertThat(firstItem.trackItem.urn).isEqualTo(urn)
    }

    private fun assertStreamItemAtPosition(kind: Kind, index: Int) {
        val subscriber = subscribeToInitialStream()
        val firstItem = subscriber.values()[0][index]
        assertThat(firstItem.kind).isEqualTo(kind)
    }

    private fun assertNoUpsellInStream() {
        val subscriber = subscribeToInitialStream()
        val stream = subscriber.values()[0]
        for (item in stream) {
            assertThat(item.kind).isNotEqualTo(Kind.STREAM_UPSELL)
        }
    }

    private fun subscribeToInitialStream(): TestObserver<List<StreamItem>> {
        return streamOperations.initialStreamItems().test()
    }

    private fun initUpdatedTimelineItems(): List<StreamItem> {
        whenever(syncInitiator.sync(Syncable.SOUNDSTREAM, SyncInitiator.ACTION_HARD_REFRESH)).thenReturn(Single.just(successWithChange()))
        val items = createItems(TimelineOperationsTest.PAGE_SIZE, 123L)
        items.add(0, promotedStreamTrack)

        val streamItemsWithPromoted = viewModelsFromStorageModel(items)
        initWithStorageModel(items, streamItemsWithPromoted)
        return streamItemsWithPromoted
    }

    private fun initSuggestedCreatorsItem() {
        whenever(suggestedCreatorsOperations.suggestedCreators()).thenReturn(Maybe.just(suggestedCreatorsItem))
    }

    private fun initFacebookListenersItem() {
        whenever(facebookInvitesOperations.listenerInvites()).thenReturn(Maybe.just(StreamItem.FacebookListenerInvites()))
    }

    private fun initFacebookCreatorsItem() {
        val streamItem = StreamItem.FacebookCreatorInvites(Urn.forTrack(1L), "url123")
        whenever(facebookInvitesOperations.creatorInvites()).thenReturn(Maybe.just(streamItem))
    }

    private fun fromPromotedTrackItem(createdAt: Date, promotedTrackItem: TrackItem): StreamEntity {
        return builderFromTrackItem(createdAt, promotedTrackItem).promotedProperties(promotedTrackItem.promotedProperties()).build()
    }

    private fun builderFromTrackItem(createdAt: Date, promotedTrackItem: TrackItem): StreamEntity.Builder {
        return StreamEntity.builder(promotedTrackItem.urn, createdAt)
    }

    private fun initWithTrack() {
        whenever(streamStorage.timelineItems(TimelineOperationsTest.PAGE_SIZE))
                .thenReturn(Observable.empty())
                .thenReturn(just(promotedStreamTrack))
        val result = Lists.newArrayList<StreamItem>(TrackStreamItem.create(promotedTrackItem, promotedStreamTrack.createdAt(), Optional.absent()))
        whenever(streamEntityToItemTransformer.apply(eq(Lists.newArrayList(promotedStreamTrack)))).thenReturn(Single.just(result))
    }

    override fun initWithStorageModel(streamEntities: List<StreamEntity>) {
        val streamItems = Lists.transform(streamEntities, { ModelFixtures.trackFromStreamEntity(it) })
        initWithStorageModel(streamEntities, streamItems)
    }

    override fun initWithStorageModelOnRepeatAfterSyncing(streamEntities: List<StreamEntity>) {
        val streamItems = Lists.transform(streamEntities, { ModelFixtures.trackFromStreamEntity(it) })
        initWithStorageModelOnRepeatAfterSyncing(streamEntities, streamItems)
    }

    override fun initViewModelFromStorageModel(streamEntities: List<StreamEntity>, streamItems: List<StreamItem>) {
        whenever(streamEntityToItemTransformer.apply(eq(streamEntities))).thenReturn(Single.just(streamItems))
    }
}
