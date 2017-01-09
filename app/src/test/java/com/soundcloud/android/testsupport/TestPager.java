package com.soundcloud.android.testsupport;

import com.soundcloud.rx.Pager;
import rx.Observable;
import rx.android.LegacyPager;

public class TestPager {

    @Deprecated
    public static <T> LegacyPager<T> pagerWithNextPage(final Observable<T> nextPage) {
        return new LegacyPager<T>() {
            @Override
            public Observable<T> call(T t) {
                return nextPage;
            }
        };
    }

    @Deprecated
    public static <T> LegacyPager<T> pagerWithSinglePage() {
        return pagerWithNextPage(LegacyPager.<T>finish());
    }

    public static <T> Pager.PagingFunction<T> singlePageFunction() {
        return t -> Pager.finish();
    }
}
