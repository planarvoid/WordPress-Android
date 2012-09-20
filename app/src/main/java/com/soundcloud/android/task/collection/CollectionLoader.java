package com.soundcloud.android.task.collection;

import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.model.ScModel;
import com.soundcloud.android.model.ScResource;

public abstract class CollectionLoader<T extends ScModel> {
    abstract ReturnData<T> load(AndroidCloudAPI application, CollectionParams<T> params);
}
