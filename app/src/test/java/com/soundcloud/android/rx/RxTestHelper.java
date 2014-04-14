package com.soundcloud.android.rx;

import static rx.android.OperatorPaged.Page;
import static rx.android.OperatorPaged.Pager;
import static rx.android.OperatorPaged.pagedWith;

import rx.Observable;
import rx.android.OperatorPaged;

public class RxTestHelper {

    public static <CollT extends Iterable<?>> Observable<Page<CollT>> singlePage(Observable<CollT> source) {
        return source.lift(pagedWith(endlessPagerFrom(Observable.<CollT>empty())));
    }

    public static <CollT extends Iterable<?>> Page<CollT> singlePage(CollT source) {
        return new Page<CollT>(source, OperatorPaged.<CollT>emptyPageObservable());
    }

    public static <CollT extends Iterable<?>> Pager<CollT> endlessPagerFrom(final Observable<CollT> observable) {
        return new Pager<CollT>() {
            @Override
            public Observable<Page<CollT>> call(CollT collection) {
                final Pager<CollT> recursivePager = endlessPagerFrom(observable);
                return observable.lift(pagedWith(recursivePager));
            }
        };
    }
}
