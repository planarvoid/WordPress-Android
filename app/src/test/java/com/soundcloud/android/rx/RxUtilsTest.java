package com.soundcloud.android.rx;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

import com.soundcloud.java.optional.Optional;
import org.junit.Test;
import rx.Observable;
import rx.functions.Func1;
import rx.observers.TestSubscriber;

public class RxUtilsTest {

    @Test
    public void returningShouldIgnoreSequenceResultAndEmitGivenConstant() throws Exception {
        TestSubscriber<Boolean> subscriber = new TestSubscriber<>();
        Observable.just("string").map(o -> true).subscribe(subscriber);

        subscriber.assertValue(true);
    }

    @Test
    public void continueWithShouldIgnoreSequenceResultAndRedirectToContinuation() throws Exception {
        TestSubscriber<Boolean> subscriber = new TestSubscriber<>();
        Observable.just("string").flatMap(o -> Observable.just(true)).subscribe(subscriber);

        subscriber.assertValue(true);
    }

    @Test
    public void concatEagerIgnorePartialErrorsOnSuccess() {
        final TestSubscriber<Integer> subscriber = new TestSubscriber<>();
        Observable
                .just(asList(Observable.just(1), Observable.just(2)))
                .compose(RxUtils.concatEagerIgnorePartialErrors())
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
                .compose(RxUtils.concatEagerIgnorePartialErrors())
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
                .compose(RxUtils.concatEagerIgnorePartialErrors())
                .subscribe(subscriber);
        subscriber.assertNoValues();
        subscriber.assertNotCompleted();
        subscriber.assertError(RuntimeException.class);
    }

    @Test
    public void optionalOfWrapsObjectInAnOptional() {
        String object = "foo";
        assertThat(((Func1<Object, Optional<Object>>) Optional::of).call(object)).isEqualTo(Optional.of(object));
    }
}
