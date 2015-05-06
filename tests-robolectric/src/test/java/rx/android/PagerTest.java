package rx.android;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import rx.Observable;
import rx.Observer;
import rx.Subscriber;
import rx.Subscription;

import java.util.Arrays;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.atomic.AtomicBoolean;

public class PagerTest {

    private final List<String> firstPage = Arrays.asList("item1", "item2", "item3");
    private final List<String> secondPage = Arrays.asList("item4", "item5", "item6");
    private final List<String> lastPage = Arrays.asList("item7", "item8", "item9");

    private final Stack<Observable<List<String>>> remainingPages = new Stack<>();

    private Pager<List<String>, List<String>> pager = Pager.create(new Pager.PagingFunction<List<String>>() {
        @Override
        public Observable<List<String>> call(List<String> page) {
            if (page == lastPage) {
                return Pager.finish(); // no next page
            } else {
                return remainingPages.pop(); // return the following page
            }
        }
    });

    @Mock Observer<List<String>> observer;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        remainingPages.push(Observable.just(lastPage));
        remainingPages.push(Observable.just(secondPage));
        remainingPages.push(Observable.just(firstPage));
    }

    @Test
    public void shouldEmitTheInitialSequenceWhenSubscribing() {
        pager.page(remainingPages.pop()).subscribe(observer);

        verify(observer).onNext(firstPage);
        verifyNoMoreInteractions(observer);
    }

    @Test
    public void shouldHaveMorePagesBeforeReachingTheLastPage() {
        pager.page(remainingPages.pop()).subscribe(observer);

        assertThat(pager.hasNext(), is(true));
    }

    @Test
    public void shouldEmitTheNextPage() {
        pager.page(remainingPages.pop()).subscribe(observer);
        pager.next();

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer).onNext(firstPage);
        inOrder.verify(observer).onNext(secondPage);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void shouldEmitTheLastPageAndComplete() {
        pager.page(remainingPages.pop()).subscribe(observer);
        jumpToLastPage();

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(3)).onNext(any(List.class));
        inOrder.verify(observer).onCompleted();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void shouldNotHaveNextPageIfLastPageReached() {
        pager.page(remainingPages.pop()).subscribe(observer);
        jumpToLastPage();

        assertThat(pager.hasNext(), is(false));
    }

    @Test
    public void shouldDoNothingWhenAskingForNextPageButThereIsNone() {
        pager.page(remainingPages.pop()).subscribe(observer);
        jumpToLastPage();

        pager.next();

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer, times(3)).onNext(any(List.class));
        inOrder.verify(observer).onCompleted();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void shouldDoNothingWhenAskingForNextPageButIsUnsubscribed() {
        Subscription subscription = pager.page(remainingPages.pop()).subscribe(observer);
        subscription.unsubscribe();

        pager.next();

        verify(observer).onNext(firstPage);
        verifyNoMoreInteractions(observer);
    }

    @Test
    public void shouldForwardErrorsToSubscriber() {
        pager.page(Observable.<List<String>>error(new Exception())).subscribe(observer);

        verify(observer).onError(isA(Exception.class));
        verifyNoMoreInteractions(observer);
    }

    @Test
    public void shouldForgetPreviousStateWhenPagingNewObservableWithSamePager() {
        pager.page(Observable.just(firstPage)).subscribe(observer);
        pager.page(Observable.just(firstPage)).subscribe(observer);

        verify(observer, times(2)).onNext(firstPage);
        verifyNoMoreInteractions(observer);
    }

    @Test
    public void shouldAllowToRetryTheCurrentPageInCaseItFailed() {
        remainingPages.clear();
        remainingPages.push(Observable.just(lastPage));
        remainingPages.push(failingSecondPage());
        remainingPages.push(Observable.just(firstPage));
        pager.page(remainingPages.pop()).subscribe(observer);
        pager.next(); // emits page 2

        pager.currentPage().subscribe(observer); // emits page 2 again (retry it)

        InOrder inOrder = inOrder(observer);
        inOrder.verify(observer).onNext(firstPage);
        inOrder.verify(observer).onError(isA(RuntimeException.class));
        inOrder.verify(observer).onNext(secondPage);
    }

    private Observable<List<String>> failingSecondPage() {
        return Observable.create(new Observable.OnSubscribe<List<String>>() {

            private AtomicBoolean failedAttempt = new AtomicBoolean();

            @Override
            public void call(Subscriber<? super List<String>> subscriber) {
                if (!failedAttempt.getAndSet(true)) {
                    subscriber.onError(new RuntimeException("failed page 2"));
                } else {
                    subscriber.onNext(secondPage);
                }
            }
        });
    }

    private void jumpToLastPage() {
        pager.next();
        pager.next();
    }
}