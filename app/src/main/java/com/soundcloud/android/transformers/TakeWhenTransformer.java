package com.soundcloud.android.transformers;

import rx.Observable;

import android.support.annotation.NonNull;

final class TakeWhenTransformer<S, T> implements Observable.Transformer<S, S> {
    @NonNull private final Observable<T> when;

    TakeWhenTransformer(final @NonNull Observable<T> when) {
        this.when = when;
    }

    @Override
    @NonNull
    public Observable<S> call(final @NonNull Observable<S> source) {
        return when.withLatestFrom(source, (__, x) -> x);
    }
}