package com.soundcloud.android.transformers;

import rx.Observable;

import android.support.annotation.NonNull;

public class Transformers {
    /**
     * Emits the latest value of the source observable whenever the `when`
     * observable emits.
     */
    public static <S, T> TakeWhenTransformer<S, T> takeWhen(final @NonNull Observable<T> when) {
        return new TakeWhenTransformer<>(when);
    }
}
