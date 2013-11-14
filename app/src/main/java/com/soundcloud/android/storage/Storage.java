package com.soundcloud.android.storage;

import rx.Observable;


public interface Storage<T> {
    T store(T resource);

    Observable<T> storeAsync(T resource);

}
