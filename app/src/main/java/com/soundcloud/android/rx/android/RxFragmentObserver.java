package com.soundcloud.android.rx.android;

import rx.Observer;

import android.os.Looper;
import android.support.v4.app.Fragment;

public class RxFragmentObserver<T extends Fragment, R> extends RxFragmentHandler<T>
        implements Observer<R>, RxFragmentOnNextMethods<T, R>, RxFragmentOnCompletedMethods<T>, RxFragmentOnErrorMethods<T> {


    public RxFragmentObserver(T fragment) {
        super(fragment);
    }

    @Override
    public final void onCompleted() {
        assertUiThread();
        T fragment = getFragmentForCallback();
        if (fragment != null) onCompleted(fragment);
    }

    @Override
    public final void onError(Exception e) {
        if (!(e instanceof IllegalThreadException)) {
            assertUiThread();
        }
        T fragment = getFragmentForCallback();
        if (fragment != null) onError(fragment, e);
    }

    @Override
    public final void onNext(R element) {
        assertUiThread();
        T fragment = getFragmentForCallback();
        if (fragment != null) onNext(fragment, element);
    }

    @Override
    public void onCompleted(T fragment) {
    }

    @Override
    public void onError(T fragment, Exception error) {
    }

    @Override
    public void onNext(T fragment, R element) {
    }

    private void assertUiThread() {
        if (Looper.getMainLooper() != Looper.myLooper()) {
            throw new IllegalThreadException("Fragment observers must run on the UI thread. Add observeOn call.");
        }
    }
}
