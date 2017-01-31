package com.soundcloud.android.view;

import static com.soundcloud.java.optional.Optional.of;

import com.google.auto.value.AutoValue;
import com.soundcloud.java.optional.Optional;

@AutoValue
public abstract class AsyncViewModel<ViewModelType> {

    public static <T> AsyncViewModel<T> fromRefreshing(T data) {
        return new AutoValue_AsyncViewModel<>(of(data), true, Optional.absent());
    }

    public static <T> AsyncViewModel<T> fromIdle(T data) {
        return new AutoValue_AsyncViewModel<>(of(data), false, Optional.absent());
    }

    public static <T> AsyncViewModel<T> create(Optional<T> optData, boolean isRefreshing, Optional<ViewError> viewErrorOptional) {
        return new AutoValue_AsyncViewModel<>(optData, isRefreshing, viewErrorOptional);
    }

    public abstract Optional<ViewModelType> data();

    public abstract boolean isRefreshing();

    public abstract Optional<ViewError> error();

}
