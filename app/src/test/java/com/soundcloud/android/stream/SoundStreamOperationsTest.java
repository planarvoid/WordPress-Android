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
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.model.PlayableProperty;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.model.UserUrn;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.sync.SyncInitiator;
import com.soundcloud.propeller.PropertySet;
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

import android.content.Context;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class SoundStreamOperationsTest {

    private static final long TIMESTAMP = 1000L;

    private SoundStreamOperations operations;
    private UserUrn userUrn = Urn.forUser(123L);

    @Mock private SoundStreamStorage soundStreamStorage;
    @Mock private SyncInitiator syncInitiator;
    @Mock private AccountOperations accountOperations;
    @Mock private Observer<Page> observer;
    @Mock private Context context;

    @Captor private ArgumentCaptor<Page> pageCaptor;

    @Before
    public void setUp() throws Exception {
        operations = new SoundStreamOperations(soundStreamStorage, syncInitiator, accountOperations, context);
        when(accountOperations.getLoggedInUserUrn()).thenReturn(userUrn);
    }

    @Test
    public void whenItLoadsPageOneItUsesAFixedInitialTimestamp() {
        when(soundStreamStorage
                .streamItemsBefore(INITIAL_TIMESTAMP, userUrn, PAGE_SIZE))
                .thenReturn(Observable.from(createItems(PAGE_SIZE, 123L)));

        operations.existingStreamItems().subscribe(observer);

        verify(soundStreamStorage).streamItemsBefore(INITIAL_TIMESTAMP, userUrn, PAGE_SIZE);
    }

    @Test
    public void whenItLoadsAnEmptyPageOfItemsOnPageOneThenItRequestsAFullSyncAndReloads() {
        // 1st page comes back blank first, then as full page of items after sync
        when(soundStreamStorage
                .streamItemsBefore(INITIAL_TIMESTAMP, userUrn, PAGE_SIZE))
                .thenReturn(Observable.<PropertySet>empty(), Observable.from(createItems(PAGE_SIZE, 123L)));
        // stop call chain for test
        when(soundStreamStorage.streamItemsBefore(123L, userUrn, PAGE_SIZE))
                .thenReturn(Observable.<PropertySet>never());
        // returning true means new items have been added to local storage
        when(syncInitiator.refreshSoundStream()).thenReturn(Observable.just(true));

        operations.existingStreamItems().subscribe(observer);

        InOrder inOrder = inOrder(observer, soundStreamStorage, syncInitiator);
        inOrder.verify(soundStreamStorage).streamItemsBefore(INITIAL_TIMESTAMP, userUrn, PAGE_SIZE);
        inOrder.verify(syncInitiator).refreshSoundStream();
        inOrder.verify(soundStreamStorage).streamItemsBefore(INITIAL_TIMESTAMP, userUrn, PAGE_SIZE);
        inOrder.verify(observer).onNext(any(Page.class));
        inOrder.verify(observer).onCompleted();
        inOrder.verifyNoMoreInteractions();
    }

    @Test // this makes sure we don't run into sync cycles
    public void whenItLoadsAnEmptyPageOfItemsOnPageOneEvenAfterAFullSyncItShouldNotSyncAgain() {
        // 1st page comes back blank first, then blank again even after syncing
        when(soundStreamStorage
                .streamItemsBefore(INITIAL_TIMESTAMP, userUrn, PAGE_SIZE))
                .thenReturn(Observable.<PropertySet>empty(), Observable.<PropertySet>empty());
        // stop call chain for test
        when(soundStreamStorage.streamItemsBefore(123L, userUrn, PAGE_SIZE))
                .thenReturn(Observable.<PropertySet>never());
        // returning true means successful sync
        when(syncInitiator.refreshSoundStream()).thenReturn(Observable.just(true));

        operations.existingStreamItems().subscribe(observer);

        InOrder inOrder = inOrder(observer, soundStreamStorage, syncInitiator);
        inOrder.verify(soundStreamStorage).streamItemsBefore(INITIAL_TIMESTAMP, userUrn, PAGE_SIZE);
        inOrder.verify(syncInitiator).refreshSoundStream();
        inOrder.verify(soundStreamStorage).streamItemsBefore(INITIAL_TIMESTAMP, userUrn, PAGE_SIZE);
        inOrder.verify(observer).onCompleted();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void whenItLoadsAFullPageOfItemsThenItLoadsNextPageUsingTimestampOfOldestItemOfPreviousPage() throws Exception {
        // 1st page is full page of items
        when(soundStreamStorage
                .streamItemsBefore(INITIAL_TIMESTAMP, userUrn, PAGE_SIZE))
                .thenReturn(Observable.from(createItems(PAGE_SIZE, 123L)));
        // stop call chain for test
        when(soundStreamStorage.streamItemsBefore(123L, userUrn, PAGE_SIZE))
                .thenReturn(Observable.<PropertySet>never());

        operations.existingStreamItems().subscribe(observer);
        advanceToNextPage();

        InOrder inOrder = inOrder(observer, soundStreamStorage, syncInitiator);
        inOrder.verify(soundStreamStorage).streamItemsBefore(INITIAL_TIMESTAMP, userUrn, PAGE_SIZE);
        inOrder.verify(soundStreamStorage).streamItemsBefore(123L, userUrn, PAGE_SIZE);
        inOrder.verify(observer).onNext(any(Page.class));
        inOrder.verify(observer).onCompleted();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void whenItLoadsAnEmptyPageOfItemsBeyondPageOneThenItRunsABackfillSyncAndReloads() {
        // 1st page is full page of items
        when(soundStreamStorage
                .streamItemsBefore(INITIAL_TIMESTAMP, userUrn, PAGE_SIZE))
                .thenReturn(Observable.from(createItems(PAGE_SIZE, 123L)));
        // 2nd page is blank on first attempt, then filled with items from backfill
        when(soundStreamStorage
                .streamItemsBefore(123L, userUrn, PAGE_SIZE))
                .thenReturn(Observable.<PropertySet>empty(), Observable.from(createItems(PAGE_SIZE, 456L)));
        // stop call chain for test
        when(soundStreamStorage.streamItemsBefore(456L, userUrn, PAGE_SIZE))
                .thenReturn(Observable.<PropertySet>never());
        // returning true means new items have been added to local storage
        when(syncInitiator.backfillSoundStream()).thenReturn(Observable.just(true));

        operations.existingStreamItems().subscribe(observer);
        advanceToNextPage();

        InOrder inOrder = inOrder(observer, syncInitiator, soundStreamStorage);
        inOrder.verify(soundStreamStorage).streamItemsBefore(123L, userUrn, PAGE_SIZE);
        inOrder.verify(syncInitiator).backfillSoundStream();
        inOrder.verify(soundStreamStorage).streamItemsBefore(123L, userUrn, PAGE_SIZE);
        inOrder.verify(observer).onNext(any(Page.class));
        inOrder.verify(observer).onCompleted();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void shouldStopPaginationIfBackfillSyncReportsNoNewItemsSynced() {
        // 1st page is full page of items
        when(soundStreamStorage
                .streamItemsBefore(INITIAL_TIMESTAMP, userUrn, PAGE_SIZE))
                .thenReturn(Observable.from(createItems(PAGE_SIZE, 123L)));
        // 2nd page is blank, will trigger backfill
        when(soundStreamStorage
                .streamItemsBefore(123L, userUrn, PAGE_SIZE))
                .thenReturn(Observable.<PropertySet>empty());
        // returning false means no new items have been added to local storage
        when(syncInitiator.backfillSoundStream()).thenReturn(Observable.just(false));

        operations.existingStreamItems().subscribe(observer);
        advanceToNextPage();

        InOrder inOrder = inOrder(soundStreamStorage, syncInitiator, observer);
        inOrder.verify(soundStreamStorage).streamItemsBefore(INITIAL_TIMESTAMP, userUrn, PAGE_SIZE);
        inOrder.verify(syncInitiator).backfillSoundStream();
        inOrder.verify(observer).onNext(OperatorPaged.emptyPage());
        inOrder.verify(observer).onCompleted();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void shouldRequestSoundStreamSyncAndReloadFromLocalStorageWhenNewItemsAvailable() {
        when(syncInitiator.refreshSoundStream()).thenReturn(Observable.just(true));
        when(soundStreamStorage
                .streamItemsBefore(INITIAL_TIMESTAMP, userUrn, PAGE_SIZE))
                .thenReturn(Observable.from(createItems(PAGE_SIZE, 123L)));
        when(soundStreamStorage
                .streamItemsBefore(123L, userUrn, PAGE_SIZE))
                .thenReturn(Observable.<PropertySet>never());

        operations.updatedStreamItems().subscribe(observer);

        InOrder inOrder = inOrder(soundStreamStorage, syncInitiator, observer);
        inOrder.verify(syncInitiator).refreshSoundStream();
        inOrder.verify(soundStreamStorage).streamItemsBefore(INITIAL_TIMESTAMP, userUrn, PAGE_SIZE);
        inOrder.verify(soundStreamStorage).streamItemsBefore(123L, userUrn, PAGE_SIZE); // next page observable
        inOrder.verify(observer).onNext(any(Page.class));
        inOrder.verify(observer).onCompleted();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void shouldRequestSoundStreamSyncAndCompleteWhenNoNewItemsAvailable() {
        when(syncInitiator.refreshSoundStream()).thenReturn(Observable.just(false));

        operations.updatedStreamItems().subscribe(observer);

        InOrder inOrder = inOrder(soundStreamStorage, syncInitiator, observer);
        inOrder.verify(syncInitiator).refreshSoundStream();
        inOrder.verify(observer).onNext(OperatorPaged.emptyPage());
        inOrder.verify(observer).onCompleted();
        inOrder.verifyNoMoreInteractions();
    }

    private void advanceToNextPage() {
        verify(observer, atLeastOnce()).onNext(pageCaptor.capture());
        verify(observer, atLeastOnce()).onCompleted();
        pageCaptor.getValue().getNextPage().subscribe(observer);
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
