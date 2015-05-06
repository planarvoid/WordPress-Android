package com.soundcloud.android.rx;

import rx.Observable;
import rx.android.NewPager;
import rx.android.Pager;
import rx.internal.util.UtilityFunctions;

public class RxTestHelper {

    public static <T> Pager<T> pagerWithNextPage(final Observable<T> nextPage) {
        return new Pager<T>() {
            @Override
            public Observable<T> call(T t) {
                return nextPage;
            }
        };
    }

    public static <T> NewPager<T, T> newPagerWithNextPage(final Observable<T> nextPage) {
        return NewPager.create(new NewPager.PagingFunction<T>() {

            @Override
            public Observable<T> call(T o) {
                return nextPage;
            }
        }, UtilityFunctions.<T>identity());
    }

    public static <T> Pager<T> pagerWithSinglePage() {
        return pagerWithNextPage(Pager.<T>finish());
    }

    public static <T> NewPager<T, T> newPagerWithSinglePage() {
        return newPagerWithNextPage(NewPager.<T>finish());
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
