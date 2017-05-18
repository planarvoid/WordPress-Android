package com.soundcloud.rx;

import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.functions.Func1;
import rx.subjects.PublishSubject;
import rx.subscriptions.Subscriptions;

public class Pager<T> {

    private static final Observable FINISH_SEQUENCE = Observable.never();

    private PublishSubject<Observable<T>> pages;
    private Observable<T> nextPage = finish();
    private Subscription subscription = Subscriptions.empty();

    private final PagingFunction<T> pagingFunction;

    public static <T> Pager<T> create(PagingFunction<T> pagingFunction) {
        return new Pager<>(pagingFunction);
    }

    Pager(PagingFunction<T> pagingFunction) {
        this.pagingFunction = pagingFunction;
    }

    /**
     * Used in the paging function to signal the caller that no more pages are available, i.e.
     * to finish paging by completing the paged sequence.
     *
     * @return the finish token
     */
    @SuppressWarnings("unchecked")
    public static <T> Observable<T> finish() {
        return FINISH_SEQUENCE;
    }

    /**
     * Transforms the given sequence to have its subsequent pages pushed into the observer subscribed
     * to the new sequence returned by this method. You can advance to the next page by calling {@link #next()}
     *
     * @param source the source sequence, which would be the first page of the sequence to be paged
     * @return a new sequence based on {@code source}, where subscribers keep receiving pages through subsequent calls
     * to {@link #next()}
     */
    public Observable<T> page(final Observable<T> source) {
        return Observable.create(new Observable.OnSubscribe<T>() {
            @Override
            public void call(final Subscriber<? super T> subscriber) {
                pages = PublishSubject.create();
                subscription = Observable.switchOnNext(pages).subscribe(new PageSubscriber(subscriber));
                subscriber.add(subscription);
                pages.onNext(source);
            }
        });
    }

    /**
     * Returns the last page received from the pager. You may use this to
     * retry that observable in case it failed the first time around.
     */
    public Observable<T> currentPage() {
        return page(nextPage);
    }

    /**
     * @return true, if there are more pages to be emitted.
     */
    public boolean hasNext() {
        return nextPage != FINISH_SEQUENCE;
    }

    /**
     * Advances the pager by pushing the next page of items into the current observer, is there is one. If the pager
     * has been unsubscribed from or there are no more pages, this method does nothing.
     */
    public void next() {
        if (!subscription.isUnsubscribed() && hasNext()) {
            pages.onNext(nextPage);
        }
    }

    public interface PagingFunction<T> extends Func1<T, Observable<T>> {
    }

    private final class PageSubscriber extends Subscriber<T> {
        private final Subscriber<? super T> inner;

        public PageSubscriber(Subscriber<? super T> inner) {
            this.inner = inner;
        }

        @Override
        public void onCompleted() {
            inner.onCompleted();
        }

        @Override
        public void onError(Throwable e) {
            inner.onError(e);
        }

        @Override
        public void onNext(T result) {
            nextPage = pagingFunction.call(result);
            inner.onNext(result);
            if (nextPage == FINISH_SEQUENCE) {
                pages.onCompleted();
            }
        }
    }
}

