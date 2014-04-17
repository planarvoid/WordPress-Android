package com.soundcloud.android.stream;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.storage.PropertySet;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import rx.Observable;
import rx.Observer;
import rx.android.OperatorPaged;

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
    private Observer observer;

    @Before
    public void setUp() throws Exception {
        operations = new SoundStreamOperations(soundStreamStorage);
    }

    @Test
    public void shouldLoadNextPageWithIncreasedOffset() throws Exception {
        Urn userUrn = Urn.forUser(123);
        when(soundStreamStorage
                .loadStreamItemsAsync(eq(userUrn), anyLong(), any(Integer.class), any(Integer.class)))
                .thenReturn(Observable.from(createItems(30)));

        operations.getStreamItems().subscribe(observer);

        ArgumentCaptor<OperatorPaged.Page> captor = ArgumentCaptor.forClass(OperatorPaged.Page.class);
        verify(observer).onNext(captor.capture());
        captor.getValue().getNextPage().subscribe(observer);
        InOrder inOrder = inOrder(soundStreamStorage);
        inOrder.verify(soundStreamStorage).loadStreamItemsAsync(eq(userUrn), anyLong(), eq(30), eq(0));
        inOrder.verify(soundStreamStorage).loadStreamItemsAsync(eq(userUrn), anyLong(), eq(30), eq(30));
    }

    @Test
    public void shouldHaveNextPageIfCurrentPageHasExpectedNumberOfItems() throws Exception {
        when(soundStreamStorage
                .loadStreamItemsAsync(any(Urn.class), anyLong(), any(Integer.class), any(Integer.class)))
                .thenReturn(Observable.from(createItems(30)));

        operations.getStreamItems().subscribe(observer);

        ArgumentCaptor<OperatorPaged.Page> captor = ArgumentCaptor.forClass(OperatorPaged.Page.class);
        verify(observer).onNext(captor.capture());
        expect(captor.getValue().hasNextPage()).toBeTrue();
    }

    @Test
    public void shouldNotHaveNextPageIfCurrentPageHasLessItemsThanExpected() throws Exception {
        when(soundStreamStorage
                .loadStreamItemsAsync(any(Urn.class), anyLong(), any(Integer.class), any(Integer.class)))
                .thenReturn(Observable.from(createItems(29)));

        operations.getStreamItems().subscribe(observer);

        ArgumentCaptor<OperatorPaged.Page> captor = ArgumentCaptor.forClass(OperatorPaged.Page.class);
        verify(observer).onNext(captor.capture());
        expect(captor.getValue().hasNextPage()).toBeFalse();
    }

    @Test
    public void shouldNotHaveNextPageIfCurrentPageHasNoItems() throws Exception {
        when(soundStreamStorage
                .loadStreamItemsAsync(any(Urn.class), anyLong(), any(Integer.class), any(Integer.class)))
                .thenReturn(Observable.from(Collections.<PropertySet>emptyList()));

        operations.getStreamItems().subscribe(observer);

        ArgumentCaptor<OperatorPaged.Page> captor = ArgumentCaptor.forClass(OperatorPaged.Page.class);
        verify(observer).onNext(captor.capture());
        expect(captor.getValue().hasNextPage()).toBeFalse();
    }

    @Test
    public void shouldRetrieveFirstPageWithTimestampThatsLargeEnoughToCoverAllPossibleItems() {
        Urn userUrn = Urn.forUser(123);
        when(soundStreamStorage
                .loadStreamItemsAsync(eq(userUrn), anyLong(), any(Integer.class), any(Integer.class)))
                .thenReturn(Observable.from(createItems(30)));

        operations.getStreamItems().subscribe(observer);

        verify(soundStreamStorage).loadStreamItemsAsync(userUrn, Long.MAX_VALUE, 30, 0);
    }

    @Test
    public void shouldUseTimestampsForPagingBeyondPageOneInOrderToKeepPaginationStable() {
        Urn userUrn = Urn.forUser(123);
        when(soundStreamStorage
                .loadStreamItemsAsync(eq(userUrn), anyLong(), any(Integer.class), any(Integer.class)))
                .thenReturn(Observable.from(createItems(30)));

        operations.getStreamItems().subscribe(observer);

        ArgumentCaptor<OperatorPaged.Page> captor = ArgumentCaptor.forClass(OperatorPaged.Page.class);
        verify(observer).onNext(captor.capture());
        captor.getValue().getNextPage().subscribe(observer);
        InOrder inOrder = inOrder(soundStreamStorage);
        inOrder.verify(soundStreamStorage).loadStreamItemsAsync(eq(userUrn), eq(Long.MAX_VALUE), eq(30), eq(0));
        inOrder.verify(soundStreamStorage).loadStreamItemsAsync(eq(userUrn), eq(TIMESTAMP), eq(30), eq(30));
    }

    private List<PropertySet> createItems(int length) {
        return Collections.nCopies(length, PropertySet.from(StreamItemProperty.CREATED_AT.bind(new Date(TIMESTAMP))));
    }
}
