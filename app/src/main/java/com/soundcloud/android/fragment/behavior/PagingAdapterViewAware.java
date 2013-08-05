package com.soundcloud.android.fragment.behavior;

import com.soundcloud.android.adapter.EndlessPagingAdapter;

public interface PagingAdapterViewAware<ModelType> extends AdapterViewAware<ModelType> {

    @Override
    EndlessPagingAdapter<ModelType> getAdapter();

}
