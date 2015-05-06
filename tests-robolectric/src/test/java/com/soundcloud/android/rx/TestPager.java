package com.soundcloud.android.rx;

import rx.Observable;
import rx.android.NewPager;
import rx.android.Pager;

public class TestPager {

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

    public static <T> NewPager.PagingFunction<T> singlePageFunction() {
        return new NewPager.PagingFunction<T>() {
            @Override
            public Observable<T> call(T t) {
                return NewPager.finish();
            }
        };
    }
}
