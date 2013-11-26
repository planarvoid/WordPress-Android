package com.soundcloud.android.dagger;

import rx.Observable;
import rx.android.AndroidObservables;

import android.support.v4.app.Fragment;

public class AndroidObservableFactory {

    private final Observable mObservable;

    public AndroidObservableFactory(Observable observable) {
        mObservable = observable;
    }

    public Observable create(Fragment fragment){
        return AndroidObservables.fromFragment(fragment, mObservable);
    }

}
