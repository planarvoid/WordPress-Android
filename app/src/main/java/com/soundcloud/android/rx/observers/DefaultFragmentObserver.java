package com.soundcloud.android.rx.observers;

import com.soundcloud.android.utils.ErrorUtils;
import rx.android.RxFragmentObserver;

import android.support.v4.app.Fragment;

public class DefaultFragmentObserver<T extends Fragment, R> extends RxFragmentObserver<T, R> {
    public DefaultFragmentObserver(T fragment) {
        super(fragment);
    }

    @Override
    public void onError(Throwable error) {
        ErrorUtils.handleThrowable(error);
        super.onError(error);
    }
}
