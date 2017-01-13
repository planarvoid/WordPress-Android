package com.soundcloud.android.stream;

import static com.soundcloud.android.stream.StreamItem.forFacebookCreatorInvites;
import static com.soundcloud.android.stream.StreamItem.forFacebookListenerInvites;
import static com.soundcloud.android.stream.StreamItem.forStationOnboarding;
import static com.soundcloud.android.testsupport.fixtures.TestSyncJobResults.successWithChange;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static rx.Observable.empty;
import static rx.Observable.from;

import com.soundcloud.android.ads.StreamAdsController;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PromotedTrackingEvent;
import com.soundcloud.android.facebookinvites.FacebookInvitesOperations;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.playback.PlayableWithReposter;
import com.soundcloud.android.presentation.PromotedListItem;
import com.soundcloud.android.stations.StationsOperations;
import com.soundcloud.android.stream.StreamItem.Kind;
import com.soundcloud.android.suggestedcreators.SuggestedCreator;
import com.soundcloud.android.suggestedcreators.SuggestedCreatorsOperations;
import com.soundcloud.android.sync.SyncInitiator;
import com.soundcloud.android.sync.SyncStateStorage;
import com.soundcloud.android.sync.Syncable;
import com.soundcloud.android.sync.timeline.TimelineOperations;
import com.soundcloud.android.sync.timeline.TimelineOperationsTest;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.android.upsell.InlineUpsellOperations;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.assertj.core.util.Lists;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import rx.Observable;
import rx.Observer;
import rx.Scheduler;
import rx.Subscriber;
import rx.observers.TestSubscriber;
import rx.subjects.PublishSubject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class StreamOperationsTest extends TimelineOperationsTest<StreamPlayable, StreamItem, StreamStorage> {

    private final StreamItem SUGGESTED_CREATORS_ITEM = StreamItem.forSuggestedCreators(Lists.<SuggestedCreator>newArrayList());
    private StreamOperations operations;

    @Mock private StreamStorage streamStorage;
    @Mock private Observer<List<StreamItem>> observer;
    @Mock private RemoveStalePromotedItemsCommand removeStalePromotedItemsCommand;
    @Mock private MarkPromotedItemAsStaleCommand markPromotedItemAsStaleCommand;
    @Mock private FacebookInvitesOperations facebookInvitesOperations;
    @Mock private StreamAdsController streamAdsController;
    @Mock private StationsOperations stationsOperations;
    @Mock private InlineUpsellOperations upsellOperations;
    @Mock private SuggestedCreatorsOperations suggestedCreatorsOperations;
    @Mock private StreamHighlightsOperations streamHighlightsOperations;

    private TestEventBus eventBus = new TestEventBus();

    private final StreamPlayable promotedStreamTrack = StreamPlayable.createFromPlayable(new Date(), TestPropertySets.expectedPromotedTrack());
    private final StreamPlayable upsellableStreamTrack = StreamPlayable.createFromPlayable(new Date(), TestPropertySets.upsellableTrack());

    @Before
    public void setUp() throws Exception {
        when(removeStalePromotedItemsCommand.toObservable(null)).thenReturn(Observable.just(Collections.emptyList()));
        when(facebookInvitesOperations.creatorInvites()).thenReturn(empty());
        when(facebookInvitesOperations.listenerInvites()).thenReturn(empty());
        when(stationsOperations.onboardingStreamItem()).thenReturn(empty());
        when(suggestedCreatorsOperations.suggestedCreators()).thenReturn(empty());
        when(streamHighlightsOperations.highlights()).thenReturn(empty());
        this.operations = (StreamOperations) super.operations;
    }

    @Override
    protected TimelineOperations<StreamItem, StreamPlayable> buildOperations(StreamStorage storage,
                                                                             SyncInitiator syncInitiator,
                                                                             Scheduler scheduler,
                                                                             SyncStateStorage syncStateStorage) {
        return new StreamOperations(storage, syncInitiator, removeStalePromotedItemsCommand,
                                    markPromotedItemAsStaleCommand, eventBus, scheduler, facebookInvitesOperations,
                                    streamAdsController, stationsOperations, upsellOperations, syncStateStorage,
                                    streamHighlightsOperations, suggestedCreatorsOperations);
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
        // 1st page comes back blank first, then includes promoted track only
        when(streamStorage.timelineItems(PAGE_SIZE))
                .thenReturn(Observable.<StreamPlayable>empty())
                .thenReturn(Observable.just(promotedStreamTrack));
        // returning true means successful sync
        when(syncInitiator.sync(Syncable.SOUNDSTREAM)).thenReturn(Observable.just(successWithChange()));

        operations.initialStreamItems().subscribe(observer);

        InOrder inOrder = inOrder(observer, streamStorage, syncInitiator);
        inOrder.verify(streamStorage).timelineItems(PAGE_SIZE);
        inOrder.verify(syncInitiator).sync(Syncable.SOUNDSTREAM);
        inOrder.verify(streamStorage).timelineItems(PAGE_SIZE);
        inOrder.verify(observer).onNext(Collections.<StreamItem>emptyList());
        inOrder.verify(observer).onCompleted();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void showsSuggestedCreatorsWhenOnlyPromotedTrackIsReturned() {
        when(streamStorage.timelineItems(PAGE_SIZE))
                .thenReturn(Observable.<StreamPlayable>empty())
                .thenReturn(Observable.just(promotedStreamTrack));
        when(syncInitiator.sync(Syncable.SOUNDSTREAM)).thenReturn(Observable.just(successWithChange()));

        initSuggestedCreatorsItem();

        assertInitialStreamFirstItemKind(Kind.SUGGESTED_CREATORS);
    }

    @Test
    public void showsSuggestedCreatorsWhenNoTracksAreReturned() {
        when(streamStorage.timelineItems(PAGE_SIZE))
                .thenReturn(Observable.<StreamPlayable>empty())
                .thenReturn(Observable.<StreamPlayable>empty());
        when(syncInitiator.sync(Syncable.SOUNDSTREAM)).thenReturn(Observable.just(successWithChange()));

        initSuggestedCreatorsItem();

        assertInitialStreamFirstItemKind(Kind.SUGGESTED_CREATORS);
    }

    @Test
    public void showsSuggestedCreatorsInsteadOfOtherNotificationItems() {
        final List<StreamPlayable> items = createItems(PAGE_SIZE, 123L);
        when(streamStorage.timelineItems(PAGE_SIZE)).thenReturn(Observable.from(items));

        initSuggestedCreatorsItem();
        initFacebookCreatorsItem();
        initFacebookListenersItem();
        initOnboardingStationsItem();

        assertInitialStreamFirstItemKind(Kind.SUGGESTED_CREATORS);
    }

    @Test
    public void showsSuggestedCreatorsInUpdatedItems() {
        final TestSubscriber<List<StreamItem>> subscriber = new TestSubscriber<>();
        initUpdatedTimelineItems();

        initSuggestedCreatorsItem();

        operations.updatedStreamItems().subscribe(subscriber);

        subscriber.assertValueCount(1);
        final StreamItem streamItem = subscriber.getOnNextEvents().get(0).get(0);
        assertThat(streamItem.kind()).isEqualTo(Kind.SUGGESTED_CREATORS);
    }

    @Test
    public void initialStreamDeletesStalePromotedTracksBeforeLoadingStreamItems() {
        final PublishSubject<List<Long>> subject = PublishSubject.create();
        final AtomicBoolean verified = new AtomicBoolean();
        when(removeStalePromotedItemsCommand.toObservable(null)).thenReturn(subject);
        when(streamStorage.timelineItems(PAGE_SIZE)).thenReturn(Observable.create(subscriber1 -> {
            verified.set(subject.hasObservers());
        }));

        operations.initialStreamItems().subscribe(observer);
        subject.onNext(Collections.<Long>emptyList());

        assertThat(verified.get()).isTrue();
    }

    @Test
    public void initialStreamWithPromotedTrackTriggersPromotedTrackImpression() {
        final List<StreamPlayable> itemsWithPromoted = createItems(PAGE_SIZE, 123L);
        itemsWithPromoted.add(0, promotedStreamTrack);

        final List<StreamItem> streamItemsWithPromoted = viewModelsFromStorageModel(itemsWithPromoted);

        when(streamStorage.timelineItems(PAGE_SIZE))
                .thenReturn(Observable.<StreamPlayable>empty())
                .thenReturn(Observable.from(itemsWithPromoted));
        when(syncInitiator.sync(Syncable.SOUNDSTREAM)).thenReturn(Observable.just(successWithChange()));

        operations.initialStreamItems().subscribe(observer);

        assertThat(eventBus.eventsOn(EventQueue.TRACKING)).hasSize(1);
        assertThat(eventBus.lastEventOn(EventQueue.TRACKING)).isInstanceOf(PromotedTrackingEvent.class);
        TrackStreamItem track = (TrackStreamItem) streamItemsWithPromoted.get(0);
        verify(markPromotedItemAsStaleCommand).call((PromotedListItem) track.trackItem());
    }

    @Test
    public void updatedItemsStreamWithPromotedTrackTriggersPromotedTrackImpression() {
        final List<StreamItem> streamItemsWithPromoted = initUpdatedTimelineItems();

        operations.updatedStreamItems().subscribe(observer);

        assertThat(eventBus.lastEventOn(EventQueue.TRACKING)).isInstanceOf(PromotedTrackingEvent.class);
        TrackStreamItem track = (TrackStreamItem) streamItemsWithPromoted.get(0);
        verify(markPromotedItemAsStaleCommand).call((PromotedListItem) track.trackItem());
    }

    @Test
    public void shouldShowFacebookListenerInvitesAsFirstItem() {
        final List<StreamPlayable> items = createItems(PAGE_SIZE, 123L);
        when(streamStorage.timelineItems(PAGE_SIZE)).thenReturn(Observable.from(items));
        initFacebookListenersItem();

        assertInitialStreamFirstItemKind(Kind.FACEBOOK_LISTENER_INVITES);
    }

    @Test
    public void shouldShowFacebookCreatorInvitesAsFirstItem() {
        final List<StreamPlayable> items = createItems(PAGE_SIZE, 123L);
        when(streamStorage.timelineItems(PAGE_SIZE)).thenReturn(Observable.from(items));
        initFacebookListenersItem();
        initFacebookCreatorsItem();

        assertInitialStreamFirstItemKind(Kind.FACEBOOK_CREATORS);
    }

    @Test
    public void shouldNotShowFacebookInvitesOnEmptyStream() {
        initFacebookListenersItem();
        when(streamStorage.timelineItems(PAGE_SIZE))
                .thenReturn(Observable.<StreamPlayable>empty())
                .thenReturn(Observable.<StreamPlayable>empty());
        when(syncInitiator.sync(Syncable.SOUNDSTREAM)).thenReturn(Observable.just(successWithChange()));

        assertInitialStreamEmpty();
    }

    @Test
    public void shouldNotShowFacebookInvitesOnPromotedOnlyStream() {
        initFacebookListenersItem();
        when(streamStorage.timelineItems(PAGE_SIZE))
                .thenReturn(Observable.<StreamPlayable>empty())
                .thenReturn(Observable.just(promotedStreamTrack));
        when(syncInitiator.sync(Syncable.SOUNDSTREAM)).thenReturn(Observable.just(successWithChange()));

        assertInitialStreamEmpty();
    }

    @Test
    public void shouldShowFacebookInvitesAbovePromotedItems() {
        final List<StreamPlayable> itemsWithPromoted = createItems(PAGE_SIZE, 123L);
        itemsWithPromoted.add(0, promotedStreamTrack);

        initFacebookListenersItem();
        when(streamStorage.timelineItems(PAGE_SIZE))
                .thenReturn(Observable.<StreamPlayable>empty())
                .thenReturn(Observable.from(itemsWithPromoted));
        when(syncInitiator.sync(Syncable.SOUNDSTREAM)).thenReturn(Observable.just(successWithChange()));

        assertInitialStreamFirstItemKind(Kind.FACEBOOK_LISTENER_INVITES);
    }

    @Test
    public void showStationsOnboardingAsFirstItem() {
        final List<StreamPlayable> items = createItems(PAGE_SIZE, 123L);
        when(streamStorage.timelineItems(PAGE_SIZE)).thenReturn(Observable.from(items));
        initOnboardingStationsItem();

        assertInitialStreamFirstItemKind(Kind.STATIONS_ONBOARDING);
    }

    @Test
    public void shouldNotShowStationsOnboardingOnEmptyStream() {
        initOnboardingStationsItem();
        when(streamStorage.timelineItems(PAGE_SIZE))
                .thenReturn(Observable.<StreamPlayable>empty())
                .thenReturn(Observable.<StreamPlayable>empty());
        when(syncInitiator.sync(Syncable.SOUNDSTREAM)).thenReturn(Observable.just(successWithChange()));

        assertInitialStreamEmpty();
    }

    @Test
    public void shouldNotShowStationsOnboardingOnPromotedOnlyStream() {
        initOnboardingStationsItem();
        when(streamStorage.timelineItems(PAGE_SIZE))
                .thenReturn(Observable.<StreamPlayable>empty())
                .thenReturn(Observable.just(promotedStreamTrack));
        when(syncInitiator.sync(Syncable.SOUNDSTREAM)).thenReturn(Observable.just(successWithChange()));

        assertInitialStreamEmpty();
    }

    @Test
    public void shouldShowStationsOnboardingAbovePromotedItems() {
        final List<StreamPlayable> itemsWithPromoted = createItems(PAGE_SIZE, 123L);
        itemsWithPromoted.add(0, promotedStreamTrack);

        initOnboardingStationsItem();
        when(streamStorage.timelineItems(PAGE_SIZE))
                .thenReturn(Observable.<StreamPlayable>empty())
                .thenReturn(Observable.from(itemsWithPromoted));
        when(syncInitiator.sync(Syncable.SOUNDSTREAM)).thenReturn(Observable.just(successWithChange()));

        assertInitialStreamFirstItemKind(Kind.STATIONS_ONBOARDING);
    }

    @Test
    public void shouldShowUpsellAfterFirstUpsellableTrack() {
        final int upsellableItemIndex = 1;
        final List<StreamPlayable> streamItems = createItems(PAGE_SIZE, 123L);
        streamItems.add(upsellableItemIndex, upsellableStreamTrack);

        when(upsellOperations.shouldDisplayInStream()).thenReturn(true);
        when(streamStorage.timelineItems(PAGE_SIZE)).thenReturn(Observable.from(streamItems));

        assertStreamItemAtPosition(Kind.STREAM_UPSELL, upsellableItemIndex + 1);
    }

    @Test
    public void shouldNotShowUpsellWhenNoUpsellableTrackIsPresent() {
        final List<StreamPlayable> streamItems = createItems(PAGE_SIZE, 123L);

        when(upsellOperations.shouldDisplayInStream()).thenReturn(true);
        when(streamStorage.timelineItems(PAGE_SIZE)).thenReturn(Observable.from(streamItems));

        assertNoUpsellInStream();
    }

    @Test
    public void shouldNotShowUpsellAfterItWasDismissedByTheUser() {
        final int upsellableItemIndex = 1;
        final List<StreamPlayable> streamItems = createItems(PAGE_SIZE, 123L);
        streamItems.add(upsellableItemIndex, upsellableStreamTrack);

        when(upsellOperations.shouldDisplayInStream()).thenReturn(false);
        when(streamStorage.timelineItems(PAGE_SIZE)).thenReturn(Observable.from(streamItems));

        assertNoUpsellInStream();
    }

    @Test
    public void shouldShowUpsellOnlyAfterFirstUpsellableTrack() {
        final int firstUpsellableTrackIndex = 2;
        final int secondUpsellableTrackIndex = 4;
        final List<StreamPlayable> streamItems = createItems(PAGE_SIZE, 123L);
        streamItems.add(firstUpsellableTrackIndex, upsellableStreamTrack);
        streamItems.add(secondUpsellableTrackIndex, upsellableStreamTrack);

        final Urn trackUrnAfterSecondUpsellable = streamItems.get(secondUpsellableTrackIndex + 1)
                                                             .playableItem().getUrn();
        when(upsellOperations.shouldDisplayInStream()).thenReturn(true);
        when(streamStorage.timelineItems(PAGE_SIZE)).thenReturn(Observable.from(streamItems));

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
        final TestSubscriber<List<PlayableWithReposter>> subscriber = new TestSubscriber<>();
        final PlayableWithReposter playableWithReposter = PlayableWithReposter.from(Urn.forTrack(123L));
        when(streamStorage.playbackItems()).thenReturn(Observable.just(playableWithReposter));

        operations.urnsForPlayback().subscribe(subscriber);

        subscriber.assertReceivedOnNext(singletonList(singletonList(playableWithReposter)));
    }

    @Test
    public void shouldReturnNewItemsSinceTimestamp() {
        final TestSubscriber<Integer> subscriber = new TestSubscriber<>();
        when(streamStorage.timelineItemCountSince(123L)).thenReturn(Observable.just(3));

        operations.newItemsSince(123L).subscribe(subscriber);

        subscriber.assertValue(3);
    }

    @Test
    public void shouldUpdateStreamForStartWhenSyncedBefore() {
        final List<StreamPlayable> items = createItems(PAGE_SIZE, 123L);
        final List<StreamItem> viewModels = viewModelsFromStorageModel(items);

        when(syncStateStorage.hasSyncedBefore(Syncable.SOUNDSTREAM)).thenReturn(true);
        when(syncInitiator.sync(Syncable.SOUNDSTREAM)).thenReturn(Observable.just(successWithChange()));
        when(syncInitiator.sync(Syncable.SOUNDSTREAM, SyncInitiator.ACTION_HARD_REFRESH)).thenReturn(Observable.just(successWithChange()));
        when(storage.timelineItems(PAGE_SIZE)).thenReturn(from(items));

        operations.updatedTimelineItemsForStart().subscribe(subscriber);

        subscriber.assertValue(viewModels);
    }

    @Test
    public void shouldNotUpdateStreamForStartWhenNeverSyncedBefore() {
        when(syncStateStorage.hasSyncedBefore(Syncable.SOUNDSTREAM)).thenReturn(false);
        when(syncInitiator.sync(Syncable.SOUNDSTREAM, SyncInitiator.ACTION_HARD_REFRESH)).thenReturn(Observable.just(successWithChange()));

        operations.updatedTimelineItemsForStart().subscribe(subscriber);

        subscriber.assertNoValues();
    }

    @Test
    public void shouldInsertAdsOnInitialStreamLoad() {
        when(streamStorage.timelineItems(PAGE_SIZE))
                .thenReturn(Observable.<StreamPlayable>empty())
                .thenReturn(Observable.from(createItems(PAGE_SIZE, 123L)));
        when(syncInitiator.sync(Syncable.SOUNDSTREAM)).thenReturn(Observable.just(successWithChange()));

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
    protected List<StreamPlayable> createItems(int length, long lastItemTimestamp) {
        return super.createStorageModels(length, lastItemTimestamp);
    }

    @Override
    protected StreamPlayable createTimelineItem(long timestamp) {
        return TestPropertySets.timelineItem(new Date(timestamp));
    }

    @Override
    protected List<StreamItem> viewModelsFromStorageModel(List<StreamPlayable> dataItems) {
        final List<StreamItem> items = new ArrayList<>(dataItems.size());
        for (StreamPlayable source : dataItems) {
            items.add(StreamItem.fromStreamPlayable(source));
        }
        return items;
    }

    private void assertInitialStreamEmpty() {
        final TestSubscriber<List<StreamItem>> subscriber = subscribeToInitialStream();
        subscriber.assertValue(Collections.<StreamItem>emptyList());
    }

    private void assertInitialStreamFirstItemKind(Kind kind) {
        assertStreamItemAtPosition(kind, 0);
    }

    private void assertTrackStreamItemAtPosition(Urn urn, int index) {
        final TestSubscriber<List<StreamItem>> subscriber = subscribeToInitialStream();
        final TrackStreamItem firstItem = (TrackStreamItem) subscriber.getOnNextEvents().get(0).get(index);
        assertThat(firstItem.trackItem().getUrn()).isEqualTo(urn);
    }

    private void assertStreamItemAtPosition(Kind kind, int index) {
        final TestSubscriber<List<StreamItem>> subscriber = subscribeToInitialStream();
        final StreamItem firstItem = subscriber.getOnNextEvents().get(0).get(index);
        assertThat(firstItem.kind()).isEqualTo(kind);
    }

    private void assertNoUpsellInStream() {
        final TestSubscriber<List<StreamItem>> subscriber = subscribeToInitialStream();
        final List<StreamItem> stream = subscriber.getOnNextEvents().get(0);
        for (StreamItem item : stream) {
            assertThat(item.kind()).isNotEqualTo(Kind.STREAM_UPSELL);
        }
    }

    private TestSubscriber<List<StreamItem>> subscribeToInitialStream() {
        final TestSubscriber<List<StreamItem>> subscriber = new TestSubscriber<>();
        operations.initialStreamItems().subscribe(subscriber);
        return subscriber;
    }

    private List<StreamItem> initUpdatedTimelineItems() {
        when(syncInitiator.sync(Syncable.SOUNDSTREAM, SyncInitiator.ACTION_HARD_REFRESH))
                .thenReturn(Observable.just(successWithChange()));
        final List<StreamPlayable> items = createItems(PAGE_SIZE, 123L);
        items.add(0, promotedStreamTrack);

        final List<StreamItem> streamItemsWithPromoted = viewModelsFromStorageModel(items);
        when(streamStorage.timelineItems(PAGE_SIZE))
                .thenReturn(Observable.from(items));
        return streamItemsWithPromoted;
    }

    private void initSuggestedCreatorsItem() {
        when(suggestedCreatorsOperations.suggestedCreators()).thenReturn(Observable.just(SUGGESTED_CREATORS_ITEM));
    }

    private void initFacebookListenersItem() {
        when(facebookInvitesOperations.listenerInvites()).thenReturn(Observable.just(forFacebookListenerInvites()));
    }

    private void initFacebookCreatorsItem() {
        final StreamItem streamItem = forFacebookCreatorInvites(Urn.forTrack(1L), "url123");
        when(facebookInvitesOperations.creatorInvites()).thenReturn(Observable.just(streamItem));
    }

    private void initOnboardingStationsItem() {
        when(stationsOperations.onboardingStreamItem()).thenReturn(Observable.just(forStationOnboarding()));
    }
}
