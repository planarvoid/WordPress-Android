package com.soundcloud.android.rx.android;

import android.support.v4.app.Fragment;

interface RxFragmentOnNextMethods<T extends Fragment, R> {

    abstract void onNext(T fragment, R element);

}
