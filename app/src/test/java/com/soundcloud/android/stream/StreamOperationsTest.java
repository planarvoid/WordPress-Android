package com.soundcloud.android.stream;

import static com.soundcloud.android.stream.StreamItem.forFacebookCreatorInvites;
import static com.soundcloud.android.stream.StreamItem.forFacebookListenerInvites;
import static com.soundcloud.android.testsupport.fixtures.TestSyncJobResults.successWithChange;
import static io.reactivex.Observable.just;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.ads.StreamAdsController;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PromotedTrackingEvent;
import com.soundcloud.android.facebookinvites.FacebookInvitesOperations;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlayableWithReposter;
import com.soundcloud.android.stream.StreamItem.Kind;
import com.soundcloud.android.suggestedcreators.SuggestedCreatorsOperations;
import com.soundcloud.android.sync.SyncInitiator;
import com.soundcloud.android.sync.SyncStateStorage;
import com.soundcloud.android.sync.Syncable;
import com.soundcloud.android.sync.timeline.TimelineOperations;
import com.soundcloud.android.sync.timeline.TimelineOperationsTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.testsupport.fixtures.PlayableFixtures;
import com.soundcloud.android.tracks.Track;
import com.soundcloud.android.tracks.TrackItem;
import com.soundcloud.android.upsell.InlineUpsellOperations;
import com.soundcloud.android.users.User;
import com.soundcloud.java.collections.Lists;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.rx.eventbus.TestEventBus;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Scheduler;
import io.reactivex.Single;
import io.reactivex.SingleObserver;
import io.reactivex.observers.TestObserver;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import rx.subjects.PublishSubject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class StreamOperationsTest extends TimelineOperationsTest<StreamEntity, StreamItem, StreamStorage> {

    private final StreamItem SUGGESTED_CREATORS_ITEM = StreamItem.forSuggestedCreators(Lists.newArrayList());
    private StreamOperations operations;

    @Mock private StreamStorage streamStorage;
    @Mock private SingleObserver<List<StreamItem>> observer;
    @Mock private RemoveStalePromotedItemsCommand removeStalePromotedItemsCommand;
    @Mock private MarkPromotedItemAsStaleCommand markPromotedItemAsStaleCommand;
    @Mock private FacebookInvitesOperations facebookInvitesOperations;
    @Mock private StreamAdsController streamAdsController;
    @Mock private InlineUpsellOperations upsellOperations;
    @Mock private SuggestedCreatorsOperations suggestedCreatorsOperations;
    @Mock private StreamEntityToItemTransformer streamEntityToItemTransformer;

    private TestEventBus eventBus = new TestEventBus();

    private final User promoter = ModelFixtures.user();
    private final Track promotedTrack = ModelFixtures.track();
    private final TrackItem promotedTrackItem = ModelFixtures.promotedTrackItem(promotedTrack, promoter);
    private final StreamEntity promotedStreamTrack = fromPromotedTrackItem(new Date(), promotedTrackItem);
    private final TrackItem upsellableTrack = PlayableFixtures.upsellableTrack();
    private final StreamEntity upsellableStreamTrack = builderFromTrackItem(new Date(), upsellableTrack).build();

    @Before
    public void setUp() throws Exception {
        when(removeStalePromotedItemsCommand.toSingle(null)).thenReturn(Single.just(Collections.emptyList()));
        when(facebookInvitesOperations.creatorInvites()).thenReturn(Maybe.empty());
        when(facebookInvitesOperations.listenerInvites()).thenReturn(Maybe.empty());
        when(suggestedCreatorsOperations.suggestedCreators()).thenReturn(Maybe.empty());
        when(streamEntityToItemTransformer.apply(eq(emptyList()))).thenReturn(Single.just(emptyList()));
        this.operations = (StreamOperations) super.operations;
    }

    @Override
    protected TimelineOperations<StreamEntity, StreamItem> buildOperations(StreamStorage storage,
                                                                           SyncInitiator syncInitiator,
                                                                           Scheduler scheduler,
                                                                           SyncStateStorage syncStateStorage) {
        return new StreamOperations(storage, syncInitiator, removeStalePromotedItemsCommand,
                                    markPromotedItemAsStaleCommand, eventBus, scheduler, facebookInvitesOperations,
                                    streamAdsController, upsellOperations, syncStateStorage,
                                    suggestedCreatorsOperations, streamEntityToItemTransformer);
    }

    @Override
    protected StreamStorage provideStorageMock() {
        return streamStorage;
    }

    @Override
    protected Syncable provideSyncable() {
        return Syncable.SOUNDSTREAM;
    }

    @Test
    public void streamIsConsideredEmptyWhenOnlyPromotedTrackIsReturnedAndDoesNotSyncAgain() {
        // 1st page comes back blank first, then includes promoted promotedTrack only
        initWithTrack();
        // returning true means successful sync
        when(syncInitiator.sync(Syncable.SOUNDSTREAM)).thenReturn(Single.just(successWithChange()));

        operations.initialStreamItems().subscribe(observer);

        InOrder inOrder = inOrder(this.observer, streamStorage, syncInitiator);
        inOrder.verify(streamStorage).timelineItems(PAGE_SIZE);
        inOrder.verify(syncInitiator).sync(Syncable.SOUNDSTREAM);
        inOrder.verify(streamStorage).timelineItems(PAGE_SIZE);
        inOrder.verify(this.observer).onSuccess(Collections.emptyList());
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void showsSuggestedCreatorsWhenOnlyPromotedTrackIsReturned() {
        initWithTrack();
        when(syncInitiator.sync(Syncable.SOUNDSTREAM)).thenReturn(Single.just(successWithChange()));

        initSuggestedCreatorsItem();

        assertInitialStreamFirstItemKind(Kind.SUGGESTED_CREATORS);
    }

    @Test
    public void showsSuggestedCreatorsWhenNoTracksAreReturned() {
        when(streamStorage.timelineItems(PAGE_SIZE))
                .thenReturn(Observable.empty())
                .thenReturn(Observable.empty());
        when(syncInitiator.sync(Syncable.SOUNDSTREAM)).thenReturn(Single.just(successWithChange()));

        initSuggestedCreatorsItem();

        assertInitialStreamFirstItemKind(Kind.SUGGESTED_CREATORS);
    }

    @Test
    public void showsSuggestedCreatorsInsteadOfOtherNotificationItems() {
        final List<StreamEntity> items = createItems(PAGE_SIZE, 123L);
        initWithStorageModel(items);

        initSuggestedCreatorsItem();
        initFacebookCreatorsItem();
        initFacebookListenersItem();

        assertInitialStreamFirstItemKind(Kind.SUGGESTED_CREATORS);
    }

    @Test
    public void showsSuggestedCreatorsInUpdatedItems() {
        initUpdatedTimelineItems();

        initSuggestedCreatorsItem();

        final TestObserver<List<StreamItem>> subscriber = operations.updatedStreamItems().test();

        subscriber.assertValueCount(1);
        final StreamItem streamItem = subscriber.values().get(0).get(0);
        assertThat(streamItem.kind()).isEqualTo(Kind.SUGGESTED_CREATORS);
    }

    @Test
    public void initialStreamDeletesStalePromotedTracksBeforeLoadingStreamItems() {
        final PublishSubject<List<Long>> subject = PublishSubject.create();
        when(removeStalePromotedItemsCommand.toObservable(null)).thenReturn(subject);
        when(streamStorage.timelineItems(PAGE_SIZE)).thenReturn(Observable.create(subscriber1 -> {
            assertThat(subject.hasObservers()).isTrue();
        }));

        operations.initialStreamItems().subscribe(observer);
        subject.onNext(Collections.emptyList());

        verify(streamStorage).timelineItems(PAGE_SIZE);
    }

    @Test
    public void publishPromotedImpressionDoesNothingIfThereAreNoPromotedItems() {
        operations.publishPromotedImpression(Collections.emptyList());

        eventBus.verifyNoEventsOn(EventQueue.TRACKING);
        verify(markPromotedItemAsStaleCommand, never()).toSingle(any());
    }

    @Test
    public void publishPromotedImpressionFiresImpressionsOnlyOncePerPromotedItem() {
        when(markPromotedItemAsStaleCommand.toSingle("ad:urn:123")).thenReturn(Single.never());
        final List<StreamEntity> itemsWithPromoted = createItems(PAGE_SIZE, 123L);
        itemsWithPromoted.add(0, promotedStreamTrack);
        initWithStorageModelOnRepeatAfterSyncing(itemsWithPromoted);
        when(syncInitiator.sync(Syncable.SOUNDSTREAM)).thenReturn(Single.just(successWithChange()));
        final TestObserver<List<StreamItem>> subscriber = subscribeToInitialStream();
        final List<StreamItem> streamItems = subscriber.values().get(0);

        //Call impression twice on same stream items
        operations.publishPromotedImpression(streamItems);
        operations.publishPromotedImpression(streamItems);

        assertThat(eventBus.eventsOn(EventQueue.TRACKING).size()).isEqualTo(1);
        assertThat(eventBus.lastEventOn(EventQueue.TRACKING)).isInstanceOf(PromotedTrackingEvent.class);
        verify(markPromotedItemAsStaleCommand, times(1)).toSingle("ad:urn:123");
    }

    @Test
    public void shouldShowFacebookListenerInvitesAsFirstItem() {
        final List<StreamEntity> items = createItems(PAGE_SIZE, 123L);
        initWithStorageModel(items);
        initFacebookListenersItem();

        assertInitialStreamFirstItemKind(Kind.FACEBOOK_LISTENER_INVITES);
    }

    @Test
    public void shouldShowFacebookCreatorInvitesAsFirstItem() {
        final List<StreamEntity> items = createItems(PAGE_SIZE, 123L);
        initWithStorageModel(items);
        initFacebookListenersItem();
        initFacebookCreatorsItem();

        assertInitialStreamFirstItemKind(Kind.FACEBOOK_CREATORS);
    }

    @Test
    public void shouldNotShowFacebookInvitesOnEmptyStream() {
        initFacebookListenersItem();
        when(streamStorage.timelineItems(PAGE_SIZE))
                .thenReturn(Observable.empty())
                .thenReturn(Observable.empty());
        when(syncInitiator.sync(Syncable.SOUNDSTREAM)).thenReturn(Single.just(successWithChange()));

        assertInitialStreamEmpty();
    }

    @Test
    public void shouldNotShowFacebookInvitesOnPromotedOnlyStream() {
        initFacebookListenersItem();
        initWithTrack();
        when(syncInitiator.sync(Syncable.SOUNDSTREAM)).thenReturn(Single.just(successWithChange()));

        assertInitialStreamEmpty();
    }

    @Test
    public void shouldShowFacebookInvitesAbovePromotedItems() {
        final List<StreamEntity> itemsWithPromoted = createItems(PAGE_SIZE, 123L);
        itemsWithPromoted.add(0, promotedStreamTrack);

        initFacebookListenersItem();
        initWithStorageModelOnRepeatAfterSyncing(itemsWithPromoted);
        when(syncInitiator.sync(Syncable.SOUNDSTREAM)).thenReturn(Single.just(successWithChange()));

        assertInitialStreamFirstItemKind(Kind.FACEBOOK_LISTENER_INVITES);
    }

    @Test
    public void shouldShowUpsellAfterFirstUpsellableTrack() {
        final int upsellableItemIndex = 1;
        final List<StreamEntity> streamItems = createItems(PAGE_SIZE, 123L);
        streamItems.add(upsellableItemIndex, upsellableStreamTrack);

        final List<StreamItem> viewModels = viewModelsFromStorageModel(streamItems);

        when(upsellOperations.shouldDisplayInStream()).thenReturn(true);
        initWithStorageModel(streamItems, viewModels);

        assertStreamItemAtPosition(Kind.STREAM_UPSELL, upsellableItemIndex + 1);
    }

    @Test
    public void shouldNotShowUpsellWhenNoUpsellableTrackIsPresent() {
        final List<StreamEntity> streamItems = createItems(PAGE_SIZE, 123L);

        when(upsellOperations.shouldDisplayInStream()).thenReturn(true);
        initWithStorageModel(streamItems);

        assertNoUpsellInStream();
    }

    @Test
    public void shouldNotShowUpsellAfterItWasDismissedByTheUser() {
        final int upsellableItemIndex = 1;
        final List<StreamEntity> streamItems = createItems(PAGE_SIZE, 123L);
        streamItems.add(upsellableItemIndex, upsellableStreamTrack);

        when(upsellOperations.shouldDisplayInStream()).thenReturn(false);
        initWithStorageModel(streamItems);

        assertNoUpsellInStream();
    }

    @Test
    public void shouldShowUpsellOnlyAfterFirstUpsellableTrack() {
        final int firstUpsellableTrackIndex = 2;
        final int secondUpsellableTrackIndex = 4;
        final List<StreamEntity> streamItems = createItems(PAGE_SIZE, 123L);
        streamItems.add(firstUpsellableTrackIndex, upsellableStreamTrack);
        streamItems.add(secondUpsellableTrackIndex, upsellableStreamTrack);

        final StreamEntity streamEntity = streamItems.get(secondUpsellableTrackIndex + 1);
        final Urn trackUrnAfterSecondUpsellable = streamEntity.urn();
        when(upsellOperations.shouldDisplayInStream()).thenReturn(true);
        initWithStorageModel(streamItems);

        assertTrackStreamItemAtPosition(trackUrnAfterSecondUpsellable, secondUpsellableTrackIndex + 2);
    }

    @Test
    public void shouldDisableUpsell() {
        operations.disableUpsell();

        verify(upsellOperations).disableInStream();
    }

    @Test
    public void shouldClearData() {
        operations.clearData();

        verify(upsellOperations).clearData();
    }

    @Test
    public void urnsForPlaybackReturnsUrnsFromStorage() {
        final PlayableWithReposter playableWithReposter = PlayableWithReposter.from(Urn.forTrack(123L));
        when(streamStorage.playbackItems()).thenReturn(just(playableWithReposter));

        final TestObserver<List<PlayableWithReposter>> subscriber = operations.urnsForPlayback().test();

        subscriber.assertValue(singletonList(playableWithReposter));
    }

    @Test
    public void shouldReturnNewItemsSinceTimestamp() {
        when(streamStorage.timelineItemCountSince(123L)).thenReturn(just(3));

        final TestObserver<Integer> subscriber = operations.newItemsSince(123L).test();

        subscriber.assertValue(3);
    }

    @Test
    public void shouldUpdateStreamForStartWhenSyncedBefore() {
        final List<StreamEntity> items = createItems(PAGE_SIZE, 123L);
        final List<StreamItem> viewModels = viewModelsFromStorageModel(items);

        when(syncStateStorage.hasSyncedBefore(Syncable.SOUNDSTREAM)).thenReturn(true);
        when(syncInitiator.sync(Syncable.SOUNDSTREAM)).thenReturn(Single.just(successWithChange()));
        when(syncInitiator.sync(Syncable.SOUNDSTREAM, SyncInitiator.ACTION_HARD_REFRESH)).thenReturn(Single.just(successWithChange()));

        initWithStorageModel(items, viewModels);

        final TestObserver<List<StreamItem>> subscriber = operations.updatedTimelineItemsForStart().test();

        subscriber.assertValue(viewModels);
    }

    @Test
    public void shouldNotUpdateStreamForStartWhenNeverSyncedBefore() {
        when(syncStateStorage.hasSyncedBefore(Syncable.SOUNDSTREAM)).thenReturn(false);
        when(syncInitiator.sync(Syncable.SOUNDSTREAM, SyncInitiator.ACTION_HARD_REFRESH)).thenReturn(Single.just(successWithChange()));

        final TestObserver<List<StreamItem>> subscriber = operations.updatedTimelineItemsForStart().test();

        subscriber.assertNoValues();
    }

    @Test
    public void shouldInsertAdsOnInitialStreamLoad() {
        initWithStorageModelOnRepeatAfterSyncing(createItems(PAGE_SIZE, 123L));
        when(syncInitiator.sync(Syncable.SOUNDSTREAM)).thenReturn(Single.just(successWithChange()));

        operations.initialStreamItems().subscribe(observer);

        verify(streamAdsController).insertAds();
    }

    @Test
    public void shouldInsertAdsOnStreamUpdate() {
        initUpdatedTimelineItems();
        operations.updatedStreamItems().subscribe(observer);

        verify(streamAdsController).insertAds();
    }

    @Override
    protected List<StreamEntity> createItems(int length, long lastItemTimestamp) {
        return super.createStorageModels(length, lastItemTimestamp);
    }

    @Override
    protected StreamEntity createTimelineItem(long timestamp) {
        return PlayableFixtures.timelineItem(new Date(timestamp));
    }

    @Override
    protected List<StreamItem> viewModelsFromStorageModel(List<StreamEntity> dataItems) {
        final List<StreamItem> items = new ArrayList<>(dataItems.size());
        for (StreamEntity source : dataItems) {
            if (source.urn().equals(promotedTrackItem.getUrn())) {
                items.add(TrackStreamItem.create(promotedTrackItem, new Date(), source.avatarUrlTemplate()));
            } else if (source.urn().equals(upsellableTrack.getUrn())) {
                items.add(TrackStreamItem.create(upsellableTrack, new Date(), source.avatarUrlTemplate()));
            } else {
                items.add(ModelFixtures.trackFromStreamEntity(source));
            }
        }
        return items;
    }

    private void assertInitialStreamEmpty() {
        final TestObserver<List<StreamItem>> subscriber = subscribeToInitialStream();
        subscriber.assertValue(Collections.emptyList());
    }

    private void assertInitialStreamFirstItemKind(Kind kind) {
        assertStreamItemAtPosition(kind, 0);
    }

    private void assertTrackStreamItemAtPosition(Urn urn, int index) {
        final TestObserver<List<StreamItem>> subscriber = subscribeToInitialStream();
        final TrackStreamItem firstItem = (TrackStreamItem) subscriber.values().get(0).get(index);
        assertThat(firstItem.trackItem().getUrn()).isEqualTo(urn);
    }

    private void assertStreamItemAtPosition(Kind kind, int index) {
        final TestObserver<List<StreamItem>> subscriber = subscribeToInitialStream();
        final StreamItem firstItem = subscriber.values().get(0).get(index);
        assertThat(firstItem.kind()).isEqualTo(kind);
    }

    private void assertNoUpsellInStream() {
        final TestObserver<List<StreamItem>> subscriber = subscribeToInitialStream();
        final List<StreamItem> stream = subscriber.values().get(0);
        for (StreamItem item : stream) {
            assertThat(item.kind()).isNotEqualTo(Kind.STREAM_UPSELL);
        }
    }

    private TestObserver<List<StreamItem>> subscribeToInitialStream() {
        return operations.initialStreamItems().test();
    }

    private List<StreamItem> initUpdatedTimelineItems() {
        when(syncInitiator.sync(Syncable.SOUNDSTREAM, SyncInitiator.ACTION_HARD_REFRESH))
                .thenReturn(Single.just(successWithChange()));
        final List<StreamEntity> items = createItems(PAGE_SIZE, 123L);
        items.add(0, promotedStreamTrack);

        final List<StreamItem> streamItemsWithPromoted = viewModelsFromStorageModel(items);
        initWithStorageModel(items, streamItemsWithPromoted);
        return streamItemsWithPromoted;
    }

    private void initSuggestedCreatorsItem() {
        when(suggestedCreatorsOperations.suggestedCreators()).thenReturn(Maybe.just(SUGGESTED_CREATORS_ITEM));
    }

    private void initFacebookListenersItem() {
        when(facebookInvitesOperations.listenerInvites()).thenReturn(Maybe.just(forFacebookListenerInvites()));
    }

    private void initFacebookCreatorsItem() {
        final StreamItem streamItem = forFacebookCreatorInvites(Urn.forTrack(1L), "url123");
        when(facebookInvitesOperations.creatorInvites()).thenReturn(Maybe.just(streamItem));
    }

    private StreamEntity fromPromotedTrackItem(Date createdAt, TrackItem promotedTrackItem) {
        return builderFromTrackItem(createdAt, promotedTrackItem).promotedProperties(promotedTrackItem.promotedProperties()).build();
    }

    private StreamEntity.Builder builderFromTrackItem(Date createdAt, TrackItem promotedTrackItem) {
        return StreamEntity.builder(promotedTrackItem.getUrn(), createdAt);
    }

    private void initWithTrack() {
        when(streamStorage.timelineItems(PAGE_SIZE))
                .thenReturn(Observable.empty())
                .thenReturn(just(promotedStreamTrack));
        List<StreamItem> result = Lists.newArrayList(TrackStreamItem.create(promotedTrackItem, promotedStreamTrack.createdAt(), Optional.absent()));
        when(streamEntityToItemTransformer.apply(eq(Lists.newArrayList(promotedStreamTrack)))).thenReturn(Single.just(result));
    }

    @Override
    protected void initWithStorageModel(List<StreamEntity> streamEntities) {
        List<StreamItem> streamItems = Lists.transform(streamEntities, ModelFixtures::trackFromStreamEntity);
        initWithStorageModel(streamEntities, streamItems);
    }

    @Override
    protected void initWithStorageModelOnRepeatAfterSyncing(List<StreamEntity> streamEntities) {
        List<StreamItem> streamItems = Lists.transform(streamEntities, ModelFixtures::trackFromStreamEntity);
        initWithStorageModelOnRepeatAfterSyncing(streamEntities, streamItems);
    }

    @Override
    protected void initViewModelFromStorageModel(List<StreamEntity> streamEntities, List<StreamItem> streamItems) {
        when(streamEntityToItemTransformer.apply(eq(streamEntities))).thenReturn(Single.just(streamItems));
    }
}
