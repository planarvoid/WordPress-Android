package com.soundcloud.android.rx.android;


import static org.junit.Assert.fail;

import rx.Observer;

public class ErrorRaisingObserver<T> implements Observer {
    public static <T> ErrorRaisingObserver errorRaisingObserver(){
        return new ErrorRaisingObserver<T>();
    }

    private ErrorRaisingObserver(){
        //dont use constructor
    }

    @Override
    public void onCompleted() {
    }

    @Override
    public void onError(Exception e) {
        fail("Test raised an unexpected exception/error when running observable : " + e.toString());
    }

    @Override
    public void onNext(Object args) {
    }
}
