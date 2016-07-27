package com.soundcloud.android.stream;

import static com.soundcloud.android.testsupport.fixtures.TestSyncJobResults.successWithChange;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static rx.Observable.from;

import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.FacebookInvitesEvent;
import com.soundcloud.android.events.PromotedTrackingEvent;
import com.soundcloud.android.facebookinvites.FacebookInvitesItem;
import com.soundcloud.android.facebookinvites.FacebookInvitesOperations;
import com.soundcloud.android.model.EntityProperty;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.PlayableItem;
import com.soundcloud.android.presentation.PromotedListItem;
import com.soundcloud.android.presentation.TypedListItem;
import com.soundcloud.android.upsell.UpsellListItem;
import com.soundcloud.android.stations.StationOnboardingStreamItem;
import com.soundcloud.android.stations.StationsOperations;
import com.soundcloud.android.sync.SyncInitiator;
import com.soundcloud.android.sync.SyncStateStorage;
import com.soundcloud.android.sync.Syncable;
import com.soundcloud.android.sync.timeline.TimelineOperations;
import com.soundcloud.android.sync.timeline.TimelineOperationsTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.android.tracks.TrackProperty;
import com.soundcloud.android.upsell.InlineUpsellOperations;
import com.soundcloud.java.collections.PropertySet;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.rx.eventbus.TestEventBus;
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

public class SoundStreamOperationsTest extends TimelineOperationsTest<TypedListItem, SoundStreamStorage> {

    private SoundStreamOperations operations;

    @Mock private SoundStreamStorage soundStreamStorage;
    @Mock private Observer<List<TypedListItem>> observer;
    @Mock private RemoveStalePromotedItemsCommand removeStalePromotedItemsCommand;
    @Mock private MarkPromotedItemAsStaleCommand markPromotedItemAsStaleCommand;
    @Mock private FacebookInvitesOperations facebookInvitesOperations;
    @Mock private StationsOperations stationsOperations;
    @Mock private InlineUpsellOperations upsellOperations;

    private TestEventBus eventBus = new TestEventBus();

    private final PropertySet promotedTrackProperties = TestPropertySets.expectedPromotedTrack();
    private final PropertySet upsellableTrackProperties = TestPropertySets.upsellableTrack();
    private final FacebookInvitesItem creatorInviteItem = new FacebookInvitesItem(FacebookInvitesItem.CREATOR_URN);

    @Before
    public void setUp() throws Exception {
        when(removeStalePromotedItemsCommand.toObservable(null)).thenReturn(Observable.just(Collections.<Long>emptyList()));
        when(facebookInvitesOperations.creatorInvites()).thenReturn(Observable.just(Optional.<FacebookInvitesItem>absent()));
        this.operations = (SoundStreamOperations) super.operations;
    }

    @Override
    protected TimelineOperations<TypedListItem> buildOperations(SoundStreamStorage storage,
                                                             SyncInitiator syncInitiator,
                                                             Scheduler scheduler,
                                                             SyncStateStorage syncStateStorage) {
        return new SoundStreamOperations(storage, syncInitiator, removeStalePromotedItemsCommand,
                                         markPromotedItemAsStaleCommand, eventBus, scheduler, facebookInvitesOperations,
                                         stationsOperations, upsellOperations, syncStateStorage);
    }

    @Override
    protected SoundStreamStorage provideStorageMock() {
        return soundStreamStorage;
    }

    @Override
    protected Syncable provideSyncable() {
        return Syncable.SOUNDSTREAM;
    }

