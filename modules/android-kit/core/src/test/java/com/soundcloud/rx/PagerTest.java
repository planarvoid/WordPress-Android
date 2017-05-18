package com.soundcloud.rx;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InOrder;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
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

    private Pager<List<String>> pager = Pager.create(new Pager.PagingFunction<List<String>>() {
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

        Mockito.verify(observer).onNext(firstPage);
        Mockito.verifyNoMoreInteractions(observer);
    }

    @Test
    public void shouldHaveMorePagesBeforeReachingTheLastPage() {
        pager.page(remainingPages.pop()).subscribe(observer);

        Assert.assertThat(pager.hasNext(), CoreMatchers.is(true));
    }

    @Test
    public void shouldEmitTheNextPage() {
        pager.page(remainingPages.pop()).subscribe(observer);
        pager.next();

        InOrder inOrder = Mockito.inOrder(observer);
        inOrder.verify(observer).onNext(firstPage);
        inOrder.verify(observer).onNext(secondPage);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void shouldEmitTheLastPageAndComplete() {
        pager.page(remainingPages.pop()).subscribe(observer);
        jumpToLastPage();

        InOrder inOrder = Mockito.inOrder(observer);
        inOrder.verify(observer, Mockito.times(3)).onNext(Matchers.any(List.class));
        inOrder.verify(observer).onCompleted();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void shouldNotHaveNextPageIfLastPageReached() {
        pager.page(remainingPages.pop()).subscribe(observer);
        jumpToLastPage();

        Assert.assertThat(pager.hasNext(), CoreMatchers.is(false));
    }

    @Test
    public void shouldDoNothingWhenAskingForNextPageButThereIsNone() {
        pager.page(remainingPages.pop()).subscribe(observer);
        jumpToLastPage();

        pager.next();

        InOrder inOrder = Mockito.inOrder(observer);
        inOrder.verify(observer, Mockito.times(3)).onNext(Matchers.any(List.class));
        inOrder.verify(observer).onCompleted();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void shouldDoNothingWhenAskingForNextPageButIsUnsubscribed() {
        Subscription subscription = pager.page(remainingPages.pop()).subscribe(observer);
        subscription.unsubscribe();

        pager.next();

        Mockito.verify(observer).onNext(firstPage);
        Mockito.verifyNoMoreInteractions(observer);
    }

    @Test
    public void shouldForwardErrorsToSubscriber() {
        pager.page(Observable.<List<String>>error(new Exception())).subscribe(observer);

        Mockito.verify(observer).onError(Matchers.isA(Exception.class));
        Mockito.verifyNoMoreInteractions(observer);
    }

    @Test
    public void shouldForgetPreviousStateWhenPagingNewObservableWithSamePager() {
        pager.page(Observable.just(firstPage)).subscribe(observer);
        pager.page(Observable.just(firstPage)).subscribe(observer);

        Mockito.verify(observer, Mockito.times(2)).onNext(firstPage);
        Mockito.verifyNoMoreInteractions(observer);
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

        InOrder inOrder = Mockito.inOrder(observer);
        inOrder.verify(observer).onNext(firstPage);
        inOrder.verify(observer).onError(Matchers.isA(RuntimeException.class));
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
