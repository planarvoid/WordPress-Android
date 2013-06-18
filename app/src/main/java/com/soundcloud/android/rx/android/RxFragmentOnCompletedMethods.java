package com.soundcloud.android.rx.android;

import android.support.v4.app.Fragment;

interface RxFragmentOnCompletedMethods<T extends Fragment> {

    abstract void onCompleted(T fragment);

}
