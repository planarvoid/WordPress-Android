package com.soundcloud.android.dao;

import com.soundcloud.android.model.behavior.ModelLike;
import rx.Observable;


public interface Storage<T extends ModelLike> {

    Observable<T> create(T resource);

}
