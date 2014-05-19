package com.soundcloud.android.view;

import rx.Observable;

import android.widget.AdapterView;

public interface ReactiveListComponent<ObservableT extends Observable<?>>
        extends ReactiveComponent<ObservableT>, AdapterView.OnItemClickListener {
}
