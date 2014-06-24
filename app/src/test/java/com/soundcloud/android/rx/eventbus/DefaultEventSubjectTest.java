package com.soundcloud.android.rx.eventbus;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import rx.Observer;
import rx.exceptions.OnErrorNotImplementedException;

public class DefaultEventSubjectTest {

    private DefaultEventSubject<Integer> subject = DefaultEventSubject.create();

    @Mock private Observer<Integer> observer1;
    @Mock private Observer<Integer> observer2;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void shouldBehaveLikePublishSubjectForOnNext() {
        subject.subscribe(observer1);
        subject.onNext(1);
        subject.subscribe(observer2);
        subject.onNext(2);

        verify(observer1).onNext(1);
        verify(observer1).onNext(2);
        verifyNoMoreInteractions(observer1);

        verify(observer2).onNext(2);
        verifyNoMoreInteractions(observer2);
    }

    @Test
    public void shouldNeverForwardOnCompletedToKeepTheQueueOpen() {
        subject.subscribe(observer1);
        subject.onCompleted();
        verifyZeroInteractions(observer1);
    }

    @Test
    public void shouldNeverForwardOnErrorForNonFatalErrorsToKeepTheQueueOpen() {
        subject.subscribe(observer1);
        subject.onError(new Exception());
        verifyZeroInteractions(observer1);
    }

    @Test(expected = OnErrorNotImplementedException.class)
    public void shouldRethrowRuntimeExceptions() {
        subject.onError(new RuntimeException());
    }

    @Test(expected = OnErrorNotImplementedException.class)
    public void shouldRethrowFatalErrors() {
        subject.onError(new StackOverflowError());
    }
}