package com.soundcloud.android.transformers;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.ObservableTransformer;

import android.support.annotation.NonNull;

final class TakeWhenTransformerV2<S, T> implements ObservableTransformer<S, S> {
    @NonNull private final Observable<T> when;

    TakeWhenTransformerV2(final @NonNull Observable<T> when) {
        this.when = when;
    }

    @Override public ObservableSource<S> apply(Observable<S> upstream) {
        return when.withLatestFrom(upstream, (__, x) -> x);
    }
}
