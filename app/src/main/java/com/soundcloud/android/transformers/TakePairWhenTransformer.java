package com.soundcloud.android.transformers;

import rx.Observable;

import android.support.annotation.NonNull;
import android.util.Pair;

public final class TakePairWhenTransformer<S, T> implements Observable.Transformer<S, Pair<S, T>> {
    @NonNull private final Observable<T> when;

    public TakePairWhenTransformer(final @NonNull Observable<T> when) {
        this.when = when;
    }

    @Override
    @NonNull
    public Observable<Pair<S, T>> call(final @NonNull Observable<S> source) {
        return when.withLatestFrom(source, (x, y) -> new Pair<>(y, x));
    }
}
