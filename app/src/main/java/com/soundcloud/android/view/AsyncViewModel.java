package com.soundcloud.android.view;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class AsyncViewModel<T> {
    public static <T> AsyncViewModel<T> fromRefreshing(T data) {
        return new AutoValue_AsyncViewModel<T>(data, true);
    }

    public static <T> AsyncViewModel<T> fromIdle(T data) {
        return new AutoValue_AsyncViewModel<T>(data, false);
    }

    public static <T> AsyncViewModel<T> create(T data, boolean isRefreshing) {
        return new AutoValue_AsyncViewModel<T>(data, isRefreshing);
    }

    public abstract T data();

    public abstract boolean isRefreshing();
}
