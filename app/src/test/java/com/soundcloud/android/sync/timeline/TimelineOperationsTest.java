package com.soundcloud.android.sync.timeline;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static rx.Observable.from;

import com.soundcloud.android.Consts;
import com.soundcloud.android.api.legacy.model.ContentStats;
import com.soundcloud.android.api.model.Timestamped;
import com.soundcloud.android.sync.SyncContent;
import com.soundcloud.android.sync.SyncInitiator;
import com.soundcloud.android.sync.SyncStateStorage;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.java.collections.PropertySet;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import rx.Observable;
import rx.Scheduler;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// AndroidUnitTest because of PropertySet
public abstract class TimelineOperationsTest<ItemT extends Timestamped, StorageT extends TimelineStorage>
        extends AndroidUnitTest {

    protected static final int PAGE_SIZE = Consts.LIST_PAGE_SIZE;
    protected static final long FIRST_ITEM_TIMESTAMP = 1000L;
    protected SyncContent syncContent;
    protected TimelineOperations<ItemT> operations;

    @Mock protected SyncInitiator syncInitiator;
    @Mock protected ContentStats contentStats;
    @Mock protected SyncStateStorage syncStateStorage;
    protected StorageT storage;
    protected TestSubscriber<List<ItemT>> subscriber = new TestSubscriber<>();

    @Before
    public void setUpTimelineTest() throws Exception {
        storage = provideStorageMock();
        operations = buildOperations(storage, syncInitiator, contentStats, Schedulers.immediate(), syncStateStorage);
        syncContent = provideSyncContent();
    }

    protected abstract TimelineOperations<ItemT> buildOperations(StorageT storage,
                                                                 SyncInitiator syncInitiator,
                                                                 ContentStats contentStats,
                                                                 Scheduler scheduler,
                                                                 SyncStateStorage syncStateStorage);

    protected abstract StorageT provideStorageMock();

    protected abstract SyncContent provideSyncContent();

    @Test
    public void shouldLoadFirstPageOfItemsFromLocalStorage() {
        final List<PropertySet> items = createItems(PAGE_SIZE, 2000L);
        when(storage.timelineItems(PAGE_SIZE)).thenReturn(from(items));

        operations.initialTimelineItems(false).subscribe(subscriber);

        subscriber.assertValue(viewModelsFromPropertySets(items));
    }

    @Test
    public void initialPageLoadSetsLastSeenToDateOfTheNewestItem() {
        final List<PropertySet> items = createItems(PAGE_SIZE, 2000L);
        when(storage.timelineItems(PAGE_SIZE)).thenReturn(from(items));

        operations.initialTimelineItems(false).subscribe(subscriber);

        verify(contentStats).setLastSeen(syncContent.content, FIRST_ITEM_TIMESTAMP);
    }

    @Test
    public void whenItLoadsAnEmptyPageOfItemsOnPageOneThenItRequestsAFullSyncAndReloads() {
        // 1st page comes back blank first, then as full page of items after sync
        final List<PropertySet> items = createItems(PAGE_SIZE, 2000L);
        when(storage.timelineItems(PAGE_SIZE))
                .thenReturn(Observable.<PropertySet>empty())
                .thenReturn(from(items));
        // returning true means new items have been added to local storage
        when(syncInitiator.syncNewTimelineItems(syncContent)).thenReturn(Observable.just(true));

        operations.initialTimelineItems(false).subscribe(subscriber);

        InOrder inOrder = inOrder(storage, syncInitiator);
        inOrder.verify(storage).timelineItems(PAGE_SIZE);
        inOrder.verify(syncInitiator).syncNewTimelineItems(syncContent);
        inOrder.verify(storage).timelineItems(PAGE_SIZE);
        inOrder.verifyNoMoreInteractions();
        subscriber.assertValue(viewModelsFromPropertySets(items));
        subscriber.assertCompleted();
    }

    @Test // this makes sure we don't run into sync cycles
    public void whenItLoadsAnEmptyPageOfItemsOnPageOneEvenAfterAFullSyncItShouldNotSyncAgain() {
        // 1st page comes back blank first, then blank again even after syncing
        when(storage.timelineItems(PAGE_SIZE))
                .thenReturn(Observable.<PropertySet>empty())
                .thenReturn(Observable.<PropertySet>empty());
        // returning true means successful sync
        when(syncInitiator.syncNewTimelineItems(syncContent)).thenReturn(Observable.just(true));

        operations.initialTimelineItems(false).subscribe(subscriber);

        InOrder inOrder = inOrder(storage, syncInitiator);
        inOrder.verify(storage).timelineItems(PAGE_SIZE);
        inOrder.verify(syncInitiator).syncNewTimelineItems(syncContent);
        inOrder.verify(storage).timelineItems(PAGE_SIZE);
        inOrder.verifyNoMoreInteractions();
        subscriber.assertValue(Collections.<ItemT>emptyList());
        subscriber.assertCompleted();
    }

    @Test
    public void pagerLoadsNextPageUsingTimestampOfOldestItemOfPreviousPage() throws Exception {
        final List<PropertySet> items = createItems(PAGE_SIZE, 2000L);
        when(storage.timelineItemsBefore(2000L, PAGE_SIZE)).thenReturn(Observable.<PropertySet>never());

        operations.pagingFunction().call(viewModelsFromPropertySets(items));

        verify(storage).timelineItemsBefore(2000L, PAGE_SIZE);
    }

    @Test
    public void whenItLoadsAnEmptyPageOfItemsBeyondPageOneThenItRunsABackfillSyncAndReloads() {
        // 1st page is full page of items
        final List<PropertySet> firstPage = createItems(PAGE_SIZE, 123L);
        // 2nd page is blank on first attempt, then filled with items from backfill
        final List<PropertySet> secondPage = createItems(PAGE_SIZE, 456L);

        when(storage.timelineItemsBefore(123L, PAGE_SIZE))
                .thenReturn(Observable.<PropertySet>empty())
                .thenReturn(from(secondPage));
        // returning true means new items have been added to local storage
        when(syncInitiator.backfillTimelineItems(syncContent)).thenReturn(Observable.just(true));

        operations.pagingFunction().call(viewModelsFromPropertySets(firstPage)).subscribe(subscriber);

        InOrder inOrder = inOrder(syncInitiator, storage);
        inOrder.verify(storage).timelineItemsBefore(123L, PAGE_SIZE);
        inOrder.verify(syncInitiator).backfillTimelineItems(syncContent);
        inOrder.verify(storage).timelineItemsBefore(123L, PAGE_SIZE);
        inOrder.verifyNoMoreInteractions();
        subscriber.assertValue(viewModelsFromPropertySets(secondPage));
        subscriber.assertCompleted();
    }

    @Test
    public void shouldStopPaginationIfBackfillSyncReportsNoNewItemsSynced() {
        // 1st page is full page of items
        final List<PropertySet> items = createItems(PAGE_SIZE, 123L);
        final List<ItemT> viewModels = viewModelsFromPropertySets(items);
        // 2nd page is blank, will trigger backfill
        when(storage.timelineItemsBefore(123L, PAGE_SIZE)).thenReturn(Observable.<PropertySet>empty());
        // returning false means no new items have been added to local storage
        when(syncInitiator.backfillTimelineItems(syncContent)).thenReturn(Observable.just(false));

        operations.pagingFunction().call(viewModels).subscribe(subscriber);

        InOrder inOrder = inOrder(storage, syncInitiator);
        inOrder.verify(storage).timelineItemsBefore(123L, PAGE_SIZE);
        inOrder.verify(syncInitiator).backfillTimelineItems(syncContent);
        inOrder.verifyNoMoreInteractions();
        subscriber.assertValue(Collections.<ItemT>emptyList());
        subscriber.assertCompleted();
    }

    @Test
    public void refreshingShouldRequestSyncAndReloadFromLocalStorageWhenNewItemsAvailable() {
        when(syncInitiator.refreshTimelineItems(syncContent)).thenReturn(Observable.just(true));
        List<PropertySet> items = createItems(PAGE_SIZE, 123L);
        List<ItemT> viewModels = viewModelsFromPropertySets(items);
        when(storage.timelineItems(PAGE_SIZE)).thenReturn(from(items));

        operations.updatedTimelineItems().subscribe(subscriber);

        InOrder inOrder = inOrder(storage, syncInitiator);
        inOrder.verify(syncInitiator).refreshTimelineItems(syncContent);
        inOrder.verify(storage).timelineItems(PAGE_SIZE);
        inOrder.verifyNoMoreInteractions();
        subscriber.assertValue(viewModels);
        subscriber.assertCompleted();
    }

    @Test
    public void refreshingShouldRequestSyncAndCompleteWhenNoNewItemsAvailable() {
        when(syncInitiator.refreshTimelineItems(syncContent)).thenReturn(Observable.just(false));

        operations.updatedTimelineItems().subscribe(subscriber);

        verify(syncInitiator).refreshTimelineItems(syncContent);
        verifyNoMoreInteractions(syncInitiator);
        verifyZeroInteractions(storage);
        subscriber.assertValue(Collections.<ItemT>emptyList());
        subscriber.assertCompleted();
    }

    @Test
    public void getLastSyncReturnsLastTimestamp() {
        final TestSubscriber<Long> subscriber = new TestSubscriber<>();
        when(syncStateStorage.lastSyncTime(syncContent.content.uri)).thenReturn(Observable.just(123L));

        operations.lastSyncTime().subscribe(subscriber);

        subscriber.assertValue(123L);
    }

    protected List<PropertySet> createItems(int length, long lastItemTimestamp) {
        final List<PropertySet> headList = Collections.nCopies(length - 1, createTimelineItem(FIRST_ITEM_TIMESTAMP));
        final ArrayList<PropertySet> propertySets = new ArrayList<>(headList);
        propertySets.add(createTimelineItem(lastItemTimestamp));
        return propertySets;
    }

    protected abstract PropertySet createTimelineItem(long timestamp);

    protected abstract List<ItemT> viewModelsFromPropertySets(List<PropertySet> source);

}
