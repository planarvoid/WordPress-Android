package com.soundcloud.android.storage;

import rx.Observable;

@Deprecated
public interface Storage<T> {
    T store(T resource);

    Observable<T> storeAsync(T resource);

}
