package com.soundcloud.android.view.adapters;

import rx.Observer;

public interface ReactiveItemAdapter<ItemT> extends Observer<Iterable<ItemT>> {

    void clear();
}
