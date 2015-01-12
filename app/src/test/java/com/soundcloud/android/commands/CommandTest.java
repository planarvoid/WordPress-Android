package com.soundcloud.android.commands;

import static com.soundcloud.android.Expect.expect;

import org.junit.Test;
import rx.Observable;
import rx.observers.TestObserver;

import java.util.Arrays;

public class CommandTest {

    private static final class ToString extends Command<Integer, String> {

        boolean called = false;

        @Override
        public String call() {
            called = true;
            return input.toString();
        }
    }

    private static final class ToInteger extends Command<String, Integer> {

        boolean called = false;

        @Override
        public Integer call() {
            called = true;
            return Integer.parseInt(input);
        }
    }

    private static final class Failed extends Command<Integer, Void> {

        @Override
        public Void call() throws Exception {
            throw new RuntimeException();
        }
    }

    private ToString toString = new ToString();
    private ToInteger toInteger = new ToInteger();
    private Failed failed = new Failed();
    private TestObserver testObserver = new TestObserver();

    @Test
    public void shouldExecuteWithBoundParameters() throws Exception {
        expect(toString.with(1).call()).toEqual("1");
    }

    @Test
    public void shouldChainCommands() throws Exception {
        expect(toString.with(1).andThen(toInteger).call()).toBe(1);
    }

    @Test
    public void shouldChainCommandsViaObservables() {
        toString.with(1).flatMap(toInteger).subscribe(testObserver);
        testObserver.assertReceivedOnNext(Arrays.asList(1));
    }

    @Test
    public void shouldBeConvertibleToAction() {
        Observable.just(1).doOnNext(toString.toAction()).subscribe(testObserver);
        expect(toString.called).toBeTrue();
    }

    @Test
    public void shouldReportErrorsThatOccurWhenInvokedAsAction() {
        Observable.just(1).doOnNext(failed.toAction()).subscribe(testObserver);
        expect(testObserver.getOnErrorEvents()).toNumber(1);
    }
}