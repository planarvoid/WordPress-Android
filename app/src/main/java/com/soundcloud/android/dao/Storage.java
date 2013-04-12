package com.soundcloud.android.dao;

import com.soundcloud.android.model.ModelLike;

public interface Storage<T extends ModelLike> {

    void create(T resource);

}
