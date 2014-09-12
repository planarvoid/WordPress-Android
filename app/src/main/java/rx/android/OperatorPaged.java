package rx.android;

import rx.Observable;
import rx.Subscriber;
import rx.functions.Func1;

import java.util.Collections;

@SuppressWarnings("unchecked")
public final class OperatorPaged<CollT extends Iterable<?>> implements Observable.Operator<OperatorPaged.Page<CollT>, CollT> {

    private static final Observable EMPTY_OBSERVABLE = Observable.empty();
    private static final Page EMPTY_PAGE = new Page(Collections.emptyList(), EMPTY_OBSERVABLE);
    private static final Observable EMPTY_PAGE_OBSERVABLE = Observable.just(EMPTY_PAGE);

    private final LegacyPager<CollT> pager;

    public static <CollT extends Iterable<?>> OperatorPaged<CollT> pagedWith(LegacyPager<CollT> pager) {
        return new OperatorPaged<CollT>(pager);
    }

    public static <CollT extends Iterable<?>> Page<CollT> emptyPage() {
        return EMPTY_PAGE;
    }

    public static <CollT extends Iterable<?>> Observable<Page<CollT>> emptyObservable() {
        return EMPTY_OBSERVABLE;
    }

    public static <CollT extends Iterable<?>> Observable<Page<CollT>> emptyPageObservable() {
        return EMPTY_PAGE_OBSERVABLE;
    }

    OperatorPaged(LegacyPager<CollT> pager) {
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

    public interface LegacyPager<CollT extends Iterable<?>> extends Func1<CollT, Observable<Page<CollT>>> {};
}