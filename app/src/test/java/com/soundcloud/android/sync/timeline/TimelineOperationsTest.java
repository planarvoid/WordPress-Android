package com.soundcloud.android.sync.timeline;

import static com.soundcloud.android.testsupport.fixtures.TestSyncJobResults.successWithChange;
import static com.soundcloud.android.testsupport.fixtures.TestSyncJobResults.successWithoutChange;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static rx.Observable.from;

import com.soundcloud.android.Consts;
import com.soundcloud.android.sync.SyncInitiator;
import com.soundcloud.android.sync.SyncStateStorage;
import com.soundcloud.android.sync.Syncable;
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
public abstract class TimelineOperationsTest<StorageModel, ViewModel, StorageT extends TimelineStorage<StorageModel>>
        extends AndroidUnitTest {

    protected static final int PAGE_SIZE = Consts.LIST_PAGE_SIZE;
    private static final long FIRST_ITEM_TIMESTAMP = 1000L;
    protected Syncable syncable;
    protected TimelineOperations<ViewModel, StorageModel> operations;

    @Mock protected SyncInitiator syncInitiator;
    @Mock protected SyncStateStorage syncStateStorage;
    protected StorageT storage;
    protected TestSubscriber<List<ViewModel>> subscriber = new TestSubscriber<>();

    @Before
    public void setUpTimelineTest() throws Exception {
        storage = provideStorageMock();
        operations = buildOperations(storage, syncInitiator, Schedulers.immediate(), syncStateStorage);
        syncable = provideSyncable();
    }

    protected abstract TimelineOperations<ViewModel, StorageModel> buildOperations(StorageT storage,
                                                                                   SyncInitiator syncInitiator,
                                                                                   Scheduler scheduler,
                                                                                   SyncStateStorage syncStateStorage);

    protected abstract StorageT provideStorageMock();

    protected abstract Syncable provideSyncable();

    @Test
    public void shouldLoadFirstPageOfItemsFromLocalStorage() {
        final List<StorageModel> items = createItems(PAGE_SIZE, 2000L);
        when(storage.timelineItems(PAGE_SIZE)).thenReturn(from(items));

        operations.initialTimelineItems(false).subscribe(subscriber);

        subscriber.assertValue(viewModelsFromStorageModel(items));
    }

    @Test
    public void whenItLoadsAnEmptyPageOfItemsOnPageOneThenItRequestsAFullSyncAndReloads() {
        // 1st page comes back blank first, then as full page of items after sync
        final List<StorageModel> items = createItems(PAGE_SIZE, 2000L);
        when(storage.timelineItems(PAGE_SIZE))
                .thenReturn(Observable.<StorageModel>empty())
                .thenReturn(from(items));
        // returning true means new items have been added to local storage
        when(syncInitiator.sync(syncable)).thenReturn(Observable.just(successWithChange()));

        operations.initialTimelineItems(false).subscribe(subscriber);

        InOrder inOrder = inOrder(storage, syncInitiator);
        inOrder.verify(storage).timelineItems(PAGE_SIZE);
        inOrder.verify(syncInitiator).sync(syncable);
        inOrder.verify(storage).timelineItems(PAGE_SIZE);
        inOrder.verifyNoMoreInteractions();
        subscriber.assertValue(viewModelsFromStorageModel(items));
        subscriber.assertCompleted();
    }

    @Test // this makes sure we don't run into sync cycles
    public void whenItLoadsAnEmptyPageOfItemsOnPageOneEvenAfterAFullSyncItShouldNotSyncAgain() {
        // 1st page comes back blank first, then blank again even after syncing
        when(storage.timelineItems(PAGE_SIZE))
                .thenReturn(Observable.<StorageModel>empty())
                .thenReturn(Observable.<StorageModel>empty());
        // returning true means successful sync
        when(syncInitiator.sync(syncable)).thenReturn(Observable.just(successWithChange()));

        operations.initialTimelineItems(false).subscribe(subscriber);

        InOrder inOrder = inOrder(storage, syncInitiator);
        inOrder.verify(storage).timelineItems(PAGE_SIZE);
        inOrder.verify(syncInitiator).sync(syncable);
        inOrder.verify(storage).timelineItems(PAGE_SIZE);
        inOrder.verifyNoMoreInteractions();
        subscriber.assertValue(Collections.<ViewModel>emptyList());
        subscriber.assertCompleted();
    }

    @Test
    public void pagerLoadsNextPageUsingTimestampOfOldestItemOfPreviousPage() throws Exception {
        final List<StorageModel> items = createItems(PAGE_SIZE, 2000L);
        when(storage.timelineItemsBefore(2000L, PAGE_SIZE)).thenReturn(Observable.<StorageModel>never());

        operations.pagingFunction().call(viewModelsFromStorageModel(items));

        verify(storage).timelineItemsBefore(2000L, PAGE_SIZE);
    }

    @Test
    public void whenItLoadsAnEmptyPageOfItemsBeyondPageOneThenItRunsABackfillSyncAndReloads() {
        // 1st page is full page of items
        final List<StorageModel> firstPage = createItems(PAGE_SIZE, 123L);
        // 2nd page is blank on first attempt, then filled with items from backfill
        final List<StorageModel> secondPage = createItems(PAGE_SIZE, 456L);

        when(storage.timelineItemsBefore(123L, PAGE_SIZE))
                .thenReturn(Observable.<StorageModel>empty())
                .thenReturn(from(secondPage));
        // returning true means new items have been added to local storage
        when(syncInitiator.sync(syncable, SyncInitiator.ACTION_APPEND)).thenReturn(Observable.just(successWithChange()));

        operations.pagingFunction().call(viewModelsFromStorageModel(firstPage)).subscribe(subscriber);

        InOrder inOrder = inOrder(syncInitiator, storage);
        inOrder.verify(storage).timelineItemsBefore(123L, PAGE_SIZE);
        inOrder.verify(syncInitiator).sync(syncable, SyncInitiator.ACTION_APPEND);
        inOrder.verify(storage).timelineItemsBefore(123L, PAGE_SIZE);
        inOrder.verifyNoMoreInteractions();
        subscriber.assertValue(viewModelsFromStorageModel(secondPage));
        subscriber.assertCompleted();
    }

    @Test
    public void shouldStopPaginationIfBackfillSyncReportsNoNewItemsSynced() {
        // 1st page is full page of items
        final List<StorageModel> items = createItems(PAGE_SIZE, 123L);
        final List<ViewModel> viewModels = viewModelsFromStorageModel(items);
        // 2nd page is blank, will trigger backfill
        when(storage.timelineItemsBefore(123L, PAGE_SIZE)).thenReturn(Observable.<StorageModel>empty());
        // returning false means no new items have been added to local storage
        when(syncInitiator.sync(syncable, SyncInitiator.ACTION_APPEND)).thenReturn(Observable.just(successWithoutChange()));

        operations.pagingFunction().call(viewModels).subscribe(subscriber);

        InOrder inOrder = inOrder(storage, syncInitiator);
        inOrder.verify(storage).timelineItemsBefore(123L, PAGE_SIZE);
        inOrder.verify(syncInitiator).sync(syncable, SyncInitiator.ACTION_APPEND);
        inOrder.verifyNoMoreInteractions();
        subscriber.assertValue(Collections.<ViewModel>emptyList());
        subscriber.assertCompleted();
    }

    @Test
    public void refreshingShouldRequestSyncAndReloadFromLocalStorageWhenNewItemsAvailable() {
        when(syncInitiator.sync(syncable, SyncInitiator.ACTION_HARD_REFRESH)).thenReturn(Observable.just(successWithChange()));
        List<StorageModel> items = createItems(PAGE_SIZE, 123L);
        List<ViewModel> viewModels = viewModelsFromStorageModel(items);
        when(storage.timelineItems(PAGE_SIZE)).thenReturn(from(items));

        operations.updatedTimelineItems().subscribe(subscriber);

        InOrder inOrder = inOrder(storage, syncInitiator);
        inOrder.verify(syncInitiator).sync(syncable, SyncInitiator.ACTION_HARD_REFRESH);
        inOrder.verify(storage).timelineItems(PAGE_SIZE);
        inOrder.verifyNoMoreInteractions();
        subscriber.assertValue(viewModels);
        subscriber.assertCompleted();
    }

    @Test
    public void refreshingShouldRequestSyncAndCompleteWhenNoNewItemsAvailable() {
        when(syncInitiator.sync(syncable, SyncInitiator.ACTION_HARD_REFRESH)).thenReturn(Observable.just(successWithoutChange()));

        operations.updatedTimelineItems().subscribe(subscriber);

        verify(syncInitiator).sync(syncable, SyncInitiator.ACTION_HARD_REFRESH);
        verifyNoMoreInteractions(syncInitiator);
        verifyZeroInteractions(storage);
        subscriber.assertValue(Collections.<ViewModel>emptyList());
        subscriber.assertCompleted();
    }

    @Test
    public void getLastSyncReturnsLastTimestamp() {
        final TestSubscriber<Long> subscriber = new TestSubscriber<>();
        when(syncStateStorage.lastSyncTime(syncable)).thenReturn(123L);

        operations.lastSyncTime().subscribe(subscriber);

        subscriber.assertValue(123L);
    }

    @Test
    public void shouldReturnNewItemsSinceTimestamp() {
        final TestSubscriber<Integer> subscriber = new TestSubscriber<>();
        when(storage.timelineItemCountSince(123L)).thenReturn(Observable.just(3));

        operations.newItemsSince(123L).subscribe(subscriber);

        subscriber.assertValue(3);
    }

    @Test
    public void shouldNotUpdateStreamForStartWhenNeverSyncedBefore() {
        when(syncStateStorage.hasSyncedBefore(syncable)).thenReturn(false);
        when(syncInitiator.sync(syncable, SyncInitiator.ACTION_HARD_REFRESH)).thenReturn(Observable.just(successWithChange()));

        operations.updatedTimelineItemsForStart().subscribe(subscriber);

        subscriber.assertNoValues();
    }

    protected List<PropertySet> createPropertySets(int length, long lastItemTimestamp) {
        final List<PropertySet> headList = Collections.nCopies(length - 1, createTimelineItem(FIRST_ITEM_TIMESTAMP));
        final ArrayList<PropertySet> propertySets = new ArrayList<>(headList);
        propertySets.add(createTimelineItem(lastItemTimestamp));
        return propertySets;
    }

    protected abstract List<StorageModel> createItems(int length, long lastItemTimestamp);

    protected abstract PropertySet createTimelineItem(long timestamp);

    protected abstract List<ViewModel> viewModelsFromStorageModel(List<StorageModel> items);

}