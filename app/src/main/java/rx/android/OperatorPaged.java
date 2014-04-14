package rx.android;

import rx.Observable;
import rx.Subscriber;
import rx.functions.Func1;

import java.util.Collections;

public final class OperatorPaged<CollT extends Iterable<?>> implements Observable.Operator<OperatorPaged.Page<CollT>, CollT> {

    private static final Observable EMPTY_OBSERVABLE = Observable.empty();

    @SuppressWarnings("unchecked")
    private static final Page EMPTY_PAGE = new Page(Collections.emptyList(), EMPTY_OBSERVABLE);

    private final Pager<CollT> pager;

    public static <CollT extends Iterable<?>> OperatorPaged<CollT> pagedWith(Pager<CollT> pager) {
        return new OperatorPaged<CollT>(pager);
    }

    @SuppressWarnings("unchecked")
    public static <T> Page<? extends Iterable<T>> emptyPage() {
        return EMPTY_PAGE;
    }

    @SuppressWarnings("unchecked")
    public static <CollT extends Iterable<?>> Observable<Page<CollT>> emptyPageObservable() {
        return EMPTY_OBSERVABLE;
    }

    OperatorPaged(Pager<CollT> pager) {
        this.pager = pager;
    }

    @Override
    public Subscriber<? super CollT> call(final Subscriber<? super Page<CollT>> subscriber) {
        return new Subscriber<CollT>() {
            @Override
            public void onCompleted() {
                // nop
            }

            @Override
            public void onError(Throwable e) {
                subscriber.onError(e);
            }

            @Override
            public void onNext(CollT collection) {
                final Observable<Page<CollT>> nextPageObservable = pager.call(collection);
                subscriber.onNext(new Page<CollT>(collection, nextPageObservable));
                subscriber.onCompleted();
            }
        };
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

    public interface Pager<CollT extends Iterable<?>> extends Func1<CollT, Observable<Page<CollT>>> {};
}