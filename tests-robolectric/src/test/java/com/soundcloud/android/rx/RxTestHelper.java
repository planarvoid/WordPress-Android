package com.soundcloud.android.rx;

import rx.Observable;
import rx.android.Pager;

public class RxTestHelper {

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
