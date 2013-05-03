package com.soundcloud.android.task.collection;

import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.model.ScModel;

interface CollectionLoader<T extends ScModel> {
    ReturnData<T> load(AndroidCloudAPI application, CollectionParams<T> params);
}
