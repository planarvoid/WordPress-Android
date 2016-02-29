package com.soundcloud.android.commands;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import rx.Observable;
import rx.Observer;

import java.util.concurrent.atomic.AtomicReference;

@RunWith(MockitoJUnitRunner.class)
public class CommandTest {

    @Mock private Observer<String> observer;

    private AtomicReference<String> inputCapture = new AtomicReference<>();

    private Command<String, String> command = new Command<String, String>() {


        @Override
        public String call(String input) {
            inputCapture.set(input);
            return "output";
        }
    };

    private Command<String, String> throwingCommand = new Command<String, String>() {
        @Override
        public String call(String input) {
            throw new RuntimeException();
        }
    };

    @Test
    public void toObservableEmitsOutputToObserver() throws Exception {
        command.toObservable("input").subscribe(observer);

        verify(observer).onNext("output");
        verify(observer).onCompleted();
        verifyNoMoreInteractions(observer);
    }

    @Test
    public void toObservableForwardsErrorsToObserver() throws Exception {
        throwingCommand.toObservable("input").subscribe(observer);

        verify(observer).onError(isA(RuntimeException.class));
        verifyNoMoreInteractions(observer);
    }

    @Test
    public void toAction() throws Exception {
        command.toAction().call("action input");

        assertThat(inputCapture.get()).isEqualTo("action input");
    }

    @Test
    public void toAction0() throws Exception {
        command.toAction0().call();

        assertThat(inputCapture.get()).isNull();
    }

    @Test
    public void toContinuation() throws Exception {
        Observable.just("item").flatMap(command.toContinuation()).subscribe(observer);

        assertThat(inputCapture.get()).isEqualTo("item");
    }
}
