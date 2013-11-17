package com.soundcloud.android.collections.tasks;

import com.soundcloud.android.api.PublicCloudAPI;
import com.soundcloud.android.model.ScModel;

interface CollectionLoader<T extends ScModel> {
    ReturnData<T> load(PublicCloudAPI application, CollectionParams<T> params);
}
