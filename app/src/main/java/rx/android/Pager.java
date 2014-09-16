package rx.android;

import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.functions.Func1;
import rx.subjects.PublishSubject;
import rx.subscriptions.Subscriptions;

public class Pager<T> {

    private static final Observable FINISH_SEQUENCE = Observable.never();

    private final NextPageFunc<T> nextPageFunc;
    private PublishSubject<Observable<T>> pages;
    private Observable<T> nextPage = FINISH_SEQUENCE;
    private Subscription subscription = Subscriptions.empty();

    /**
     * Used in a {@link rx.android.Pager.NextPageFunc} to signal the caller that no more pages are available, i.e.
     * to finish paging by completing the paged sequence.
     *
     * @return the finish token
     */
    public static <T> Observable<T> finish() {
        return FINISH_SEQUENCE;
    }

    /**
     * Creates a new pager using the given paging function.
     *
     * @param nextPageFunc describes how from the previous item of the paged observable, the next page i.e. observable
     *                     is obtained. The given function must signal to the pager that no more pages are available
     *                     by returning the finish sequence {@link #finish()}
     */
    public static <T> Pager<T> create(NextPageFunc<T> nextPageFunc) {
        return new Pager<>(nextPageFunc);
    }

    private Pager(NextPageFunc<T> nextPageFunc) {
        this.nextPageFunc = nextPageFunc;
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
                subscription = Observable.switchOnNext(pages).subscribe(new Subscriber<T>() {
                    @Override
                    public void onCompleted() {
                        subscriber.onCompleted();
                    }

                    @Override
                    public void onError(Throwable e) {
                        subscriber.onError(e);
                    }

                    @Override
                    public void onNext(T result) {
                        nextPage = nextPageFunc.call(result);
                        subscriber.onNext(result);
                        if (nextPage == FINISH_SEQUENCE) {
                            pages.onCompleted();
                        }
                    }
                });
                subscriber.add(subscription);
                pages.onNext(source);
            }
        });
    }

    /**
     * Returns the last page received from the given {@link rx.android.Pager.NextPageFunc}. You may use this to
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

    /**
     * Describes how from a previously emitted page, construct the following page as an Observable.
     */
    public interface NextPageFunc<T> extends Func1<T, Observable<T>> {
    }
}
