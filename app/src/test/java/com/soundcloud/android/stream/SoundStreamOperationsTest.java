package com.soundcloud.android.stream;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
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
    private static final int PAGE_SIZE = 30;

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
    public void shouldLoadFirstPageWithTimestampThatsLargeEnoughToCoverAllPossibleItems() {
        Urn userUrn = Urn.forUser(123);
        when(soundStreamStorage
                .loadStreamItemsAsync(eq(userUrn), anyLong(), any(Integer.class)))
                .thenReturn(Observable.from(createItems(PAGE_SIZE)));

        operations.getStreamItems().subscribe(observer);

        verify(soundStreamStorage).loadStreamItemsAsync(userUrn, Long.MAX_VALUE, PAGE_SIZE);
    }

    @Test
    public void shouldLoadNextPageByUsingTimestampOfOldestItemOfPreviousPage() throws Exception {
        Urn userUrn = Urn.forUser(123);
        when(soundStreamStorage
                .loadStreamItemsAsync(eq(userUrn), eq(Long.MAX_VALUE), any(Integer.class)))
                .thenReturn(Observable.from(createItems(PAGE_SIZE)));
        when(soundStreamStorage
                .loadStreamItemsAsync(eq(userUrn), eq(TIMESTAMP), any(Integer.class)))
                .thenReturn(Observable.from(createItems(PAGE_SIZE, 123L)));

        operations.getStreamItems().subscribe(observer);

        verify(observer).onNext(pageCaptor.capture());
        pageCaptor.getValue().getNextPage().subscribe(observer);
        InOrder inOrder = inOrder(soundStreamStorage);
        inOrder.verify(soundStreamStorage).loadStreamItemsAsync(userUrn, TIMESTAMP, PAGE_SIZE);
        inOrder.verify(soundStreamStorage).loadStreamItemsAsync(userUrn, 123L, PAGE_SIZE);
    }

    @Test
    public void shouldHaveNextPageIfCurrentPageHasExpectedNumberOfItems() throws Exception {
        when(soundStreamStorage
                .loadStreamItemsAsync(any(Urn.class), anyLong(), any(Integer.class)))
                .thenReturn(Observable.from(createItems(PAGE_SIZE)));

        operations.getStreamItems().subscribe(observer);

        verify(observer).onNext(pageCaptor.capture());
        expect(pageCaptor.getValue().hasNextPage()).toBeTrue();
    }

    @Test
    public void shouldTriggerBackfillAndReloadWhenNotEnoughItemsFoundInLocalStorage() {
        Urn userUrn = Urn.forUser(123);
        when(soundStreamStorage
                .loadStreamItemsAsync(eq(userUrn), eq(Long.MAX_VALUE), eq(PAGE_SIZE)))
                .thenReturn(Observable.from(createItems(PAGE_SIZE - 1, 123L)));
        // returning true means new items have been added to local storage
        when(syncInitiator.backfillSoundStream()).thenReturn(Observable.just(true));

        jumpToNextPage();

        InOrder inOrder = inOrder(syncInitiator, soundStreamStorage);
        inOrder.verify(syncInitiator).backfillSoundStream();
        inOrder.verify(soundStreamStorage).loadStreamItemsAsync(eq(userUrn), eq(123L), eq(PAGE_SIZE));
    }

    @Test
    public void shouldTriggerBackfillAndReloadWhenNoItemsFoundInLocalStorage() {
        Urn userUrn = Urn.forUser(123);
        when(soundStreamStorage
                .loadStreamItemsAsync(eq(userUrn), eq(Long.MAX_VALUE), eq(PAGE_SIZE)))
                .thenReturn(Observable.<PropertySet>empty());
        // returning true means new items have been added to local storage
        when(syncInitiator.backfillSoundStream()).thenReturn(Observable.just(true));

        jumpToNextPage();

        InOrder inOrder = inOrder(syncInitiator, soundStreamStorage);
        inOrder.verify(syncInitiator).backfillSoundStream();
        inOrder.verify(soundStreamStorage).loadStreamItemsAsync(eq(userUrn), eq(Long.MAX_VALUE), eq(PAGE_SIZE));
    }

    @Test
    public void shouldStopPaginationIfBackfillSyncReportsNoNewItemsSynced() {
        Urn userUrn = Urn.forUser(123);
        when(soundStreamStorage
                .loadStreamItemsAsync(eq(userUrn), eq(Long.MAX_VALUE), eq(PAGE_SIZE)))
                .thenReturn(Observable.from(createItems(PAGE_SIZE - 1)));
        // returning false means no new items have been added to local storage
        when(syncInitiator.backfillSoundStream()).thenReturn(Observable.just(false));

        jumpToNextPage();

        InOrder inOrder = inOrder(syncInitiator, observer);
        inOrder.verify(syncInitiator).backfillSoundStream();
        inOrder.verify(observer).onNext(OperatorPaged.emptyPage());
        inOrder.verify(observer).onCompleted();
        inOrder.verifyNoMoreInteractions();
    }

    private void jumpToNextPage() {
        operations.getStreamItems().subscribe(observer);
        verify(observer).onNext(pageCaptor.capture());
        verify(observer).onCompleted();
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
