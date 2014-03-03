package rx.android;

import rx.Observable;
import rx.Observer;
import rx.Subscriber;
import rx.Subscription;
import rx.functions.Func1;

import java.util.Collections;

public final class OperationPaged {

    private static final Observable EMPTY_OBSERVABLE = Observable.empty();

    @SuppressWarnings("unchecked")
    private static final Page EMPTY_PAGE = new Page(Collections.emptyList(), EMPTY_OBSERVABLE);

    @SuppressWarnings("unchecked")
    public static <T> Page<? extends Iterable<T>> emptyPage() {
        return EMPTY_PAGE;
    }

    @SuppressWarnings("unchecked")
    public static <CollT extends Iterable<?>> Observable<Page<CollT>> emptyPageObservable() {
        return EMPTY_OBSERVABLE;
    }

    public static class Page<CollT extends Iterable<?>> {

        private final CollT mPagedCollection;
        private final Observable<Page<CollT>> mNextPageObservable;

        public Page(CollT pagedCollection, Observable<Page<CollT>> nextPageObservable) {
            this.mPagedCollection = pagedCollection;
            this.mNextPageObservable = nextPageObservable;
        }

        public boolean hasNextPage() {
            return mNextPageObservable != EMPTY_OBSERVABLE;
        }

        public Observable<Page<CollT>> getNextPage() {
            return mNextPageObservable;
        }

        public CollT getPagedCollection() {
            return mPagedCollection;
        }
    }

    public static <CollT extends Iterable<?>> Observable.OnSubscribe<Page<CollT>> paged(
            final Observable<CollT> source, final Func1<CollT, Observable<Page<CollT>>> nextPageGenerator) {
        return new Observable.OnSubscribe<Page<CollT>>() {
            @Override
            public void call(final Subscriber<? super Page<CollT>> subscriber) {
                Subscription wrapped = source.subscribe(new Subscriber<CollT>() {
                    @Override
                    public void onCompleted() {
                    }

                    @Override
                    public void onError(Throwable e) {
                        subscriber.onError(e);
                    }

                    @Override
                    public void onNext(CollT pagedCollection) {
                        final Observable<Page<CollT>> nextPageObservable = nextPageGenerator.call(pagedCollection);
                        subscriber.onNext(new Page<CollT>(pagedCollection, nextPageObservable));
                        subscriber.onCompleted();
                    }
                });
                subscriber.add(wrapped);
            }
        };
    }

    public static <CollT extends Iterable<?>> Func1<CollT, Observable<Page<CollT>>> nextPageFrom(final Observable<CollT> observable) {
        return new Func1<CollT, Observable<Page<CollT>>>() {
            @Override
            public Observable<Page<CollT>> call(CollT collection) {
                return Observable.create(paged(observable, nextPageFrom(observable)));
            }
        };
    }

}