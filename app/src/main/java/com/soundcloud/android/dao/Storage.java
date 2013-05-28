package com.soundcloud.android.dao;

import com.soundcloud.android.model.behavior.Identifiable;
import rx.Observable;


public interface Storage<T extends Identifiable> {

    Observable<T> create(T resource);

}
