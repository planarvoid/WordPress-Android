package com.soundcloud.android.transformers;

import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.ObservableTransformer;

import android.support.annotation.NonNull;
import android.util.Pair;

public final class TakePairWhenTransformerV2<S, T> implements ObservableTransformer<S, Pair<S, T>> {
    @NonNull private final Observable<T> when;

    public TakePairWhenTransformerV2(final @NonNull Observable<T> when) {
        this.when = when;
    }

    @Override
    public ObservableSource<Pair<S, T>> apply(@io.reactivex.annotations.NonNull Observable<S> upstream) {
        return when.withLatestFrom(upstream, (x, y) -> new Pair<>(y, x));
    }
}
