package rx.android;

import rx.Observable;
import rx.Observer;
import rx.Subscription;
import rx.util.functions.Func1;

import java.util.Collections;
import java.util.Iterator;

public final class OperationPaged {

    private static final Observable EMPTY_OBSERVABLE = Observable.empty();

    @SuppressWarnings("unchecked")
    private static final Page EMPTY_PAGE = new Page(Collections.emptyList(), EMPTY_OBSERVABLE);

    @SuppressWarnings("unchecked")
    public static <T> Page<T> emptyPage() {
        return EMPTY_PAGE;
    }

    @SuppressWarnings("unchecked")
    public static <T> Observable<Page<T>> emptyPageObservable() {
        return EMPTY_OBSERVABLE;
    }

    public static class Page<ItemT> implements Iterable<ItemT> {

        private final Iterable<ItemT> mPagedCollection;
        private final Observable<Page<ItemT>> mNextPageObservable;

        public Page(Iterable<ItemT> pagedCollection, Observable<Page<ItemT>> nextPageObservable) {
            this.mPagedCollection = pagedCollection;
            this.mNextPageObservable = nextPageObservable;
        }

        public Iterable<ItemT> getContent() {
            return mPagedCollection;
        }

        public boolean hasNextPage() {
            return mNextPageObservable != EMPTY_OBSERVABLE;
        }

        public Observable<Page<ItemT>> getNextPage() {
            return mNextPageObservable;
        }

        @Override
        public Iterator<ItemT> iterator() {
            return mPagedCollection.iterator();
        }
    }

    public static <ItemT, CollT extends Iterable<ItemT>> Func1<Observer<Page<ItemT>>, Subscription> paged(final Observable<CollT> source,
                                                                                                          final Func1<CollT, Observable<Page<ItemT>>> nextPageGenerator) {
        return new Func1<Observer<Page<ItemT>>, Subscription>() {
            @Override
            public Subscription call(final Observer<Page<ItemT>> observer) {
                return source.subscribe(new Observer<CollT>() {
                    @Override
                    public void onCompleted() {
                    }

                    @Override
                    public void onError(Throwable e) {
                        observer.onError(e);
                    }

                    @Override
                    public void onNext(CollT pagedCollection) {
                        final Observable<Page<ItemT>> nextPageObservable = nextPageGenerator.call(pagedCollection);
                        observer.onNext(new Page<ItemT>(pagedCollection, nextPageObservable));
                        if (nextPageObservable == EMPTY_OBSERVABLE) {
                            observer.onCompleted();
                        }
                    }
                });
            }
        };
    }
}