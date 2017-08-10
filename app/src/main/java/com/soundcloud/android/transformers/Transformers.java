package com.soundcloud.android.transformers;

import com.soundcloud.android.rx.ObservableDoOnFirst;
import io.reactivex.Observable;
import io.reactivex.functions.Consumer;

import android.support.annotation.NonNull;

public class Transformers {
    /**
     * Emits the latest value of the source observable whenever the `when`
     * observable emits.
     */
    public static <S, T> TakeWhenTransformerV2<S, T> takeWhenV2(final @NonNull Observable<T> when) {
        return new TakeWhenTransformerV2<>(when);
    }

    /**
     * Emits the latest value of the source `when` observable whenever the
     * `when` observable emits.
     */
    public static <S, T> TakePairWhenTransformerV2<S, T> takePairWhenV2(final @NonNull Observable<T> when) {
        return new TakePairWhenTransformerV2<>(when);
    }

    public static <T> ObservableDoOnFirst<T> doOnFirst(final Consumer<T> action) {
        return new ObservableDoOnFirst<>(action);
    }
}
