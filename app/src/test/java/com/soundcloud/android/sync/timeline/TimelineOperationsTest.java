package com.soundcloud.android.sync.timeline;

import static com.soundcloud.android.testsupport.fixtures.TestSyncJobResults.successWithChange;
import static com.soundcloud.android.testsupport.fixtures.TestSyncJobResults.successWithoutChange;
import static io.reactivex.Observable.empty;
import static io.reactivex.Observable.fromIterable;
import static io.reactivex.Observable.just;
import static io.reactivex.Observable.never;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.Consts;
import com.soundcloud.android.sync.SyncInitiator;
import com.soundcloud.android.sync.SyncStateStorage;
import com.soundcloud.android.sync.Syncable;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import io.reactivex.Scheduler;
import io.reactivex.Single;
import io.reactivex.observers.TestObserver;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import rx.observers.AssertableSubscriber;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class TimelineOperationsTest<StorageModel, ViewModel, StorageT extends TimelineStorage<StorageModel>>
        extends AndroidUnitTest {

    protected static final int PAGE_SIZE = Consts.LIST_PAGE_SIZE;
    private static final long FIRST_ITEM_TIMESTAMP = 1000L;
    public static final long LAST_ITEM_TIMESTAMP = 2000L;
    protected Syncable syncable;
    protected TimelineOperations<StorageModel, ViewModel> operations;

    @Mock protected SyncInitiator syncInitiator;
    @Mock protected SyncStateStorage syncStateStorage;
    protected StorageT storage;

    @Before
    public void setUpTimelineTest() throws Exception {
        storage = provideStorageMock();
        operations = buildOperations(storage, syncInitiator, io.reactivex.schedulers.Schedulers.trampoline(), syncStateStorage);
        syncable = provideSyncable();
        when(storage.timelineItemsBefore(anyLong(), anyInt())).thenReturn(empty());
        when(storage.timelineItems(anyInt())).thenReturn(empty());
    }

    protected abstract TimelineOperations<StorageModel, ViewModel> buildOperations(StorageT storage,
                                                                                   SyncInitiator syncInitiator,
                                                                                   Scheduler scheduler,
                                                                                   SyncStateStorage syncStateStorage);

    protected abstract StorageT provideStorageMock();

    protected abstract Syncable provideSyncable();

    @Test
    public void shouldLoadFirstPageOfItemsFromLocalStorage() {
        final List<StorageModel> items = createItems(PAGE_SIZE, LAST_ITEM_TIMESTAMP);
        final List<ViewModel> viewModels = viewModelsFromStorageModel(items);
        initWithStorageModel(items, viewModels);

        final TestObserver<List<ViewModel>> observer = operations.initialTimelineItems(false).test();

        observer.assertValue(viewModels);
    }

    @Test
    public void whenItLoadsAnEmptyPageOfItemsOnPageOneThenItRequestsAFullSyncAndReloads() {
        // 1st page comes back blank first, then as full page of items after sync
        final List<StorageModel> items = createItems(PAGE_SIZE, LAST_ITEM_TIMESTAMP);
        final List<ViewModel> viewModels = viewModelsFromStorageModel(items);
        initWithStorageModelOnRepeatAfterSyncing(items, viewModels);
        // returning true means new items have been added to local storage
        when(syncInitiator.sync(syncable)).thenReturn(Single.just(successWithChange()));

        final TestObserver<List<ViewModel>> observer = operations.initialTimelineItems(false).test();

        InOrder inOrder = inOrder(storage, syncInitiator);
        inOrder.verify(storage).timelineItems(PAGE_SIZE);
        inOrder.verify(syncInitiator).sync(syncable);
        inOrder.verify(storage).timelineItems(PAGE_SIZE);
        inOrder.verifyNoMoreInteractions();
        observer.assertValue(viewModels);
        observer.assertComplete();
    }

    @Test // this makes sure we don't run into sync cycles
    public void whenItLoadsAnEmptyPageOfItemsOnPageOneEvenAfterAFullSyncItShouldNotSyncAgain() {
        // 1st page comes back blank first, then blank again even after syncing
        when(storage.timelineItems(PAGE_SIZE))
                .thenReturn(empty())
                .thenReturn(empty());
        // returning true means successful sync
        when(syncInitiator.sync(syncable)).thenReturn(Single.just(successWithChange()));

        final TestObserver<List<ViewModel>> observer = operations.initialTimelineItems(false).test();

        InOrder inOrder = inOrder(storage, syncInitiator);
        inOrder.verify(storage).timelineItems(PAGE_SIZE);
        inOrder.verify(syncInitiator).sync(syncable);
        inOrder.verify(storage).timelineItems(PAGE_SIZE);
        inOrder.verifyNoMoreInteractions();
        observer.assertValue(Collections.emptyList());
        observer.assertComplete();
    }

    @Test
    public void pagerLoadsNextPageUsingTimestampOfOldestItemOfPreviousPage() throws Exception {
        final List<StorageModel> items = createItems(PAGE_SIZE, LAST_ITEM_TIMESTAMP);
        when(storage.timelineItemsBefore(LAST_ITEM_TIMESTAMP, PAGE_SIZE)).thenReturn(never());

        operations.pagingFunction().call(viewModelsFromStorageModel(items));

        verify(storage).timelineItemsBefore(LAST_ITEM_TIMESTAMP, PAGE_SIZE);
    }

    @Test
    public void whenItLoadsAnEmptyPageOfItemsBeyondPageOneThenItRunsABackfillSyncAndReloads() {
        // 1st page is full page of items
        final long lastItemTimestamp = 123L;
        final List<StorageModel> firstPage = createItems(PAGE_SIZE, lastItemTimestamp);
        // 2nd page is blank on first attempt, then filled with items from backfill
        final List<StorageModel> secondPage = createItems(PAGE_SIZE, 456L);

        final List<ViewModel> viewModels = viewModelsFromStorageModel(secondPage);
        initWithStorageModelOnRepeatAfterSyncingBefore(lastItemTimestamp, secondPage, viewModels);
        // returning true means new items have been added to local storage
        when(syncInitiator.sync(syncable, SyncInitiator.ACTION_APPEND)).thenReturn(Single.just(successWithChange()));

        final AssertableSubscriber<List<ViewModel>> observer = operations.pagingFunction().call(viewModelsFromStorageModel(firstPage)).test();

        InOrder inOrder = inOrder(syncInitiator, storage);
        inOrder.verify(storage).timelineItemsBefore(lastItemTimestamp, PAGE_SIZE);
        inOrder.verify(syncInitiator).sync(syncable, SyncInitiator.ACTION_APPEND);
        inOrder.verify(storage).timelineItemsBefore(lastItemTimestamp, PAGE_SIZE);
        inOrder.verifyNoMoreInteractions();
        observer.assertValue(viewModels);
        observer.assertCompleted();
    }

    @Test
    public void shouldStopPaginationIfBackfillSyncReportsNoNewItemsSynced() {
        // 1st page is full page of items
        final List<StorageModel> items = createItems(PAGE_SIZE, 123L);
        final List<ViewModel> viewModels = viewModelsFromStorageModel(items);
        // 2nd page is blank, will trigger backfill
        when(storage.timelineItemsBefore(123L, PAGE_SIZE)).thenReturn(empty());
        // returning false means no new items have been added to local storage
        when(syncInitiator.sync(syncable, SyncInitiator.ACTION_APPEND)).thenReturn(Single.just(successWithoutChange()));

        final AssertableSubscriber<List<ViewModel>> observer = operations.pagingFunction().call(viewModels).test();

        InOrder inOrder = inOrder(storage, syncInitiator);
        inOrder.verify(storage).timelineItemsBefore(123L, PAGE_SIZE);
        inOrder.verify(syncInitiator).sync(syncable, SyncInitiator.ACTION_APPEND);
        inOrder.verifyNoMoreInteractions();
        observer.assertValue(Collections.emptyList());
        observer.assertCompleted();
    }

    @Test
    public void refreshingShouldRequestSyncAndReloadFromLocalStorageWhenNewItemsAvailable() {
        when(syncInitiator.sync(syncable, SyncInitiator.ACTION_HARD_REFRESH)).thenReturn(Single.just(successWithChange()));
        List<StorageModel> items = createItems(PAGE_SIZE, 123L);
        List<ViewModel> viewModels = viewModelsFromStorageModel(items);
        initWithStorageModel(items, viewModels);

        final TestObserver<List<ViewModel>> observer = operations.updatedTimelineItems().test();

        InOrder inOrder = inOrder(storage, syncInitiator);
        inOrder.verify(syncInitiator).sync(syncable, SyncInitiator.ACTION_HARD_REFRESH);
        inOrder.verify(storage).timelineItems(PAGE_SIZE);
        inOrder.verifyNoMoreInteractions();
        observer.assertValue(viewModels);
        observer.assertComplete();
    }

    @Test
    public void refreshingShouldRequestSyncAndCompleteWhenNoNewItemsAvailable() {
        when(syncInitiator.sync(syncable, SyncInitiator.ACTION_HARD_REFRESH)).thenReturn(Single.just(successWithoutChange()));

        final TestObserver<List<ViewModel>> observer = operations.updatedTimelineItems().test();

        verify(syncInitiator).sync(syncable, SyncInitiator.ACTION_HARD_REFRESH);
        verifyNoMoreInteractions(syncInitiator);
        verifyZeroInteractions(storage);
        observer.assertValue(Collections.emptyList());
        observer.assertComplete();
    }

    @Test
    public void getLastSyncReturnsLastTimestamp() {
        when(syncStateStorage.lastSyncTime(syncable)).thenReturn(123L);

        final TestObserver<Long> observer = operations.lastSyncTime().test();

        observer.assertValue(123L);
    }

    @Test
    public void shouldReturnNewItemsSinceTimestamp() {
        when(storage.timelineItemCountSince(123L)).thenReturn(just(3));

        final TestObserver<Integer> observer = operations.newItemsSince(123L).test();

        observer.assertValue(3);
    }

    @Test
    public void shouldNotUpdateStreamForStartWhenNeverSyncedBefore() {
        when(syncStateStorage.hasSyncedBefore(syncable)).thenReturn(false);
        when(syncInitiator.sync(syncable, SyncInitiator.ACTION_HARD_REFRESH)).thenReturn(Single.just(successWithChange()));

        final TestObserver<List<ViewModel>> observer = operations.updatedTimelineItemsForStart().test();

        observer.assertNoValues();
    }

    protected List<StorageModel> createStorageModels(int length, long lastItemTimestamp) {
        final List<StorageModel> headList = Collections.nCopies(length - 1, createTimelineItem(FIRST_ITEM_TIMESTAMP));
        final ArrayList<StorageModel> propertySets = new ArrayList<>(headList);
        propertySets.add(createTimelineItem(lastItemTimestamp));
        return propertySets;
    }

    protected abstract List<StorageModel> createItems(int length, long lastItemTimestamp);

    protected abstract StorageModel createTimelineItem(long timestamp);

    protected abstract List<ViewModel> viewModelsFromStorageModel(List<StorageModel> items);


    protected void initWithStorageModel(List<StorageModel> streamEntities) {
        when(storage.timelineItems(PAGE_SIZE)).thenReturn(fromIterable(streamEntities));
    }

    protected void initWithStorageModel(List<StorageModel> streamEntities, List<ViewModel> viewModels) {
        when(storage.timelineItems(PAGE_SIZE)).thenReturn(fromIterable(streamEntities));
        initViewModelFromStorageModel(streamEntities, viewModels);
    }

    protected void initWithStorageModelOnRepeatAfterSyncing(List<StorageModel> streamEntities) {
        when(storage.timelineItems(PAGE_SIZE)).thenReturn(empty()).thenReturn(fromIterable(streamEntities));
    }

    private void initWithStorageModelOnRepeatAfterSyncingBefore(long timestamp, List<StorageModel> streamEntities, List<ViewModel> viewModels) {
        when(storage.timelineItemsBefore(timestamp, PAGE_SIZE)).thenReturn(empty()).thenReturn(fromIterable(streamEntities));
        initViewModelFromStorageModel(streamEntities, viewModels);
    }

    protected void initWithStorageModelOnRepeatAfterSyncing(List<StorageModel> streamEntities, List<ViewModel> viewModels) {
        when(storage.timelineItems(PAGE_SIZE)).thenReturn(empty()).thenReturn(fromIterable(streamEntities));
        initViewModelFromStorageModel(streamEntities, viewModels);
    }

    protected abstract void initViewModelFromStorageModel(List<StorageModel> streamEntities, List<ViewModel> viewModels);
}
