package rx.android;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static rx.android.OperatorPaged.Page;
import static com.soundcloud.android.rx.RxTestHelper.endlessPagerFrom;
import static rx.android.OperatorPaged.pagedWith;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import rx.Observable;
import rx.Observer;

import java.util.Arrays;
import java.util.List;

public class OperatorPagedTest {

    @Mock
    private Observer mockObserver;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void itDeliversIterableSequencesInPages() {
        Observable<List<Integer>> emptyPage = Observable.empty();
        pagedObservableWithNext(emptyPage).subscribe(mockObserver);

        ArgumentCaptor<Page> arguments = ArgumentCaptor.forClass(Page.class);
        verify(mockObserver).onNext(arguments.capture());
        verify(mockObserver).onCompleted();

        Page page = arguments.getValue();
        assertTrue(page.getPagedCollection().iterator().hasNext());
    }

    @Test
    public void itDeliversTheNextPage() {
        Observable source = Observable.just(1, 2, 3).toList();
        Observable<List<Integer>> nextPage = Observable.just(4, 5, 6).toList();

        Observable<Page<List<Integer>>> observable = source.lift(pagedWith(endlessPagerFrom(nextPage)));
        observable.subscribe(mockObserver);

        ArgumentCaptor<Page> arguments = ArgumentCaptor.forClass(Page.class);
        verify(mockObserver).onNext(arguments.capture());

        assertTrue(arguments.getValue().hasNextPage());
        Page page = arguments.getValue();

        Observer<Page> nextPageObserver = mock(Observer.class);
        page.getNextPage().subscribe(nextPageObserver);
        verify(nextPageObserver).onNext(any(Page.class));
    }

    @Test
    public void itForwardsExceptions() {
        Observable<List<Integer>> error = Observable.error(new Exception());
        Observable<List<Integer>> empty = Observable.empty();
        Observable<Page<List<Integer>>> observable = error.lift(pagedWith(endlessPagerFrom(empty)));
        observable.subscribe(mockObserver);
        verify(mockObserver).onError(isA(Exception.class));
    }

    @Test
    public void emptyPageDoesNotHaveANextPage() {
        assertFalse(OperatorPaged.emptyPage().hasNextPage());
        assertSame(OperatorPaged.emptyPage().getNextPage(), OperatorPaged.emptyObservable());
    }

    @Test
    public void emptyPageWrapsAnEmptyCollection() {
        assertFalse(OperatorPaged.emptyPage().getPagedCollection().iterator().hasNext());
    }

    @Test
    public void emptyObservableShouldOnlyFireCompleted() {
        OperatorPaged.emptyObservable().subscribe(mockObserver);
        verify(mockObserver, never()).onNext(any());
        verify(mockObserver, never()).onError(any(Throwable.class));
        verify(mockObserver).onCompleted();
        verifyNoMoreInteractions(mockObserver);
    }

    @Test
    public void emptyPageObservableEmitASingleEmptyPage() {
        OperatorPaged.emptyPageObservable().subscribe(mockObserver);
        verify(mockObserver).onNext(OperatorPaged.emptyPage());
        verify(mockObserver, never()).onError(any(Throwable.class));
        verify(mockObserver).onCompleted();
        verifyNoMoreInteractions(mockObserver);
    }

    private Observable<Page<List<Integer>>> pagedObservableWithNext(Observable<List<Integer>> nextPage) {
        Observable source = Observable.from(Arrays.asList(1, 2, 3)).toList();
        return source.lift(pagedWith(endlessPagerFrom(nextPage)));
    }
}
