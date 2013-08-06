package com.soundcloud.android.fragment.behavior;

import com.soundcloud.android.adapter.ItemAdapter;

public interface AdapterViewAware<ModelType> {

    void setEmptyViewStatus(int status);
    ItemAdapter<ModelType> getAdapter();

}
