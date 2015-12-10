package com.soundcloud.android.commands;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import rx.Observable;
import rx.observers.TestObserver;

public class LegacyCommandTest {

    private static final class ToString extends LegacyCommand<Integer, String, ToString> {

        boolean called = false;

        @Override
        public String call() {
            called = true;
            return input.toString();
        }
    }

    private static final class ToInteger extends LegacyCommand<String, Integer, ToInteger> {

        boolean called = false;

        @Override
        public Integer call() {
            called = true;
            return Integer.parseInt(input);
        }
    }

    private static final class Failed extends LegacyCommand<Integer, Void, Failed> {

        @Override
        public Void call() throws Exception {
            throw new RuntimeException();
        }
    }

    private ToString toString = new ToString();
    private ToInteger toInteger = new ToInteger();
    private Failed failed = new Failed();
    private TestObserver<Integer> testObserver = new TestObserver<>();

    @Test
    public void shouldExecuteWithBoundParameters() throws Exception {
        assertThat(toString.with(1).call()).isEqualTo("1");
    }

    @Test
    public void shouldChainCommands() throws Exception {
        assertThat(toString.with(1).andThen(toInteger).call()).isEqualTo(1);
    }

    @Test
    public void shouldChainCommandsViaObservables() {
        toString.with(1).flatMap(toInteger).subscribe(testObserver);
        testObserver.assertReceivedOnNext(singletonList(1));
    }

    @Test
    public void shouldBeConvertibleToAction() {
        Observable.just(1).doOnNext(toString.toAction()).subscribe(testObserver);
        assertThat(toString.called).isTrue();
    }

    @Test
    public void shouldReportErrorsThatOccurWhenInvokedAsAction() {
        Observable.just(1).doOnNext(failed.toAction()).subscribe(testObserver);
        assertThat(testObserver.getOnErrorEvents()).hasSize(1);
    }
}
