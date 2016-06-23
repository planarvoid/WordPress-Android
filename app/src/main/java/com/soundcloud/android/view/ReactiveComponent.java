package com.soundcloud.android.view;

import rx.Observable;
import rx.Subscription;

public interface ReactiveComponent<ObservableT extends Observable<?>> {

    ObservableT buildObservable();

    Subscription connectObservable(ObservableT observable);

}
