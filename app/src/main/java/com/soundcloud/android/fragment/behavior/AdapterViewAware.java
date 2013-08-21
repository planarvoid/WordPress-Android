package com.soundcloud.android.fragment.behavior;

import rx.Observer;

public interface AdapterViewAware<ModelType> {

    void setEmptyViewStatus(int status);
    Observer<ModelType> getAdapterObserver();

}
