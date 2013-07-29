package com.soundcloud.android.rx.observers;

import com.soundcloud.android.SoundCloudApplication;
import rx.android.RxFragmentObserver;

import android.support.v4.app.Fragment;

public class ScFragmentObserver<T extends Fragment, R> extends RxFragmentObserver<T, R> {
    public ScFragmentObserver(T fragment) {
        super(fragment);
    }

    @Override
    public void onError(Exception error) {
        SoundCloudApplication.handleSilentException(error.getMessage(), error);
        error.printStackTrace();
        super.onError(error);
    }
}
