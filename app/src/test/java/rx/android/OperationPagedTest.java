package rx.android;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static rx.android.OperationPaged.Page;
import static rx.android.OperationPaged.nextPageFrom;
import static rx.android.OperationPaged.paged;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import rx.Observable;
import rx.Observer;

import java.util.List;

@RunWith(JUnit4.class)
public class OperationPagedTest {

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
        Observable source = Observable.from(1, 2, 3).toList();
        Observable<List<Integer>> nextPage = Observable.from(4, 5, 6).toList();

        Observable<Page<List<Integer>>> observable = Observable.create(paged(source, nextPageFrom(nextPage)));
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
        Observable<Page<List<Integer>>> observable = Observable.create(paged(error, nextPageFrom(empty)));
        observable.subscribe(mockObserver);
        verify(mockObserver).onError(isA(Exception.class));
    }

    @Test
    public void testEmptyPageHelper() {
        assertFalse(OperationPaged.emptyPage().hasNextPage());
        assertFalse(OperationPaged.emptyPage().getPagedCollection().iterator().hasNext());
        assertSame(OperationPaged.emptyPage().getNextPage(), OperationPaged.emptyPageObservable());
    }

    @Test
    public void testEmptyPageObservable() {
        OperationPaged.emptyPageObservable().subscribe(mockObserver);
        verify(mockObserver, never()).onNext(any());
        verify(mockObserver, never()).onError(any(Throwable.class));
        verify(mockObserver).onCompleted();
    }

    private Observable<Page<List<Integer>>> pagedObservableWithNext(Observable<List<Integer>> nextPage) {
        Observable source = Observable.from(1, 2, 3).toList();
        return Observable.create(paged(source, nextPageFrom(nextPage)));
    }
}
