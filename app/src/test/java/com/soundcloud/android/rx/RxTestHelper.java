package com.soundcloud.android.rx;

import static rx.android.OperatorPaged.Page;
import static rx.android.OperatorPaged.pagedWith;

import rx.Observable;
import rx.android.OperatorPaged;
import rx.android.Pager;

public class RxTestHelper {

    public static <CollT extends Iterable<?>> Observable<Page<CollT>> singlePage(Observable<CollT> source) {
        return source.lift(pagedWith(endlessPagerFrom(Observable.<CollT>empty())));
    }

    public static <CollT extends Iterable<?>> OperatorPaged.LegacyPager<CollT> endlessPagerFrom(final Observable<CollT> observable) {
        return new OperatorPaged.LegacyPager<CollT>() {
            @Override
            public Observable<Page<CollT>> call(CollT collection) {
                final OperatorPaged.LegacyPager<CollT> recursivePager = endlessPagerFrom(observable);
                return observable.lift(pagedWith(recursivePager));
            }
        };
    }

    public static <T> Pager.NextPageFunc<T> withNextPage(final Observable<T> nextPage) {
        return new Pager.NextPageFunc<T>() {
            @Override
            public Observable<T> call(T t) {
                return nextPage;
            }
        };
    }

    public static <T> Pager<T> pagerWithSinglePage() {
        return Pager.create(withNextPage(Pager.<T>finish()));
    }
}