    @Test
    public void streamIsConsideredEmptyWhenOnlyPromotedTrackIsReturnedAndDoesNotSyncAgain() {
        // 1st page comes back blank first, then includes promoted track only
        when(soundStreamStorage.timelineItems(PAGE_SIZE))
                .thenReturn(Observable.<PropertySet>empty())
                .thenReturn(Observable.just(promotedTrackProperties));
        // returning true means successful sync
        when(syncInitiator.sync(Syncable.SOUNDSTREAM)).thenReturn(Observable.just(successWithChange()));

        operations.initialStreamItems().subscribe(observer);

        InOrder inOrder = inOrder(observer, soundStreamStorage, syncInitiator);
        inOrder.verify(soundStreamStorage).timelineItems(PAGE_SIZE);
        inOrder.verify(syncInitiator).sync(Syncable.SOUNDSTREAM);
        inOrder.verify(soundStreamStorage).timelineItems(PAGE_SIZE);
        inOrder.verify(observer).onNext(Collections.<TypedListItem>emptyList());
        inOrder.verify(observer).onCompleted();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void initialStreamDeletesStalePromotedTracksBeforeLoadingStreamItems() {
        final PublishSubject<List<Long>> subject = PublishSubject.create();
        final AtomicBoolean verified = new AtomicBoolean();
        when(removeStalePromotedItemsCommand.toObservable(null)).thenReturn(subject);
        when(soundStreamStorage.timelineItems(PAGE_SIZE)).thenReturn(Observable.create(new Observable.OnSubscribe<PropertySet>() {
            @Override
            public void call(Subscriber<? super PropertySet> subscriber) {
                verified.set(subject.hasObservers());
            }
        }));

        operations.initialStreamItems().subscribe(observer);
        subject.onNext(Collections.<Long>emptyList());

        assertThat(verified.get()).isTrue();
    }

    @Test
    public void initialStreamWithPromotedTrackTriggersPromotedTrackImpression() {
        final List<PropertySet> itemsWithPromoted = createItems(PAGE_SIZE, 123L);
        itemsWithPromoted.add(0, promotedTrackProperties);

        final List<TypedListItem> streamItemsWithPromoted = viewModelsFromPropertySets(itemsWithPromoted);

        when(soundStreamStorage.timelineItems(PAGE_SIZE))
                .thenReturn(Observable.<PropertySet>empty())
                .thenReturn(Observable.from(itemsWithPromoted));
        when(syncInitiator.sync(Syncable.SOUNDSTREAM)).thenReturn(Observable.just(successWithChange()));

        operations.initialStreamItems().subscribe(observer);

        assertThat(eventBus.eventsOn(EventQueue.TRACKING)).hasSize(1);
        assertThat(eventBus.lastEventOn(EventQueue.TRACKING)).isInstanceOf(PromotedTrackingEvent.class);
        verify(markPromotedItemAsStaleCommand).call((PromotedListItem) streamItemsWithPromoted.get(0));
    }

    @Test
    public void updatedItemsStreamWithPromotedTrackTriggersPromotedTrackImpression() {
        when(syncInitiator.sync(Syncable.SOUNDSTREAM, SyncInitiator.ACTION_HARD_REFRESH))
                .thenReturn(Observable.just(successWithChange()));
        final List<PropertySet> items = createItems(PAGE_SIZE, 123L);
        items.add(0, promotedTrackProperties);

        final List<TypedListItem> streamItemsWithPromoted = viewModelsFromPropertySets(items);
        when(soundStreamStorage.timelineItems(PAGE_SIZE))
                .thenReturn(Observable.from(items));

        operations.updatedStreamItems().subscribe(observer);

        assertThat(eventBus.lastEventOn(EventQueue.TRACKING)).isInstanceOf(PromotedTrackingEvent.class);
        verify(markPromotedItemAsStaleCommand).call((PromotedListItem) streamItemsWithPromoted.get(0));
    }

    @Test
    public void shouldShowFacebookListenerInvitesAsFirstItem() {
        final List<PropertySet> items = createItems(PAGE_SIZE, 123L);
        when(soundStreamStorage.timelineItems(PAGE_SIZE)).thenReturn(Observable.from(items));
        when(facebookInvitesOperations.canShowForListeners()).thenReturn(true);

        assertInitialStreamFirstItemUrn(FacebookInvitesItem.LISTENER_URN);
    }

    @Test
    public void shouldShowFacebookCreatorInvitesAsFirstItem() {
        final List<PropertySet> items = createItems(PAGE_SIZE, 123L);
        when(soundStreamStorage.timelineItems(PAGE_SIZE)).thenReturn(Observable.from(items));
        when(facebookInvitesOperations.canShowForListeners()).thenReturn(true);
        when(facebookInvitesOperations.creatorInvites()).thenReturn(Observable.just(Optional.of(creatorInviteItem)));

        assertInitialStreamFirstItemUrn(FacebookInvitesItem.CREATOR_URN);
    }

    @Test
    public void shouldTrackFacebookCreatorInvitesShown() {
        final List<PropertySet> items = createItems(PAGE_SIZE, 123L);
        when(soundStreamStorage.timelineItems(PAGE_SIZE)).thenReturn(Observable.from(items));
        when(facebookInvitesOperations.creatorInvites()).thenReturn(Observable.just(Optional.of(creatorInviteItem)));

        operations.initialStreamItems().subscribe(observer);

        assertThat(eventBus.lastEventOn(EventQueue.TRACKING)).isInstanceOf(FacebookInvitesEvent.class);
    }

    @Test
    public void shouldNotShowFacebookInvitesOnEmptyStream() {
        when(facebookInvitesOperations.canShowForListeners()).thenReturn(true);
        when(soundStreamStorage.timelineItems(PAGE_SIZE))
                .thenReturn(Observable.<PropertySet>empty())
                .thenReturn(Observable.<PropertySet>empty());
        when(syncInitiator.sync(Syncable.SOUNDSTREAM)).thenReturn(Observable.just(successWithChange()));

        assertInitialStreamEmpty();
    }

    @Test
    public void shouldNotShowFacebookInvitesOnPromotedOnlyStream() {
        when(facebookInvitesOperations.canShowForListeners()).thenReturn(true);
        when(soundStreamStorage.timelineItems(PAGE_SIZE))
                .thenReturn(Observable.<PropertySet>empty())
                .thenReturn(Observable.just(promotedTrackProperties));
        when(syncInitiator.sync(Syncable.SOUNDSTREAM)).thenReturn(Observable.just(successWithChange()));

        assertInitialStreamEmpty();
    }

    @Test
    public void shouldShowFacebookInvitesAbovePromotedItems() {
        final List<PropertySet> itemsWithPromoted = createItems(PAGE_SIZE, 123L);
        itemsWithPromoted.add(0, promotedTrackProperties);

        when(facebookInvitesOperations.canShowForListeners()).thenReturn(true);
        when(soundStreamStorage.timelineItems(PAGE_SIZE))
                .thenReturn(Observable.<PropertySet>empty())
                .thenReturn(Observable.from(itemsWithPromoted));
        when(syncInitiator.sync(Syncable.SOUNDSTREAM)).thenReturn(Observable.just(successWithChange()));

        assertInitialStreamFirstItemUrn(FacebookInvitesItem.LISTENER_URN);
    }

    @Test
    public void showStationsOnboardingAsFirstItem() {
        final List<PropertySet> items = createItems(PAGE_SIZE, 123L);
        when(soundStreamStorage.timelineItems(PAGE_SIZE)).thenReturn(Observable.from(items));
        when(stationsOperations.shouldDisplayOnboardingStreamItem()).thenReturn(true);

        assertInitialStreamFirstItemUrn(StationOnboardingStreamItem.URN);
    }

    @Test
    public void shouldNotShowStationsOnboardingOnEmptyStream() {
        when(stationsOperations.shouldDisplayOnboardingStreamItem()).thenReturn(true);
        when(soundStreamStorage.timelineItems(PAGE_SIZE))
                .thenReturn(Observable.<PropertySet>empty())
                .thenReturn(Observable.<PropertySet>empty());
        when(syncInitiator.sync(Syncable.SOUNDSTREAM)).thenReturn(Observable.just(successWithChange()));

        assertInitialStreamEmpty();
    }

    @Test
    public void shouldNotShowStationsOnboardingOnPromotedOnlyStream() {
        when(stationsOperations.shouldDisplayOnboardingStreamItem()).thenReturn(true);
        when(soundStreamStorage.timelineItems(PAGE_SIZE))
                .thenReturn(Observable.<PropertySet>empty())
                .thenReturn(Observable.just(promotedTrackProperties));
        when(syncInitiator.sync(Syncable.SOUNDSTREAM)).thenReturn(Observable.just(successWithChange()));

        assertInitialStreamEmpty();
    }

    @Test
    public void shouldShowStationsOnboardingAbovePromotedItems() {
        final List<PropertySet> itemsWithPromoted = createItems(PAGE_SIZE, 123L);
        itemsWithPromoted.add(0, promotedTrackProperties);

        when(stationsOperations.shouldDisplayOnboardingStreamItem()).thenReturn(true);
        when(soundStreamStorage.timelineItems(PAGE_SIZE))
                .thenReturn(Observable.<PropertySet>empty())
                .thenReturn(Observable.from(itemsWithPromoted));
        when(syncInitiator.sync(Syncable.SOUNDSTREAM)).thenReturn(Observable.just(successWithChange()));

        assertInitialStreamFirstItemUrn(StationOnboardingStreamItem.URN);
    }

    @Test
    public void shouldShowUpsellAfterFirstUpsellableTrack() {
        final int upsellableItemIndex = 1;
        final List<PropertySet> streamItems = createItems(PAGE_SIZE, 123L);
        streamItems.add(upsellableItemIndex, upsellableTrackProperties);

        when(upsellOperations.shouldDisplayInStream()).thenReturn(true);
        when(soundStreamStorage.timelineItems(PAGE_SIZE)).thenReturn(Observable.from(streamItems));

        assertStreamItemAtPosition(UpsellListItem.STREAM_UPSELL_URN, upsellableItemIndex + 1);
    }

    @Test
    public void shouldNotShowUpsellWhenNoUpsellableTrackIsPresent() {
        final List<PropertySet> streamItems = createItems(PAGE_SIZE, 123L);

        when(upsellOperations.shouldDisplayInStream()).thenReturn(true);
        when(soundStreamStorage.timelineItems(PAGE_SIZE)).thenReturn(Observable.from(streamItems));

        assertNoUpsellInStream();
    }

    @Test
    public void shouldNotShowUpsellAfterItWasDismissedByTheUser() {
        final int upsellableItemIndex = 1;
        final List<PropertySet> streamItems = createItems(PAGE_SIZE, 123L);
        streamItems.add(upsellableItemIndex, upsellableTrackProperties);

        when(upsellOperations.shouldDisplayInStream()).thenReturn(false);
        when(soundStreamStorage.timelineItems(PAGE_SIZE)).thenReturn(Observable.from(streamItems));

        assertNoUpsellInStream();
    }

    @Test
    public void shouldShowUpsellOnlyAfterFirstUpsellableTrack() {
        final int firstUpsellableTrackIndex = 2;
        final int secondUpsellableTrackIndex = 4;
        final List<PropertySet> streamItems = createItems(PAGE_SIZE, 123L);
        streamItems.add(firstUpsellableTrackIndex, upsellableTrackProperties);
        streamItems.add(secondUpsellableTrackIndex, upsellableTrackProperties);

        final Urn trackUrnAfterSecondUpsellable = streamItems.get(secondUpsellableTrackIndex + 1)
                                                             .get(TrackProperty.URN);
        when(upsellOperations.shouldDisplayInStream()).thenReturn(true);
        when(soundStreamStorage.timelineItems(PAGE_SIZE)).thenReturn(Observable.from(streamItems));

        assertStreamItemAtPosition(trackUrnAfterSecondUpsellable, secondUpsellableTrackIndex + 2);
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
        final TestSubscriber<List<PropertySet>> subscriber = new TestSubscriber<>();
        final PropertySet propertySet = PropertySet.from(EntityProperty.URN.bind(Urn.forTrack(123)));
        when(soundStreamStorage.playbackItems()).thenReturn(Observable.just(propertySet));

        operations.urnsForPlayback().subscribe(subscriber);

        subscriber.assertReceivedOnNext(singletonList(singletonList(propertySet)));
    }

    @Test
    public void shouldReturnNewItemsSinceTimestamp() {
        final TestSubscriber<Integer> subscriber = new TestSubscriber<>();
        when(soundStreamStorage.timelineItemCountSince(123L)).thenReturn(Observable.just(3));

        operations.newItemsSince(123L).subscribe(subscriber);

        subscriber.assertValue(3);
    }

    @Test
    public void shouldUpdateStreamForStartWhenSyncedBefore() {
        final List<PropertySet> items = createItems(PAGE_SIZE, 123L);
        final List<TypedListItem> viewModels = viewModelsFromPropertySets(items);

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

    @Override
    protected List<TypedListItem> viewModelsFromPropertySets(List<PropertySet> propertySets) {
        final List<TypedListItem> items = new ArrayList<>(propertySets.size());
        for (PropertySet source : propertySets) {
            if (source.get(EntityProperty.URN).isPlayable()) {
                items.add(PlayableItem.from(source));
            }
        }
        return items;
    }

    @Override
    protected PropertySet createTimelineItem(long timestamp) {
        return PropertySet.from(
                PlayableProperty.URN.bind(ModelFixtures.create(ApiTrack.class).getUrn()),
                PlayableProperty.CREATED_AT.bind(new Date(timestamp)));
    }

    private void assertInitialStreamEmpty() {
        final TestSubscriber<List<TypedListItem>> subscriber = subscribeToInitialStream();
        subscriber.assertValue(Collections.<TypedListItem>emptyList());
    }

    private void assertInitialStreamFirstItemUrn(Urn urn) {
        assertStreamItemAtPosition(urn, 0);
    }

    private void assertStreamItemAtPosition(Urn urn, int index) {
        final TestSubscriber<List<TypedListItem>> subscriber = subscribeToInitialStream();
        final TypedListItem firstItem = subscriber.getOnNextEvents().get(0).get(index);
        assertThat(firstItem.getUrn()).isEqualTo(urn);
    }

    private void assertNoUpsellInStream() {
        final TestSubscriber<List<TypedListItem>> subscriber = subscribeToInitialStream();
        final List<TypedListItem> stream = subscriber.getOnNextEvents().get(0);
        for (TypedListItem item : stream) {
            assertThat(item.getKind()).isNotEqualTo(TypedListItem.Kind.UPSELL);
        }
    }

    private TestSubscriber<List<TypedListItem>> subscribeToInitialStream() {
        final TestSubscriber<List<TypedListItem>> subscriber = new TestSubscriber<>();
        operations.initialStreamItems().subscribe(subscriber);
        return subscriber;
    }
}
