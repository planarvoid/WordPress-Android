package com.soundcloud.android.stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.legacy.model.ContentStats;
import com.soundcloud.android.api.model.ApiTrack;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PromotedTrackingEvent;
import com.soundcloud.android.events.StreamNotificationEvent;
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

    private TestEventBus eventBus = new TestEventBus();

    private final PropertySet promotedTrackProperties = TestPropertySets.expectedPromotedTrack();
    private final FacebookInvitesItem facebookInviteItem = new FacebookInvitesItem(Arrays.asList("url1", "url2"));

    @Before
    public void setUp() throws Exception {
        when(removeStalePromotedItemsCommand.toObservable(null)).thenReturn(Observable.just(Collections.<Long>emptyList()));
        when(facebookInvitesOperations.loadWithPictures())
                .thenReturn(Observable.just(Optional.<FacebookInvitesItem>absent()));
        this.operations = (SoundStreamOperations) super.operations;
    }

    @Override
    protected TimelineOperations<StreamItem> buildOperations(SoundStreamStorage storage, SyncInitiator syncInitiator,
                                                             ContentStats contentStats, Scheduler scheduler) {
        return new SoundStreamOperations(storage, syncInitiator, contentStats, removeStalePromotedItemsCommand,
                markPromotedItemAsStaleCommand, eventBus, scheduler, facebookInvitesOperations, stationsOperations,
                featureFlags);
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
                PlayableProperty.CREATED_AT.bind(new Date(ignoredDate))));

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
    public void shouldShowFacebookInvitesAsFirstItem() {
        final List<PropertySet> items = createItems(PAGE_SIZE, 123L);
        when(soundStreamStorage.timelineItems(PAGE_SIZE)).thenReturn(Observable.from(items));
        when(facebookInvitesOperations.loadWithPictures())
                .thenReturn(Observable.just(Optional.of(facebookInviteItem)));

        operations.initialStreamItems().subscribe(observer);

        verify(observer).onNext(itemsWithFacebookInvite(items));
        verify(observer).onCompleted();
    }

    @Test
    public void shouldShowFacebookInvitesAsFirstItemWithoutFriendPictures() {
        final List<PropertySet> items = createItems(PAGE_SIZE, 123L);
        when(soundStreamStorage.timelineItems(PAGE_SIZE)).thenReturn(Observable.from(items));
        when(facebookInvitesOperations.loadWithPictures())
                .thenReturn(Observable.just(Optional.of(facebookInviteItem)));

        operations.initialStreamItems().subscribe(observer);

        verify(observer).onNext(itemsWithFacebookInvite(items));
        verify(observer).onCompleted();
    }

    @Test
    public void shouldTrackFacebookInvitesShown() {
        final List<PropertySet> items = createItems(PAGE_SIZE, 123L);
        when(soundStreamStorage.timelineItems(PAGE_SIZE)).thenReturn(Observable.from(items));
        when(facebookInvitesOperations.loadWithPictures())
                .thenReturn(Observable.just(Optional.of(facebookInviteItem)));

        operations.initialStreamItems().subscribe(observer);

        assertThat(eventBus.lastEventOn(EventQueue.TRACKING)).isInstanceOf(StreamNotificationEvent.class);
    }

    @Test
    public void shouldNotShowFacebookInvitesOnEmptyStream() {
        when(facebookInvitesOperations.loadWithPictures())
                .thenReturn(Observable.just(Optional.of(facebookInviteItem)));
        when(soundStreamStorage.timelineItems(PAGE_SIZE))
                .thenReturn(Observable.<PropertySet>empty())
                .thenReturn(Observable.<PropertySet>empty());
        when(syncInitiator.syncNewTimelineItems(SYNC_CONTENT)).thenReturn(Observable.just(true));

        operations.initialStreamItems().subscribe(observer);

        verify(observer).onNext(Collections.<StreamItem>emptyList());
        verify(observer).onCompleted();
    }

    @Test
    public void shouldNotShowFacebookInvitesOnPromotedOnlyStream() {
        when(facebookInvitesOperations.loadWithPictures())
                .thenReturn(Observable.just(Optional.of(facebookInviteItem)));
        when(soundStreamStorage.timelineItems(PAGE_SIZE))
                .thenReturn(Observable.<PropertySet>empty())
                .thenReturn(Observable.just(promotedTrackProperties));
        when(syncInitiator.syncNewTimelineItems(SYNC_CONTENT)).thenReturn(Observable.just(true));

        operations.initialStreamItems().subscribe(observer);

        verify(observer).onNext(Collections.<StreamItem>emptyList());
        verify(observer).onCompleted();
    }

    @Test
    public void shouldShowFacebookInvitesAbovePromotedItems() {
        final List<PropertySet> itemsWithPromoted = createItems(PAGE_SIZE, 123L);
        itemsWithPromoted.add(0, promotedTrackProperties);

        when(facebookInvitesOperations.loadWithPictures())
                .thenReturn(Observable.just(Optional.of(facebookInviteItem)));
        when(soundStreamStorage.timelineItems(PAGE_SIZE))
                .thenReturn(Observable.<PropertySet>empty())
                .thenReturn(Observable.from(itemsWithPromoted));
        when(syncInitiator.syncNewTimelineItems(SYNC_CONTENT)).thenReturn(Observable.just(true));

        operations.initialStreamItems().subscribe(observer);

        verify(observer).onNext(itemsWithFacebookInvite(itemsWithPromoted));
        verify(observer).onCompleted();
    }

    @Test
    public void showStationsOnboardingAsFirstItem() {
        final List<PropertySet> items = createItems(PAGE_SIZE, 123L);
        when(featureFlags.isEnabled(Flag.STATIONS_SOFT_LAUNCH)).thenReturn(true);
        when(soundStreamStorage.timelineItems(PAGE_SIZE)).thenReturn(Observable.from(items));
        when(stationsOperations.shouldDisplayOnboardingStreamItem()).thenReturn(true);

        final TestSubscriber<List<StreamItem>> subscriber = new TestSubscriber<>();
        operations.initialStreamItems().subscribe(subscriber);

        final StreamItem firstItem = subscriber.getOnNextEvents().get(0).get(0);
        assertThat(firstItem.getEntityUrn()).isEqualTo(StationOnboardingStreamItem.URN);
    }

    @Test
    public void shouldNotShowStationsOnboardingOnEmptyStream() {
        when(stationsOperations.shouldDisplayOnboardingStreamItem()).thenReturn(true);
        when(soundStreamStorage.timelineItems(PAGE_SIZE))
                .thenReturn(Observable.<PropertySet>empty())
                .thenReturn(Observable.<PropertySet>empty());
        when(syncInitiator.syncNewTimelineItems(SYNC_CONTENT)).thenReturn(Observable.just(true));

        final TestSubscriber<List<StreamItem>> subscriber = new TestSubscriber<>();
        operations.initialStreamItems().subscribe(subscriber);

        subscriber.assertValue(Collections.<StreamItem>emptyList());
    }

    @Test
    public void shouldNotShowStationsOnboardingOnPromotedOnlyStream() {
        when(stationsOperations.shouldDisplayOnboardingStreamItem()).thenReturn(true);
        when(soundStreamStorage.timelineItems(PAGE_SIZE))
                .thenReturn(Observable.<PropertySet>empty())
                .thenReturn(Observable.just(promotedTrackProperties));
        when(syncInitiator.syncNewTimelineItems(SYNC_CONTENT)).thenReturn(Observable.just(true));

        final TestSubscriber<List<StreamItem>> subscriber = new TestSubscriber<>();
        operations.initialStreamItems().subscribe(subscriber);

        subscriber.assertValue(Collections.<StreamItem>emptyList());
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

        final TestSubscriber<List<StreamItem>> subscriber = new TestSubscriber<>();
        operations.initialStreamItems().subscribe(subscriber);

        final StreamItem firstItem = subscriber.getOnNextEvents().get(0).get(0);
        assertThat(firstItem.getEntityUrn()).isEqualTo(StationOnboardingStreamItem.URN);
    }

    private List<StreamItem> itemsWithFacebookInvite(List<PropertySet> items) {
        final List<StreamItem> streamItems = viewModelsFromPropertySets(items);
        streamItems.add(0, facebookInviteItem);
        return streamItems;
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
                PlayableProperty.CREATED_AT.bind(new Date(timestamp)));
    }
}
