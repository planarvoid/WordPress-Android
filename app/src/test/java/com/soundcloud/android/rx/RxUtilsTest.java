package com.soundcloud.android.rx;

import static com.soundcloud.android.rx.RxUtils.continueWith;
import static com.soundcloud.android.rx.RxUtils.returning;

import org.junit.Test;
import rx.Observable;
import rx.observers.TestSubscriber;

public class RxUtilsTest {

    @Test
    public void returningShouldIgnoreSequenceResultAndEmitGivenConstant() throws Exception {
        TestSubscriber<Boolean> subscriber = new TestSubscriber<>();
        Observable.just("string").map(returning(true)).subscribe(subscriber);

        subscriber.assertValue(true);
    }

    @Test
    public void continueWithShouldIgnoreSequenceResultAndRedirectToContinuation() throws Exception {
        TestSubscriber<Boolean> subscriber = new TestSubscriber<>();
        Observable.just("string").flatMap(continueWith(Observable.just(true))).subscribe(subscriber);

        subscriber.assertValue(true);
    }
}
