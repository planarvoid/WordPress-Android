package com.soundcloud.android.stream;

import static com.soundcloud.android.stream.SoundStreamOperations.PAGE_SIZE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import com.soundcloud.android.api.legacy.model.ContentStats;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PromotedTrackEvent;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.rx.TestEventBus;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.sync.SyncInitiator;
import com.soundcloud.android.testsupport.PlatformUnitTest;
import com.soundcloud.android.testsupport.fixtures.TestPropertySets;
import com.soundcloud.android.tracks.PromotedTrackProperty;
import com.soundcloud.propeller.PropertySet;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import rx.Observable;
import rx.Observer;
import rx.Subscriber;
import rx.schedulers.Schedulers;
import rx.subjects.PublishSubject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class SoundStreamOperationsTest extends PlatformUnitTest {

    private static final long TIMESTAMP = 1000L;

    private SoundStreamOperations operations;

    @Mock private SoundStreamStorage soundStreamStorage;
    @Mock private SyncInitiator syncInitiator;
    @Mock private Observer<List<PropertySet>> observer;
    @Mock private ContentStats contentStats;
    @Mock private RemoveStalePromotedTracksCommand removeStalePromotedTracksCommand;

    private TestEventBus eventBus = new TestEventBus();

    private final PropertySet promotedTrackProperties = TestPropertySets.expectedPromotedTrack();

    @Before
    public void setUp() throws Exception {
        when(removeStalePromotedTracksCommand.toObservable(null)).thenReturn(Observable.just(Collections.<Long>emptyList()));
        operations = new SoundStreamOperations(soundStreamStorage, syncInitiator, contentStats,
                removeStalePromotedTracksCommand, eventBus, Schedulers.immediate());
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
                PromotedTrackProperty.AD_URN.bind("adswizz:ad:123"),
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
                .thenReturn(Observable.<PropertySet>empty(), Observable.from(items));
        // returning true means new items have been added to local storage
        when(syncInitiator.initialSoundStream()).thenReturn(Observable.just(true));

        operations.initialStreamItems().subscribe(observer);

        InOrder inOrder = inOrder(observer, soundStreamStorage, syncInitiator);
        inOrder.verify(soundStreamStorage).initialStreamItems(PAGE_SIZE);
        inOrder.verify(syncInitiator).initialSoundStream();
        inOrder.verify(soundStreamStorage).initialStreamItems(PAGE_SIZE);
        inOrder.verify(observer).onNext(items);
        inOrder.verify(observer).onCompleted();
        inOrder.verifyNoMoreInteractions();
    }

    @Test // this makes sure we don't run into sync cycles
    public void whenItLoadsAnEmptyPageOfItemsOnPageOneEvenAfterAFullSyncItShouldNotSyncAgain() {
        // 1st page comes back blank first, then blank again even after syncing
        when(soundStreamStorage.initialStreamItems(PAGE_SIZE))
                .thenReturn(Observable.<PropertySet>empty(), Observable.<PropertySet>empty());
        // returning true means successful sync
        when(syncInitiator.initialSoundStream()).thenReturn(Observable.just(true));

        operations.initialStreamItems().subscribe(observer);

        InOrder inOrder = inOrder(observer, soundStreamStorage, syncInitiator);
        inOrder.verify(soundStreamStorage).initialStreamItems(PAGE_SIZE);
        inOrder.verify(syncInitiator).initialSoundStream();
        inOrder.verify(soundStreamStorage).initialStreamItems(PAGE_SIZE);
        inOrder.verify(observer).onNext(Collections.<PropertySet>emptyList());
        inOrder.verify(observer).onCompleted();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void streamIsConsideredEmptyWhenOnlyPromotedTrackIsReturnedAndDoesNotSyncAgain() {
        // 1st page comes back blank first, then includes promoted track only
        when(soundStreamStorage.initialStreamItems(PAGE_SIZE))
                .thenReturn(Observable.<PropertySet>empty(), Observable.just(promotedTrackProperties));
        // returning true means successful sync
        when(syncInitiator.initialSoundStream()).thenReturn(Observable.just(true));

        operations.initialStreamItems().subscribe(observer);

        InOrder inOrder = inOrder(observer, soundStreamStorage, syncInitiator);
        inOrder.verify(soundStreamStorage).initialStreamItems(PAGE_SIZE);
        inOrder.verify(syncInitiator).initialSoundStream();
        inOrder.verify(soundStreamStorage).initialStreamItems(PAGE_SIZE);
        inOrder.verify(observer).onNext(Collections.<PropertySet>emptyList());
        inOrder.verify(observer).onCompleted();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void pagerLoadsNextPageUsingTimestampOfOldestItemOfPreviousPage() throws Exception {
        final List<PropertySet> items = createItems(PAGE_SIZE, 123L);
        when(soundStreamStorage.streamItemsBefore(123L, PAGE_SIZE))
                .thenReturn(Observable.<PropertySet>never());

        operations.pagingFunction().call(items);

        verify(soundStreamStorage).streamItemsBefore(123L, PAGE_SIZE);
    }

    @Test
    public void whenItLoadsAnEmptyPageOfItemsBeyondPageOneThenItRunsABackfillSyncAndReloads() {
        // 1st page is full page of items
        final List<PropertySet> firstPage = createItems(PAGE_SIZE, 123L);
        // 2nd page is blank on first attempt, then filled with items from backfill
        final List<PropertySet> secondPage = createItems(PAGE_SIZE, 456L);
        when(soundStreamStorage.streamItemsBefore(123L, PAGE_SIZE))
                .thenReturn(Observable.<PropertySet>empty(), Observable.from(secondPage));
        // returning true means new items have been added to local storage
        when(syncInitiator.backfillSoundStream()).thenReturn(Observable.just(true));

        operations.pagingFunction().call(firstPage).subscribe(observer);

        InOrder inOrder = inOrder(observer, syncInitiator, soundStreamStorage);
        inOrder.verify(soundStreamStorage).streamItemsBefore(123L, PAGE_SIZE);
        inOrder.verify(syncInitiator).backfillSoundStream();
        inOrder.verify(soundStreamStorage).streamItemsBefore(123L, PAGE_SIZE);
        inOrder.verify(observer).onNext(secondPage);
        inOrder.verify(observer).onCompleted();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void shouldStopPaginationIfBackfillSyncReportsNoNewItemsSynced() {
        // 1st page is full page of items
        final List<PropertySet> items = createItems(PAGE_SIZE, 123L);
        // 2nd page is blank, will trigger backfill
        when(soundStreamStorage.streamItemsBefore(123L, PAGE_SIZE))
                .thenReturn(Observable.<PropertySet>empty());
        // returning false means no new items have been added to local storage
        when(syncInitiator.backfillSoundStream()).thenReturn(Observable.just(false));

        operations.pagingFunction().call(items).subscribe(observer);

        InOrder inOrder = inOrder(soundStreamStorage, syncInitiator, observer);
        inOrder.verify(soundStreamStorage).streamItemsBefore(123L, PAGE_SIZE);
        inOrder.verify(syncInitiator).backfillSoundStream();
        inOrder.verify(observer).onNext(Collections.<PropertySet>emptyList());
        inOrder.verify(observer).onCompleted();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void shouldRequestSoundStreamSyncAndReloadFromLocalStorageWhenNewItemsAvailable() {
        when(syncInitiator.refreshSoundStream())
                .thenReturn(Observable.just(true));
        final List<PropertySet> items = createItems(PAGE_SIZE, 123L);
        when(soundStreamStorage.initialStreamItems(PAGE_SIZE))
                .thenReturn(Observable.from(items));

        operations.updatedStreamItems().subscribe(observer);

        InOrder inOrder = inOrder(soundStreamStorage, syncInitiator, observer);
        inOrder.verify(syncInitiator).refreshSoundStream();
        inOrder.verify(soundStreamStorage).initialStreamItems(PAGE_SIZE);
        inOrder.verify(observer).onNext(items);
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
        inOrder.verify(observer).onNext(Collections.<PropertySet>emptyList());
        inOrder.verify(observer).onCompleted();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void initialStreamDeletesStalePromotedTracksBeforeLoadingStreamItems() {
        final PublishSubject<List<Long>> subject = PublishSubject.create();
        final AtomicBoolean verified = new AtomicBoolean();
        when(removeStalePromotedTracksCommand.toObservable(null)).thenReturn(subject);
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

        when(soundStreamStorage.initialStreamItems(PAGE_SIZE))
                .thenReturn(Observable.<PropertySet>empty(), Observable.from(itemsWithPromoted));
        when(syncInitiator.initialSoundStream()).thenReturn(Observable.just(true));

        operations.initialStreamItems().subscribe(observer);

        assertThat(eventBus.eventsOn(EventQueue.TRACKING)).hasSize(1);
        assertThat(eventBus.lastEventOn(EventQueue.TRACKING)).isInstanceOf(PromotedTrackEvent.class);
    }

    @Test
    public void updatedItemsStreamWithPromotedTrackTriggersPromotedTrackImpression() {
        when(syncInitiator.refreshSoundStream())
                .thenReturn(Observable.just(true));
        final List<PropertySet> items = createItems(PAGE_SIZE, 123L);
        items.add(0, promotedTrackProperties);
        when(soundStreamStorage.initialStreamItems(PAGE_SIZE))
                .thenReturn(Observable.from(items));

        operations.updatedStreamItems().subscribe(observer);

        assertThat(eventBus.lastEventOn(EventQueue.TRACKING)).isInstanceOf(PromotedTrackEvent.class);
    }

    private List<PropertySet> createItems(int length, long timestampOfLastItem) {
        final List<PropertySet> headList = Collections.nCopies(length - 1, PropertySet.from(
                PlayableProperty.URN.bind(Urn.forTrack(1L)),
                PlayableProperty.CREATED_AT.bind(new Date(TIMESTAMP))));
        final PropertySet lastItem = PropertySet.from(
                PlayableProperty.URN.bind(Urn.forTrack(1L)),
                PlayableProperty.CREATED_AT.bind(new Date(timestampOfLastItem)));
        final ArrayList<PropertySet> propertySets = Lists.newArrayList(headList);
        propertySets.add(lastItem);
        return propertySets;
    }

}
