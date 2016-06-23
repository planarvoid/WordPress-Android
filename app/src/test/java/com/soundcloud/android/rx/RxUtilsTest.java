package com.soundcloud.android.rx;

import static com.soundcloud.android.rx.RxUtils.continueWith;
import static com.soundcloud.android.rx.RxUtils.returning;
import static java.util.Arrays.asList;

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

    @Test
    public void concatEagerIgnorePartialErrorsOnSuccess() {
        final TestSubscriber<Integer> subscriber = new TestSubscriber<>();
        Observable
                .just(asList(Observable.just(1), Observable.just(2)))
                .compose(RxUtils.<Integer>concatEagerIgnorePartialErrors())
                .subscribe(subscriber);
        subscriber.assertReceivedOnNext(asList(1, 2));
        subscriber.assertCompleted();
        subscriber.assertNoErrors();
    }

    @Test
    public void concatEagerIgnorePartialErrorsOnPartialSuccess() {
        final TestSubscriber<Integer> subscriber = new TestSubscriber<>();
        Observable
                .just(asList(Observable.just(1), Observable.<Integer>error(new RuntimeException()), Observable.just(2)))
                .compose(RxUtils.<Integer>concatEagerIgnorePartialErrors())
                .subscribe(subscriber);
        subscriber.assertReceivedOnNext(asList(1, 2));
        subscriber.assertCompleted();
        subscriber.assertNoErrors();
    }

    @Test
    public void concatEagerIgnorePartialErrorsOnFail() {
        final TestSubscriber<Integer> subscriber = new TestSubscriber<>();
        Observable
                .just(asList(Observable.<Integer>error(new RuntimeException()),
                             Observable.<Integer>error(new RuntimeException())))
                .compose(RxUtils.<Integer>concatEagerIgnorePartialErrors())
                .subscribe(subscriber);
        subscriber.assertNoValues();
        subscriber.assertNotCompleted();
        subscriber.assertError(RuntimeException.class);
    }
}
