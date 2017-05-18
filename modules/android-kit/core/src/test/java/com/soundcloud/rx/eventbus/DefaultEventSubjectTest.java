package com.soundcloud.rx.eventbus;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import rx.Observer;
import rx.functions.Action1;

@RunWith(MockitoJUnitRunner.class)
public class DefaultEventSubjectTest {

    private EventSubject<Integer> subject = EventSubject.create();

    @Mock private Observer<Integer> observer1;
    @Mock private Observer<Integer> observer2;
    @Mock private Action1<Throwable> errorHandler;

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

    @Test
    public void shouldForwardErrorToCustomErrorHandler() {
        subject = EventSubject.create(errorHandler);

        final Exception exception = new Exception();
        subject.onError(exception);

        verify(errorHandler).call(exception);
    }
}