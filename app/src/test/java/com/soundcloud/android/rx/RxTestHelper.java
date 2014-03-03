package com.soundcloud.android.rx;

import static rx.android.OperationPaged.Page;
import static rx.android.OperationPaged.paged;

import rx.Observable;
import rx.android.OperationPaged;

public class RxTestHelper {

    public static <CollT extends Iterable<?>> Observable<Page<CollT>> singlePage(Observable<CollT> source) {
        return Observable.create(paged(source, OperationPaged.nextPageFrom(Observable.<CollT>empty())));
    }
}
