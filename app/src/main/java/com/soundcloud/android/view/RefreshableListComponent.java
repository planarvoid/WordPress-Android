package com.soundcloud.android.view;

import rx.Observable;

public interface RefreshableListComponent<ObservableT extends Observable<?>>
        extends ReactiveListComponent<ObservableT> {

    ObservableT refreshObservable();

}
