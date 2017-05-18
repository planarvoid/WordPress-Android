package com.soundcloud.android.testsupport;

import com.soundcloud.rx.Pager;
import rx.Observable;

public class TestPager {

    public static <T> Pager.PagingFunction<T> singlePageFunction() {
        return new Pager.PagingFunction<T>() {
            @Override
            public Observable<T> call(T t) {
                return Pager.finish();
            }
        };
    }
}
