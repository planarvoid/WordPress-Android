package com.soundcloud.android.dao;

import com.soundcloud.android.model.ScResource;

public interface Storage<T extends ScResource> {

    void create(T resource);

}
