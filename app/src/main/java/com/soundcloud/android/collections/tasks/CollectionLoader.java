package com.soundcloud.android.collections.tasks;

import com.soundcloud.android.api.legacy.PublicApi;
import com.soundcloud.android.api.legacy.model.ScModel;

interface CollectionLoader<T extends ScModel> {
    ReturnData<T> load(PublicApi application, CollectionParams<T> params);
}
