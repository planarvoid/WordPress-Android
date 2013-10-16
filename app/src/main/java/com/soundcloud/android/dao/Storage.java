package com.soundcloud.android.dao;

import rx.Observable;


public interface Storage<T> {
    T store(T resource);

    Observable<T> storeAsync(T resource);

}
