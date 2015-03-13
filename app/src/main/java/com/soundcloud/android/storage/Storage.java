package com.soundcloud.android.storage;

@Deprecated
public interface Storage<T> {
    T store(T resource);
}
