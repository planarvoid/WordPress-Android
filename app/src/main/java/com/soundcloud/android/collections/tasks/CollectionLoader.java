package com.soundcloud.android.collections.tasks;

import com.soundcloud.android.api.legacy.PublicApiWrapper;
import com.soundcloud.android.model.ScModel;

interface CollectionLoader<T extends ScModel> {
    ReturnData<T> load(PublicApiWrapper application, CollectionParams<T> params);
}
