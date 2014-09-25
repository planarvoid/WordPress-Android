package com.soundcloud.android.rx;

import static rx.android.OperatorPaged.Page;
import static rx.android.OperatorPaged.pagedWith;

import rx.Observable;
import rx.android.OperatorPaged;
import rx.android.Pager;

public class RxTestHelper {

    public static <CollT extends Iterable<?>> OperatorPaged.LegacyPager<CollT> endlessPagerFrom(final Observable<CollT> observable) {
        return new OperatorPaged.LegacyPager<CollT>() {
            @Override
            public Observable<Page<CollT>> call(CollT collection) {
                final OperatorPaged.LegacyPager<CollT> recursivePager = endlessPagerFrom(observable);
                return observable.lift(pagedWith(recursivePager));
            }
        };
    }

    public static <T> Pager<T> pagerWithNextPage(final Observable<T> nextPage) {
        return new Pager<T>() {
            @Override
            public Observable<T> call(T t) {
                return nextPage;
            }
        };
    }

    public static <T> Pager<T> pagerWithSinglePage() {
        return pagerWithNextPage(Pager.<T>finish());
    }
}
