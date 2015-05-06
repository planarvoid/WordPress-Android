package com.soundcloud.android.stream;

import static com.soundcloud.android.stream.SoundStreamOperations.PAGE_SIZE;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import com.soundcloud.android.api.legacy.model.ContentStats;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.sync.SyncInitiator;
import com.soundcloud.propeller.PropertySet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import rx.Observable;
import rx.Observer;
import rx.schedulers.Schedulers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class SoundStreamOperationsTest {

    private static final long TIMESTAMP = 1000L;

    private SoundStreamOperations operations;

    @Mock private SoundStreamStorage soundStreamStorage;
    @Mock private SyncInitiator syncInitiator;
    @Mock private Observer<List<PropertySet>> observer;
    @Mock private ContentStats contentStats;

    @Before
    public void setUp() throws Exception {
        operations = new SoundStreamOperations(soundStreamStorage, syncInitiator, contentStats, Schedulers.immediate());
    }

    @Test
    public void existingStreamItemsLoadsPageOne() {
        when(soundStreamStorage.initialStreamItems(PAGE_SIZE))
                .thenReturn(Observable.from(createItems(PAGE_SIZE, 123L)));

        operations.initialStreamItems().subscribe(observer);

        verify(soundStreamStorage).initialStreamItems(PAGE_SIZE);
    }

    @Test
    public void whenItLoadsAnEmptyPageOfItemsOnPageOneThenItRequestsAFullSyncAndReloads() {
        // 1st page comes back blank first, then as full page of items after sync
        final List<PropertySet> items = createItems(PAGE_SIZE, 123L);
        when(soundStreamStorage.initialStreamItems(PAGE_SIZE))
                .thenReturn(Observable.<PropertySet>empty(), Observable.from(items));
        // returning true means new items have been added to local storage
        when(syncInitiator.refreshSoundStream()).thenReturn(Observable.just(true));

        operations.initialStreamItems().subscribe(observer);

        InOrder inOrder = inOrder(observer, soundStreamStorage, syncInitiator);
        inOrder.verify(soundStreamStorage).initialStreamItems(PAGE_SIZE);
        inOrder.verify(syncInitiator).refreshSoundStream();
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
        when(syncInitiator.refreshSoundStream())
                .thenReturn(Observable.just(true));

        operations.initialStreamItems().subscribe(observer);

        InOrder inOrder = inOrder(observer, soundStreamStorage, syncInitiator);
        inOrder.verify(soundStreamStorage).initialStreamItems(PAGE_SIZE);
        inOrder.verify(syncInitiator).refreshSoundStream();
        inOrder.verify(soundStreamStorage).initialStreamItems(PAGE_SIZE);
        inOrder.verify(observer).onNext(Collections.<PropertySet>emptyList());
        inOrder.verify(observer).onCompleted();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void pagerLoadsNextPageUsingTimestampOfOldestItemOfPreviousPage() throws Exception {
        // 1st page is full page of items
        final List<PropertySet> items = createItems(PAGE_SIZE, 123L);
        when(soundStreamStorage.initialStreamItems(PAGE_SIZE))
                .thenReturn(Observable.from(items));
        // stop call chain for test
        when(soundStreamStorage.streamItemsBefore(123L, PAGE_SIZE))
                .thenReturn(Observable.<PropertySet>never());

        operations.pager().page(operations.initialStreamItems()).subscribe(observer);
        operations.pager().next();

        InOrder inOrder = inOrder(observer, soundStreamStorage, syncInitiator);
        inOrder.verify(soundStreamStorage).initialStreamItems(PAGE_SIZE);
        inOrder.verify(soundStreamStorage).streamItemsBefore(123L, PAGE_SIZE);
        inOrder.verify(observer).onNext(items);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void whenItLoadsAnEmptyPageOfItemsBeyondPageOneThenItRunsABackfillSyncAndReloads() {
        // 1st page is full page of items
        final List<PropertySet> firstPage = createItems(PAGE_SIZE, 123L);
        when(soundStreamStorage.initialStreamItems(PAGE_SIZE))
                .thenReturn(Observable.from(firstPage));
        // 2nd page is blank on first attempt, then filled with items from backfill
        final List<PropertySet> secondPage = createItems(PAGE_SIZE, 456L);
        when(soundStreamStorage.streamItemsBefore(123L, PAGE_SIZE))
                .thenReturn(Observable.<PropertySet>empty(), Observable.from(secondPage));
        // stop call chain for test
        when(soundStreamStorage.streamItemsBefore(456L, PAGE_SIZE))
                .thenReturn(Observable.<PropertySet>never());
        // returning true means new items have been added to local storage
        when(syncInitiator.backfillSoundStream())
                .thenReturn(Observable.just(true));

        operations.pager().page(operations.initialStreamItems()).subscribe(observer);
        operations.pager().next();

        InOrder inOrder = inOrder(observer, syncInitiator, soundStreamStorage);
        inOrder.verify(soundStreamStorage).streamItemsBefore(123L, PAGE_SIZE);
        inOrder.verify(observer).onNext(firstPage);
        inOrder.verify(syncInitiator).backfillSoundStream();
        inOrder.verify(soundStreamStorage).streamItemsBefore(123L, PAGE_SIZE);
        inOrder.verify(observer).onNext(secondPage);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void shouldStopPaginationIfBackfillSyncReportsNoNewItemsSynced() {
        // 1st page is full page of items
        when(soundStreamStorage.initialStreamItems(PAGE_SIZE))
                .thenReturn(Observable.from(createItems(PAGE_SIZE, 123L)));
        // 2nd page is blank, will trigger backfill
        when(soundStreamStorage.streamItemsBefore(123L, PAGE_SIZE))
                .thenReturn(Observable.<PropertySet>empty());
        // returning false means no new items have been added to local storage
        when(syncInitiator.backfillSoundStream())
                .thenReturn(Observable.just(false));

        operations.pager().page(operations.initialStreamItems()).subscribe(observer);
        operations.pager().next();

        InOrder inOrder = inOrder(soundStreamStorage, syncInitiator, observer);
        inOrder.verify(soundStreamStorage).initialStreamItems(PAGE_SIZE);
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
