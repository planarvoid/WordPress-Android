package com.soundcloud.android.rx.android;

import android.support.v4.app.Fragment;

interface RxFragmentOnErrorMethods<T extends Fragment> {

    abstract void onError(T fragment, Exception error);

}
