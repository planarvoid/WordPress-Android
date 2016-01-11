package com.soundcloud.android.stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.legacy.model.ContentStats;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.FacebookInvitesEvent;
import com.soundcloud.android.events.PromotedTrackingEvent;
import com.soundcloud.android.facebookinvites.FacebookInvitesItem;
import com.soundcloud.android.facebookinvites.FacebookInvitesOperations;
import com.soundcloud.android.model.EntityProperty;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.PromotedItemProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.PlayableItem;
import com.soundcloud.android.presentation.PromotedListItem;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.stations.StationOnboardingStreamItem;
import com.soundcloud.android.stations.StationsOperations;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.sync.SyncContent;
import com.soundcloud.android.sync.SyncInitiator;
import com.soundcloud.android.sync.timeline.TimelineOperations;
import com.soundcloud.android.sync.timeline.TimelineOperationsTest;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.android.tracks.TrackProperty;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class SoundStreamOperationsTest extends TimelineOperationsTest<StreamItem, SoundStreamStorage> {

    private static final SyncContent SYNC_CONTENT = SyncContent.MySoundStream;

    private SoundStreamOperations operations;

    @Mock private SoundStreamStorage soundStreamStorage;
    @Mock private Observer<List<StreamItem>> observer;
    @Mock private RemoveStalePromotedItemsCommand removeStalePromotedItemsCommand;
    @Mock private MarkPromotedItemAsStaleCommand markPromotedItemAsStaleCommand;
    @Mock private FacebookInvitesOperations facebookInvitesOperations;
    @Mock private StationsOperations stationsOperations;
    @Mock private FeatureFlags featureFlags;
    @Mock private UpsellOperations upsellOperations;

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
    protected TimelineOperations<StreamItem> buildOperations(SoundStreamStorage storage, SyncInitiator syncInitiator,
                                                             ContentStats contentStats, Scheduler scheduler) {
        return new SoundStreamOperations(storage, syncInitiator, contentStats, removeStalePromotedItemsCommand,
                markPromotedItemAsStaleCommand, eventBus, scheduler, facebookInvitesOperations,
                stationsOperations, upsellOperations, featureFlags);
    }

    @Override
    protected SoundStreamStorage provideStorageMock() {
        return soundStreamStorage;
    }

    @Override
    protected SyncContent provideSyncContent() {
        return SYNC_CONTENT;
    }

    @Test
    public void initialStreamIgnoresPromotedItemWhenItSetsLastSeenTimestamp() {
        final List<PropertySet> items = createItems(PAGE_SIZE - 1, 123L);
        final long ignoredDate = Long.MAX_VALUE - 1;
        items.add(0, PropertySet.from(
                PlayableProperty.URN.bind(Urn.forTrack(12345L)),
                PromotedItemProperty.AD_URN.bind("adswizz:ad:123"),
                SoundStreamProperty.CREATED_AT.bind(new Date(ignoredDate))));

        when(soundStreamStorage.timelineItems(PAGE_SIZE)).thenReturn(Observable.from(items));

        operations.initialStreamItems().subscribe(observer);

        verify(contentStats).setLastSeen(Content.ME_SOUND_STREAM, FIRST_ITEM_TIMESTAMP);
    }

    @Test
    public void streamIsConsideredEmptyWhenOnlyPromotedTrackIsReturnedAndDoesNotSyncAgain() {
        // 1st page comes back blank first, then includes promoted track only
        when(soundStreamStorage.timelineItems(PAGE_SIZE))
                .thenReturn(Observable.<PropertySet>empty())
                .thenReturn(Observable.just(promotedTrackProperties));
        // returning true means successful sync
        when(syncInitiator.syncNewTimelineItems(SYNC_CONTENT)).thenReturn(Observable.just(true));

        operations.initialStreamItems().subscribe(observer);

        InOrder inOrder = inOrder(observer, soundStreamStorage, syncInitiator);
        inOrder.verify(soundStreamStorage).timelineItems(PAGE_SIZE);
        inOrder.verify(syncInitiator).syncNewTimelineItems(SYNC_CONTENT);
        inOrder.verify(soundStreamStorage).timelineItems(PAGE_SIZE);
        inOrder.verify(observer).onNext(Collections.<StreamItem>emptyList());
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

        final List<StreamItem> streamItemsWithPromoted = viewModelsFromPropertySets(itemsWithPromoted);

        when(soundStreamStorage.timelineItems(PAGE_SIZE))
                .thenReturn(Observable.<PropertySet>empty())
                .thenReturn(Observable.from(itemsWithPromoted));
        when(syncInitiator.syncNewTimelineItems(SYNC_CONTENT)).thenReturn(Observable.just(true));

        operations.initialStreamItems().subscribe(observer);

        assertThat(eventBus.eventsOn(EventQueue.TRACKING)).hasSize(1);
        assertThat(eventBus.lastEventOn(EventQueue.TRACKING)).isInstanceOf(PromotedTrackingEvent.class);
        verify(markPromotedItemAsStaleCommand).call((PromotedListItem) streamItemsWithPromoted.get(0));
    }

    @Test
    public void updatedItemsStreamWithPromotedTrackTriggersPromotedTrackImpression() {
        when(syncInitiator.refreshTimelineItems(SYNC_CONTENT))
                .thenReturn(Observable.just(true));
        final List<PropertySet> items = createItems(PAGE_SIZE, 123L);
        items.add(0, promotedTrackProperties);

        final List<StreamItem> streamItemsWithPromoted = viewModelsFromPropertySets(items);
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
        when(syncInitiator.syncNewTimelineItems(SYNC_CONTENT)).thenReturn(Observable.just(true));

        assertInitialStreamEmpty();
    }

    @Test
    public void shouldNotShowFacebookInvitesOnPromotedOnlyStream() {
        when(facebookInvitesOperations.canShowForListeners()).thenReturn(true);
        when(soundStreamStorage.timelineItems(PAGE_SIZE))
                .thenReturn(Observable.<PropertySet>empty())
                .thenReturn(Observable.just(promotedTrackProperties));
        when(syncInitiator.syncNewTimelineItems(SYNC_CONTENT)).thenReturn(Observable.just(true));

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
        when(syncInitiator.syncNewTimelineItems(SYNC_CONTENT)).thenReturn(Observable.just(true));

        assertInitialStreamFirstItemUrn(FacebookInvitesItem.LISTENER_URN);
    }

    @Test
    public void showStationsOnboardingAsFirstItem() {
        final List<PropertySet> items = createItems(PAGE_SIZE, 123L);
        when(featureFlags.isEnabled(Flag.STATIONS_SOFT_LAUNCH)).thenReturn(true);
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
        when(syncInitiator.syncNewTimelineItems(SYNC_CONTENT)).thenReturn(Observable.just(true));

        assertInitialStreamEmpty();
    }

    @Test
    public void shouldNotShowStationsOnboardingOnPromotedOnlyStream() {
        when(stationsOperations.shouldDisplayOnboardingStreamItem()).thenReturn(true);
        when(soundStreamStorage.timelineItems(PAGE_SIZE))
                .thenReturn(Observable.<PropertySet>empty())
                .thenReturn(Observable.just(promotedTrackProperties));
        when(syncInitiator.syncNewTimelineItems(SYNC_CONTENT)).thenReturn(Observable.just(true));

        assertInitialStreamEmpty();
    }

    @Test
    public void shouldShowStationsOnboardingAbovePromotedItems() {
        final List<PropertySet> itemsWithPromoted = createItems(PAGE_SIZE, 123L);
        itemsWithPromoted.add(0, promotedTrackProperties);

        when(featureFlags.isEnabled(Flag.STATIONS_SOFT_LAUNCH)).thenReturn(true);
        when(stationsOperations.shouldDisplayOnboardingStreamItem()).thenReturn(true);
        when(soundStreamStorage.timelineItems(PAGE_SIZE))
                .thenReturn(Observable.<PropertySet>empty())
                .thenReturn(Observable.from(itemsWithPromoted));
        when(syncInitiator.syncNewTimelineItems(SYNC_CONTENT)).thenReturn(Observable.just(true));

        assertInitialStreamFirstItemUrn(StationOnboardingStreamItem.URN);
    }

    @Test
    public void shouldShowUpsellAfterFirstUpsellableTrack() {
        final int upsellableItemIndex = 1;
        final List<PropertySet> streamItems = createItems(PAGE_SIZE, 123L);
        streamItems.add(upsellableItemIndex, upsellableTrackProperties);

        when(featureFlags.isEnabled(Flag.UPSELL_IN_STREAM)).thenReturn(true);
        when(upsellOperations.canDisplayUpsellInStream()).thenReturn(true);
        when(soundStreamStorage.timelineItems(PAGE_SIZE)).thenReturn(Observable.from(streamItems));

        assertStreamItemAtPosition(UpsellNotificationItem.URN, upsellableItemIndex + 1);
    }

    @Test
    public void shouldNotShowUpsellWhenNoUpsellableTrackIsPresent() {
        final List<PropertySet> streamItems = createItems(PAGE_SIZE, 123L);

        when(featureFlags.isEnabled(Flag.UPSELL_IN_STREAM)).thenReturn(true);
        when(upsellOperations.canDisplayUpsellInStream()).thenReturn(true);
        when(soundStreamStorage.timelineItems(PAGE_SIZE)).thenReturn(Observable.from(streamItems));

        assertNoUpsellInStream();
    }

    @Test
    public void shouldNotShowUpsellAfterItWasDismissedByTheUser() {
        final int upsellableItemIndex = 1;
        final List<PropertySet> streamItems = createItems(PAGE_SIZE, 123L);
        streamItems.add(upsellableItemIndex, upsellableTrackProperties);

        when(featureFlags.isEnabled(Flag.UPSELL_IN_STREAM)).thenReturn(true);
        when(upsellOperations.canDisplayUpsellInStream()).thenReturn(false);
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

        final Urn trackUrnAfterSecondUpsellable = streamItems.get(secondUpsellableTrackIndex + 1).get(TrackProperty.URN);
        when(featureFlags.isEnabled(Flag.UPSELL_IN_STREAM)).thenReturn(true);
        when(upsellOperations.canDisplayUpsellInStream()).thenReturn(true);
        when(soundStreamStorage.timelineItems(PAGE_SIZE)).thenReturn(Observable.from(streamItems));

        assertStreamItemAtPosition(trackUrnAfterSecondUpsellable, secondUpsellableTrackIndex + 2);
    }

    @Test
    public void shouldDisableUpsell() {
        operations.disableUpsell();

        verify(upsellOperations).disableUpsell();
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

        subscriber.assertReceivedOnNext(Arrays.asList(Arrays.asList(propertySet)));
    }

    @Override
    protected List<StreamItem> viewModelsFromPropertySets(List<PropertySet> propertySets) {
        final List<StreamItem> items = new ArrayList<>(propertySets.size());
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
                SoundStreamProperty.CREATED_AT.bind(new Date(timestamp)));
    }

    private void assertInitialStreamEmpty() {
        final TestSubscriber<List<StreamItem>> subscriber = subscribeToInitialStream();
        subscriber.assertValue(Collections.<StreamItem>emptyList());
    }

    private void assertInitialStreamFirstItemUrn(Urn urn) {
        assertStreamItemAtPosition(urn, 0);
    }

    private void assertStreamItemAtPosition(Urn urn, int index) {
        final TestSubscriber<List<StreamItem>> subscriber = subscribeToInitialStream();
        final StreamItem firstItem = subscriber.getOnNextEvents().get(0).get(index);
        assertThat(firstItem.getEntityUrn()).isEqualTo(urn);
    }

    private void assertNoUpsellInStream() {
        final TestSubscriber<List<StreamItem>> subscriber = subscribeToInitialStream();
        final List<StreamItem> stream = subscriber.getOnNextEvents().get(0);
        for (StreamItem item : stream) {
            assertThat(item.getKind()).isNotEqualTo(StreamItem.Kind.UPSELL);
        }
    }

    private TestSubscriber<List<StreamItem>> subscribeToInitialStream() {
        final TestSubscriber<List<StreamItem>> subscriber = new TestSubscriber<>();
        operations.initialStreamItems().subscribe(subscriber);
        return subscriber;
    }
}
