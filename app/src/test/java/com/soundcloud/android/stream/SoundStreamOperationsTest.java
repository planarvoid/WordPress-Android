package com.soundcloud.android.stream;

import static com.soundcloud.android.stream.SoundStreamOperations.PAGE_SIZE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.legacy.model.ContentStats;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PromotedTrackingEvent;
import com.soundcloud.android.events.StreamNotificationEvent;
import com.soundcloud.android.facebookinvites.FacebookInvitesItem;
import com.soundcloud.android.facebookinvites.FacebookInvitesOperations;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.PromotedItemProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.PlayableItem;
import com.soundcloud.android.presentation.PromotedListItem;
import com.soundcloud.android.stations.StationOnboardingStreamItem;
import com.soundcloud.android.stations.StationsOperations;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.sync.SyncInitiator;
import com.soundcloud.android.testsupport.AndroidUnitTest;
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
import rx.Subscriber;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;

import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class SoundStreamOperationsTest extends AndroidUnitTest {

    private static final long TIMESTAMP = 1000L;

    private SoundStreamOperations operations;

    @Mock private SoundStreamStorage soundStreamStorage;
    @Mock private SyncInitiator syncInitiator;
    @Mock private Observer<List<StreamItem>> observer;
    @Mock private ContentStats contentStats;
    @Mock private RemoveStalePromotedItemsCommand removeStalePromotedItemsCommand;
    @Mock private MarkPromotedItemAsStaleCommand markPromotedItemAsStaleCommand;
    @Mock private FacebookInvitesOperations facebookInvitesOperations;
    @Mock private StationsOperations stationsOperations;

    private TestEventBus eventBus = new TestEventBus();

    private final PropertySet promotedTrackProperties = TestPropertySets.expectedPromotedTrack();
    private final FacebookInvitesItem facebookInviteItem = new FacebookInvitesItem(Arrays.asList("url1", "url2"));
    private final StationOnboardingStreamItem stationOnboardingStreamItem = new StationOnboardingStreamItem();

    @Before
    public void setUp() throws Exception {
        when(removeStalePromotedItemsCommand.toObservable(null)).thenReturn(Observable.just(Collections.<Long>emptyList()));
        when(facebookInvitesOperations.loadWithPictures())
                .thenReturn(Observable.just(Optional.<FacebookInvitesItem>absent()));

        operations = new SoundStreamOperations(
                soundStreamStorage,
                syncInitiator,
                contentStats,
                removeStalePromotedItemsCommand,
                markPromotedItemAsStaleCommand,
                eventBus,
                Schedulers.immediate(),
                facebookInvitesOperations,
                stationsOperations
        );
    }

    @Test
    public void initialStreamItemsLoadsPageOne() {
        when(soundStreamStorage.initialStreamItems(PAGE_SIZE))
                .thenReturn(Observable.from(createItems(PAGE_SIZE, 123L)));

        operations.initialStreamItems().subscribe(observer);

        verify(soundStreamStorage).initialStreamItems(PAGE_SIZE);
    }

    @Test
    public void initialStreamSetsLastSeen() {
        final List<PropertySet> items = createItems(PAGE_SIZE, 123L);
        when(soundStreamStorage.initialStreamItems(PAGE_SIZE))
                .thenReturn(Observable.from(items));

        operations.initialStreamItems().subscribe(observer);

        verify(contentStats).setLastSeen(Content.ME_SOUND_STREAM, TIMESTAMP);
    }

    @Test
    public void initialStreamIgnoresPromotedItemWhenItSetsLastSeen() {
        final List<PropertySet> items = createItems(PAGE_SIZE - 1, 123L);
        final long ignoredDate = Long.MAX_VALUE - 1;
        items.add(0, PropertySet.from(
                PlayableProperty.URN.bind(Urn.forTrack(12345L)),
                PromotedItemProperty.AD_URN.bind("adswizz:ad:123"),
                PlayableProperty.CREATED_AT.bind(new Date(ignoredDate))));

        when(soundStreamStorage.initialStreamItems(PAGE_SIZE))
                .thenReturn(Observable.from(items));

        operations.initialStreamItems().subscribe(observer);
        verify(contentStats).setLastSeen(Content.ME_SOUND_STREAM, TIMESTAMP);
    }

    @Test
    public void whenItLoadsAnEmptyPageOfItemsOnPageOneThenItRequestsAFullSyncAndReloads() {
        // 1st page comes back blank first, then as full page of items after sync
        final List<PropertySet> items = createItems(PAGE_SIZE, 123L);
        when(soundStreamStorage.initialStreamItems(PAGE_SIZE))
                .thenReturn(Observable.<PropertySet>empty())
                .thenReturn(Observable.from(items));
        // returning true means new items have been added to local storage
        when(syncInitiator.initialSoundStream()).thenReturn(Observable.just(true));

        operations.initialStreamItems().subscribe(observer);

        InOrder inOrder = inOrder(observer, soundStreamStorage, syncInitiator);
        inOrder.verify(soundStreamStorage).initialStreamItems(PAGE_SIZE);
        inOrder.verify(syncInitiator).initialSoundStream();
        inOrder.verify(soundStreamStorage).initialStreamItems(PAGE_SIZE);
        inOrder.verify(observer).onNext(streamItemsFromPropertySets(items));
        inOrder.verify(observer).onCompleted();
        inOrder.verifyNoMoreInteractions();
    }

    @Test // this makes sure we don't run into sync cycles
    public void whenItLoadsAnEmptyPageOfItemsOnPageOneEvenAfterAFullSyncItShouldNotSyncAgain() {
        // 1st page comes back blank first, then blank again even after syncing
        when(soundStreamStorage.initialStreamItems(PAGE_SIZE))
                .thenReturn(Observable.<PropertySet>empty())
                .thenReturn(Observable.<PropertySet>empty());
        // returning true means successful sync
        when(syncInitiator.initialSoundStream()).thenReturn(Observable.just(true));

        operations.initialStreamItems().subscribe(observer);

        InOrder inOrder = inOrder(observer, soundStreamStorage, syncInitiator);
        inOrder.verify(soundStreamStorage).initialStreamItems(PAGE_SIZE);
        inOrder.verify(syncInitiator).initialSoundStream();
        inOrder.verify(soundStreamStorage).initialStreamItems(PAGE_SIZE);
        inOrder.verify(observer).onNext(Collections.<StreamItem>emptyList());
        inOrder.verify(observer).onCompleted();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void streamIsConsideredEmptyWhenOnlyPromotedTrackIsReturnedAndDoesNotSyncAgain() {
        // 1st page comes back blank first, then includes promoted track only
        when(soundStreamStorage.initialStreamItems(PAGE_SIZE))
                .thenReturn(Observable.<PropertySet>empty())
                .thenReturn(Observable.just(promotedTrackProperties));
        // returning true means successful sync
        when(syncInitiator.initialSoundStream()).thenReturn(Observable.just(true));

        operations.initialStreamItems().subscribe(observer);

        InOrder inOrder = inOrder(observer, soundStreamStorage, syncInitiator);
        inOrder.verify(soundStreamStorage).initialStreamItems(PAGE_SIZE);
        inOrder.verify(syncInitiator).initialSoundStream();
        inOrder.verify(soundStreamStorage).initialStreamItems(PAGE_SIZE);
        inOrder.verify(observer).onNext(Collections.<StreamItem>emptyList());
        inOrder.verify(observer).onCompleted();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void pagerLoadsNextPageUsingTimestampOfOldestItemOfPreviousPage() throws Exception {
        final List<PropertySet> items = createItems(PAGE_SIZE, 123L);
        when(soundStreamStorage.streamItemsBefore(123L, PAGE_SIZE))
                .thenReturn(Observable.<PropertySet>never());

        operations.pagingFunction().call(streamItemsFromPropertySets(items));

        verify(soundStreamStorage).streamItemsBefore(123L, PAGE_SIZE);
    }

    @Test
    public void whenItLoadsAnEmptyPageOfItemsBeyondPageOneThenItRunsABackfillSyncAndReloads() {
        // 1st page is full page of items
        final List<PropertySet> firstPage = createItems(PAGE_SIZE, 123L);
        // 2nd page is blank on first attempt, then filled with items from backfill
        final List<PropertySet> secondPage = createItems(PAGE_SIZE, 456L);

        when(soundStreamStorage.streamItemsBefore(123L, PAGE_SIZE))
                .thenReturn(Observable.<PropertySet>empty())
                .thenReturn(Observable.from(secondPage));
        // returning true means new items have been added to local storage
        when(syncInitiator.backfillSoundStream()).thenReturn(Observable.just(true));

        operations.pagingFunction().call(streamItemsFromPropertySets(firstPage)).subscribe(observer);

        InOrder inOrder = inOrder(observer, syncInitiator, soundStreamStorage);
        inOrder.verify(soundStreamStorage).streamItemsBefore(123L, PAGE_SIZE);
        inOrder.verify(syncInitiator).backfillSoundStream();
        inOrder.verify(soundStreamStorage).streamItemsBefore(123L, PAGE_SIZE);
        inOrder.verify(observer).onNext(streamItemsFromPropertySets(secondPage));
        inOrder.verify(observer).onCompleted();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void shouldStopPaginationIfBackfillSyncReportsNoNewItemsSynced() {
        // 1st page is full page of items
        final List<PropertySet> items = createItems(PAGE_SIZE, 123L);
        final List<StreamItem> streamItems = streamItemsFromPropertySets(items);
        // 2nd page is blank, will trigger backfill
        when(soundStreamStorage.streamItemsBefore(123L, PAGE_SIZE))
                .thenReturn(Observable.<PropertySet>empty());
        // returning false means no new items have been added to local storage
        when(syncInitiator.backfillSoundStream()).thenReturn(Observable.just(false));

        operations.pagingFunction().call(streamItems).subscribe(observer);

        InOrder inOrder = inOrder(soundStreamStorage, syncInitiator, observer);
        inOrder.verify(soundStreamStorage).streamItemsBefore(123L, PAGE_SIZE);
        inOrder.verify(syncInitiator).backfillSoundStream();
        inOrder.verify(observer).onNext(Collections.<StreamItem>emptyList());
        inOrder.verify(observer).onCompleted();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void shouldRequestSoundStreamSyncAndReloadFromLocalStorageWhenNewItemsAvailable() {
        when(syncInitiator.refreshSoundStream())
                .thenReturn(Observable.just(true));
        final List<PropertySet> items = createItems(PAGE_SIZE, 123L);
        final List<StreamItem> streamItems = streamItemsFromPropertySets(items);

        when(soundStreamStorage.initialStreamItems(PAGE_SIZE))
                .thenReturn(Observable.from(items));

        operations.updatedStreamItems().subscribe(observer);

        InOrder inOrder = inOrder(soundStreamStorage, syncInitiator, observer);
        inOrder.verify(syncInitiator).refreshSoundStream();
        inOrder.verify(soundStreamStorage).initialStreamItems(PAGE_SIZE);
        inOrder.verify(observer).onNext(streamItems);
        inOrder.verify(observer).onCompleted();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void shouldRequestSoundStreamSyncAndCompleteWhenNoNewItemsAvailable() {
        when(syncInitiator.refreshSoundStream())
                .thenReturn(Observable.just(false));

        operations.updatedStreamItems().subscribe(observer);

        InOrder inOrder = inOrder(soundStreamStorage, syncInitiator, observer);
        inOrder.verify(syncInitiator).refreshSoundStream();
        inOrder.verify(observer).onNext(Collections.<StreamItem>emptyList());
        inOrder.verify(observer).onCompleted();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void initialStreamDeletesStalePromotedTracksBeforeLoadingStreamItems() {
        final PublishSubject<List<Long>> subject = PublishSubject.create();
        final AtomicBoolean verified = new AtomicBoolean();
        when(removeStalePromotedItemsCommand.toObservable(null)).thenReturn(subject);
        when(soundStreamStorage.initialStreamItems(PAGE_SIZE)).thenReturn(Observable.create(new Observable.OnSubscribe<PropertySet>() {
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

        final List<StreamItem> streamItemsWithPromoted = streamItemsFromPropertySets(itemsWithPromoted);

        when(soundStreamStorage.initialStreamItems(PAGE_SIZE))
                .thenReturn(Observable.<PropertySet>empty())
                .thenReturn(Observable.from(itemsWithPromoted));
        when(syncInitiator.initialSoundStream()).thenReturn(Observable.just(true));

        operations.initialStreamItems().subscribe(observer);

        assertThat(eventBus.eventsOn(EventQueue.TRACKING)).hasSize(1);
        assertThat(eventBus.lastEventOn(EventQueue.TRACKING)).isInstanceOf(PromotedTrackingEvent.class);
        verify(markPromotedItemAsStaleCommand).call((PromotedListItem) streamItemsWithPromoted.get(0));
    }

    @Test
    public void updatedItemsStreamWithPromotedTrackTriggersPromotedTrackImpression() {
        when(syncInitiator.refreshSoundStream())
                .thenReturn(Observable.just(true));
        final List<PropertySet> items = createItems(PAGE_SIZE, 123L);
        items.add(0, promotedTrackProperties);

        final List<StreamItem> streamItemsWithPromoted = streamItemsFromPropertySets(items);
        when(soundStreamStorage.initialStreamItems(PAGE_SIZE))
                .thenReturn(Observable.from(items));

        operations.updatedStreamItems().subscribe(observer);

        assertThat(eventBus.lastEventOn(EventQueue.TRACKING)).isInstanceOf(PromotedTrackingEvent.class);
        verify(markPromotedItemAsStaleCommand).call((PromotedListItem) streamItemsWithPromoted.get(0));
    }

    @Test
    public void shouldShowFacebookInvitesAsFirstItem() {
        final List<PropertySet> items = createItems(PAGE_SIZE, 123L);
        when(soundStreamStorage.initialStreamItems(PAGE_SIZE)).thenReturn(Observable.from(items));
        when(facebookInvitesOperations.loadWithPictures())
                .thenReturn(Observable.just(Optional.of(facebookInviteItem)));

        operations.initialStreamItems().subscribe(observer);

        verify(observer).onNext(itemsWithInvites(items));
        verify(observer).onCompleted();
    }

    @Test
    public void shouldShowFacebookInvitesAsFirstItemWithoutFriendPictures() {
        final List<PropertySet> items = createItems(PAGE_SIZE, 123L);
        when(soundStreamStorage.initialStreamItems(PAGE_SIZE)).thenReturn(Observable.from(items));
        when(facebookInvitesOperations.loadWithPictures())
                .thenReturn(Observable.just(Optional.of(facebookInviteItem)));

        operations.initialStreamItems().subscribe(observer);

        verify(observer).onNext(itemsWithInvites(items));
        verify(observer).onCompleted();
    }

    @Test
    public void shouldTrackFacebookInvitesShown() {
        final List<PropertySet> items = createItems(PAGE_SIZE, 123L);
        when(soundStreamStorage.initialStreamItems(PAGE_SIZE)).thenReturn(Observable.from(items));
        when(facebookInvitesOperations.loadWithPictures())
                .thenReturn(Observable.just(Optional.of(facebookInviteItem)));

        operations.initialStreamItems().subscribe(observer);

        assertThat(eventBus.lastEventOn(EventQueue.TRACKING)).isInstanceOf(StreamNotificationEvent.class);
    }

    @Test
    public void shouldNotShowFacebookInvitesOnEmptyStream() {
        when(facebookInvitesOperations.loadWithPictures())
                .thenReturn(Observable.just(Optional.of(facebookInviteItem)));
        when(soundStreamStorage.initialStreamItems(PAGE_SIZE))
                .thenReturn(Observable.<PropertySet>empty())
                .thenReturn(Observable.<PropertySet>empty());
        when(syncInitiator.initialSoundStream()).thenReturn(Observable.just(true));

        operations.initialStreamItems().subscribe(observer);

        verify(observer).onNext(Collections.<StreamItem>emptyList());
        verify(observer).onCompleted();
    }

    @Test
    public void shouldNotShowFacebookInvitesOnPromotedOnlyStream() {
        when(facebookInvitesOperations.loadWithPictures())
                .thenReturn(Observable.just(Optional.of(facebookInviteItem)));
        when(soundStreamStorage.initialStreamItems(PAGE_SIZE))
                .thenReturn(Observable.<PropertySet>empty())
                .thenReturn(Observable.just(promotedTrackProperties));
        when(syncInitiator.initialSoundStream()).thenReturn(Observable.just(true));

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
        when(soundStreamStorage.initialStreamItems(PAGE_SIZE))
                .thenReturn(Observable.<PropertySet>empty())
                .thenReturn(Observable.from(itemsWithPromoted));
        when(syncInitiator.initialSoundStream()).thenReturn(Observable.just(true));

        operations.initialStreamItems().subscribe(observer);

        verify(observer).onNext(itemsWithInvites(itemsWithPromoted));
        verify(observer).onCompleted();
    }

    @Test
    public void showStationsOnboardingAsFirstItem() {
        final List<PropertySet> items = createItems(PAGE_SIZE, 123L);
        when(soundStreamStorage.initialStreamItems(PAGE_SIZE)).thenReturn(Observable.from(items));
        when(stationsOperations.shouldDisplayOnboardingStreamItem()).thenReturn(true);

        final TestSubscriber<List<StreamItem>> subscriber = new TestSubscriber<>();
        operations.initialStreamItems().subscribe(subscriber);

        final StreamItem firstItem = subscriber.getOnNextEvents().get(0).get(0);
        assertThat(firstItem.getEntityUrn()).isEqualTo(StationOnboardingStreamItem.URN);
    }

    @Test
    public void shouldNotShowStationsOnboardingOnEmptyStream() {
        when(stationsOperations.shouldDisplayOnboardingStreamItem()).thenReturn(true);
        when(soundStreamStorage.initialStreamItems(PAGE_SIZE))
                .thenReturn(Observable.<PropertySet>empty())
                .thenReturn(Observable.<PropertySet>empty());
        when(syncInitiator.initialSoundStream()).thenReturn(Observable.just(true));

        final TestSubscriber<List<StreamItem>> subscriber = new TestSubscriber<>();
        operations.initialStreamItems().subscribe(subscriber);

        subscriber.assertValue(Collections.<StreamItem>emptyList());
    }

    @Test
    public void shouldNotShowStationsOnboardingOnPromotedOnlyStream() {
        when(stationsOperations.shouldDisplayOnboardingStreamItem()).thenReturn(true);
        when(soundStreamStorage.initialStreamItems(PAGE_SIZE))
                .thenReturn(Observable.<PropertySet>empty())
                .thenReturn(Observable.just(promotedTrackProperties));
        when(syncInitiator.initialSoundStream()).thenReturn(Observable.just(true));

        final TestSubscriber<List<StreamItem>> subscriber = new TestSubscriber<>();
        operations.initialStreamItems().subscribe(subscriber);

        subscriber.assertValue(Collections.<StreamItem>emptyList());
    }

    @Test
    public void shouldShowStationsOnboardingAbovePromotedItems() {
        final List<PropertySet> itemsWithPromoted = createItems(PAGE_SIZE, 123L);
        itemsWithPromoted.add(0, promotedTrackProperties);

        when(stationsOperations.shouldDisplayOnboardingStreamItem()).thenReturn(true);
        when(soundStreamStorage.initialStreamItems(PAGE_SIZE))
                .thenReturn(Observable.<PropertySet>empty())
                .thenReturn(Observable.from(itemsWithPromoted));
        when(syncInitiator.initialSoundStream()).thenReturn(Observable.just(true));

        final TestSubscriber<List<StreamItem>> subscriber = new TestSubscriber<>();
        operations.initialStreamItems().subscribe(subscriber);

        final StreamItem firstItem = subscriber.getOnNextEvents().get(0).get(0);
        assertThat(firstItem.getEntityUrn()).isEqualTo(StationOnboardingStreamItem.URN);
    }

    private List<PropertySet> createItems(int length, long timestampOfLastItem) {
        final List<PropertySet> headList = Collections.nCopies(length - 1, PropertySet.from(
                PlayableProperty.URN.bind(Urn.forTrack(1L)),
                PlayableProperty.CREATED_AT.bind(new Date(TIMESTAMP))));
        final PropertySet lastItem = PropertySet.from(
                PlayableProperty.URN.bind(Urn.forTrack(1L)),
                PlayableProperty.CREATED_AT.bind(new Date(timestampOfLastItem)));
        final ArrayList<PropertySet> propertySets = new ArrayList<>(headList);
        propertySets.add(lastItem);
        return propertySets;
    }

    private List<StreamItem> streamItemsFromPropertySets(List<PropertySet> items) {
        List<StreamItem> streamItems = new ArrayList<>(items.size());

        for (PropertySet item : items) {
            streamItems.add(PlayableItem.from(item));
        }

        return streamItems;
    }

    private List<StreamItem> itemsWithInvites(List<PropertySet> items) {
        return getStreamItems(items, facebookInviteItem);
    }

    private List<StreamItem> itemsStationsOnboarding(List<PropertySet> items) {
        return getStreamItems(items, stationOnboardingStreamItem);
    }

    @NonNull
    private List<StreamItem> getStreamItems(List<PropertySet> items, StreamItem streamItem) {
        final List<StreamItem> streamItems = streamItemsFromPropertySets(items);
        streamItems.add(0, streamItem);
        return streamItems;
    }
}
