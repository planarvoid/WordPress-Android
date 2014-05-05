package com.soundcloud.android.stream;

import static com.soundcloud.android.stream.SoundStreamOperations.INITIAL_TIMESTAMP;
import static com.soundcloud.android.stream.SoundStreamOperations.PAGE_SIZE;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static rx.android.OperatorPaged.Page;

import com.google.common.collect.Lists;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.storage.PropertySet;
import com.soundcloud.android.sync.SyncInitiator;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.Mock;
import rx.Observable;
import rx.Observer;
import rx.android.OperatorPaged;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class SoundStreamOperationsTest {

    private static final long TIMESTAMP = 1000L;

    private SoundStreamOperations operations;

    @Mock
    private SoundStreamStorage soundStreamStorage;

    @Mock
    private SyncInitiator syncInitiator;

    @Mock
    private Observer<Page> observer;

    @Captor
    private ArgumentCaptor<Page> pageCaptor;

    @Before
    public void setUp() throws Exception {
        operations = new SoundStreamOperations(soundStreamStorage, syncInitiator);
    }

    @Test
    public void whenItLoadsPageOneItUsesAFixedInitialTimestamp() {
        Urn userUrn = Urn.forUser(123);
        when(soundStreamStorage
                .loadStreamItemsAsync(userUrn, INITIAL_TIMESTAMP, PAGE_SIZE))
                .thenReturn(Observable.from(createItems(PAGE_SIZE)));

        operations.getStreamItems().subscribe(observer);

        verify(soundStreamStorage).loadStreamItemsAsync(userUrn, INITIAL_TIMESTAMP, PAGE_SIZE);
    }

    @Test
    public void whenItLoadsAnEmptyPageOfItemsOnPageOneThenItRequestsAFullSyncAndReloads() {
        Urn userUrn = Urn.forUser(123);
        // 1st page comes back blank first, then as full page of items after sync
        when(soundStreamStorage
                .loadStreamItemsAsync(userUrn, INITIAL_TIMESTAMP, PAGE_SIZE))
                .thenReturn(Observable.<PropertySet>empty(), Observable.from(createItems(PAGE_SIZE, 123L)));
        // stop call chain for test
        when(soundStreamStorage.loadStreamItemsAsync(userUrn, 123L, PAGE_SIZE))
                .thenReturn(Observable.<PropertySet>never());
        // returning true means new items have been added to local storage
        when(syncInitiator.syncSoundStream()).thenReturn(Observable.just(true));

        operations.getStreamItems().subscribe(observer);

        InOrder inOrder = inOrder(observer, soundStreamStorage, syncInitiator);
        inOrder.verify(soundStreamStorage).loadStreamItemsAsync(userUrn, INITIAL_TIMESTAMP, PAGE_SIZE);
        inOrder.verify(syncInitiator).syncSoundStream();
        inOrder.verify(soundStreamStorage).loadStreamItemsAsync(userUrn, INITIAL_TIMESTAMP, PAGE_SIZE);
        inOrder.verify(observer).onNext(any(Page.class));
        inOrder.verify(observer).onCompleted();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void whenItLoadsAFullPageOfItemsThenItLoadsNextPageUsingTimestampOfOldestItemOfPreviousPage() throws Exception {
        Urn userUrn = Urn.forUser(123);
        // 1st page is full page of items
        when(soundStreamStorage
                .loadStreamItemsAsync(userUrn, INITIAL_TIMESTAMP, PAGE_SIZE))
                .thenReturn(Observable.from(createItems(PAGE_SIZE, 123L)));
        // stop call chain for test
        when(soundStreamStorage.loadStreamItemsAsync(userUrn, 123L, PAGE_SIZE))
                .thenReturn(Observable.<PropertySet>never());

        operations.getStreamItems().subscribe(observer);
        advanceToNextPage();

        InOrder inOrder = inOrder(observer, soundStreamStorage, syncInitiator);
        inOrder.verify(soundStreamStorage).loadStreamItemsAsync(userUrn, INITIAL_TIMESTAMP, PAGE_SIZE);
        inOrder.verify(soundStreamStorage).loadStreamItemsAsync(userUrn, 123L, PAGE_SIZE);
        inOrder.verify(observer).onNext(any(Page.class));
        inOrder.verify(observer).onCompleted();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void whenItLoadsAnEmptyPageOfItemsBeyondPageOneThenItRunsABackfillSyncAndReloads() {
        Urn userUrn = Urn.forUser(123);
        // 1st page is full page of items
        when(soundStreamStorage
                .loadStreamItemsAsync(userUrn, INITIAL_TIMESTAMP, PAGE_SIZE))
                .thenReturn(Observable.from(createItems(PAGE_SIZE, 123L)));
        // 2nd page is blank on first attempt, then filled with items from backfill
        when(soundStreamStorage
                .loadStreamItemsAsync(userUrn, 123L, PAGE_SIZE))
                .thenReturn(Observable.<PropertySet>empty(), Observable.from(createItems(PAGE_SIZE, 456L)));
        // stop call chain for test
        when(soundStreamStorage.loadStreamItemsAsync(userUrn, 456L, PAGE_SIZE))
                .thenReturn(Observable.<PropertySet>never());
        // returning true means new items have been added to local storage
        when(syncInitiator.backfillSoundStream()).thenReturn(Observable.just(true));

        operations.getStreamItems().subscribe(observer);
        advanceToNextPage();

        InOrder inOrder = inOrder(observer, syncInitiator, soundStreamStorage);
        inOrder.verify(soundStreamStorage).loadStreamItemsAsync(userUrn, 123L, PAGE_SIZE);
        inOrder.verify(syncInitiator).backfillSoundStream();
        inOrder.verify(soundStreamStorage).loadStreamItemsAsync(userUrn, 123L, PAGE_SIZE);
        inOrder.verify(observer).onNext(any(Page.class));
        inOrder.verify(observer).onCompleted();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void shouldStopPaginationIfBackfillSyncReportsNoNewItemsSynced() {
        Urn userUrn = Urn.forUser(123);
        // 1st page is full page of items
        when(soundStreamStorage
                .loadStreamItemsAsync(userUrn, INITIAL_TIMESTAMP, PAGE_SIZE))
                .thenReturn(Observable.from(createItems(PAGE_SIZE, 123L)));
        // 2nd page is blank, will trigger backfill
        when(soundStreamStorage
                .loadStreamItemsAsync(userUrn, 123L, PAGE_SIZE))
                .thenReturn(Observable.<PropertySet>empty());
        // returning false means no new items have been added to local storage
        when(syncInitiator.backfillSoundStream()).thenReturn(Observable.just(false));

        operations.getStreamItems().subscribe(observer);
        advanceToNextPage();

        InOrder inOrder = inOrder(soundStreamStorage, syncInitiator, observer);
        inOrder.verify(soundStreamStorage).loadStreamItemsAsync(userUrn, INITIAL_TIMESTAMP, PAGE_SIZE);
        inOrder.verify(syncInitiator).backfillSoundStream();
        inOrder.verify(observer).onNext(OperatorPaged.emptyPage());
        inOrder.verify(observer).onCompleted();
        inOrder.verifyNoMoreInteractions();
    }

    private void advanceToNextPage() {
        verify(observer, atLeastOnce()).onNext(pageCaptor.capture());
        verify(observer, atLeastOnce()).onCompleted();
        pageCaptor.getValue().getNextPage().subscribe(observer);
    }

    private List<PropertySet> createItems(int length) {
        return createItems(length, TIMESTAMP);
    }

    private List<PropertySet> createItems(int length, long timestampOfLastItem) {
        final List<PropertySet> headList = Collections.nCopies(length - 1, PropertySet.from(
                StreamItemProperty.SOUND_URN.bind(Urn.forTrack(1L)),
                StreamItemProperty.CREATED_AT.bind(new Date(TIMESTAMP))));
        final PropertySet lastItem = PropertySet.from(
                StreamItemProperty.SOUND_URN.bind(Urn.forTrack(1L)),
                StreamItemProperty.CREATED_AT.bind(new Date(timestampOfLastItem)));
        final ArrayList<PropertySet> propertySets = Lists.newArrayList(headList);
        propertySets.add(lastItem);
        return propertySets;
    }
}
