package com.soundcloud.android.rx.android;

import static com.soundcloud.android.Expect.expect;
import static org.junit.Assert.fail;

import rx.Observer;

public class ErrorMatchingObserver<T> implements Observer<T> {

    private Class<? extends Exception> clazz;

    private ErrorMatchingObserver(Class<? extends Exception> clazz){
        this.clazz = clazz;
    }

    @Override
    public void onCompleted() {
        fail("On completed call when expecting an exception");
    }

    @Override
    public void onError(Exception e) {
        expect(e.getClass()).toEqual((Class)clazz);
    }

    @Override
    public void onNext(T args) {
        fail("OnNext call when expecting an exception");
    }

    public static <T> ErrorMatchingObserver<T> errorMatchingObserver(Class<? extends Exception> e){
        return new ErrorMatchingObserver<T>(e);
    }
}
