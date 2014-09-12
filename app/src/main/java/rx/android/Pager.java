package rx.android;

import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.functions.Func1;
import rx.subjects.PublishSubject;
import rx.subscriptions.Subscriptions;

public final class Pager<T> {

    private static final Observable FINISH_SEQUENCE = Observable.never();

    private final NextPageFunc<T> nextPageFunc;
    private PublishSubject<Observable<T>> pages;
    private Observable<T> nextPage = FINISH_SEQUENCE;
    private Subscription subscription = Subscriptions.empty();

    public static <T> Observable<T> finish() {
        return FINISH_SEQUENCE;
    }

    public static <T> Pager<T> create(NextPageFunc<T> nextPageFunc) {
        return new Pager<>(nextPageFunc);
    }

    private Pager(NextPageFunc<T> nextPageFunc) {
        this.nextPageFunc = nextPageFunc;
    }

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

    public boolean hasNext() {
        return nextPage != FINISH_SEQUENCE;
    }

    public void next() {
        if (!subscription.isUnsubscribed() && hasNext()) {
            pages.onNext(nextPage);
        }
    }

    public interface NextPageFunc<CollT> extends Func1<CollT, Observable<CollT>> {
    }
}
