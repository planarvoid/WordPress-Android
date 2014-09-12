package com.soundcloud.android.view.adapters;

import rx.Observer;

public interface ReactiveAdapter<T> extends Observer<T> {

    void clear();

}
