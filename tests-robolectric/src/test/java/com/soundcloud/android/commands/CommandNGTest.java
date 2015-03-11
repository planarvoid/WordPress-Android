package com.soundcloud.android.commands;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.Observable;
import rx.Observer;

import java.util.concurrent.atomic.AtomicReference;

@RunWith(SoundCloudTestRunner.class)
public class CommandNGTest {

    @Mock private Observer<String> observer;

    private AtomicReference<String> inputCapture = new AtomicReference<>();

    private CommandNG<String, String> command = new CommandNG<String, String>() {


        @Override
        public String call(String input) {
            inputCapture.set(input);
            return "output";
        }
    };

    private CommandNG<String, String> throwingCommand = new CommandNG<String, String>() {
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

        expect(inputCapture.get()).toEqual("action input");
    }

    @Test
    public void toContinuation() throws Exception {
        Observable.just("item").flatMap(command.toContinuation()).subscribe(observer);

        expect(inputCapture.get()).toEqual("item");
    }
}