package com.soundcloud.android.fragment.behavior;

import com.soundcloud.android.adapter.ItemAdapter;
import com.soundcloud.android.view.EmptyListView;

public interface AdapterViewAware<ModelType> {

    EmptyListView getEmptyView();
    ItemAdapter<ModelType> getAdapter();

}
