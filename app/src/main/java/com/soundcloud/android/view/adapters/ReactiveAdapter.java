package com.soundcloud.android.view.adapters;

import rx.Observer;

@Deprecated // remove once we remove the old PullToRefreshController
public interface ReactiveAdapter<T> extends Observer<T> {

    void clear();

}
