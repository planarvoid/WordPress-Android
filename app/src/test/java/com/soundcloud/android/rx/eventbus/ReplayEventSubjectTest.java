package com.soundcloud.android.rx.eventbus;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import rx.Observer;

public class ReplayEventSubjectTest {

    private ReplayEventSubject<Integer> subject = ReplayEventSubject.create();

    @Mock private Observer<Integer> observer1;
    @Mock private Observer<Integer> observer2;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void shouldReplayLastEventToSubscribers() {
        subject.onNext(1);
        subject.onNext(2);
        subject.subscribe(observer1);
        subject.subscribe(observer2);

        verify(observer1).onNext(2);
        verifyNoMoreInteractions(observer1);

        verify(observer2).onNext(2);
        verifyNoMoreInteractions(observer2);
    }

    @Test
    public void shouldNeverForwardOnCompleted() {
        subject.subscribe(observer1);
        subject.onCompleted();
        verifyZeroInteractions(observer1);
    }

    @Test
    public void shouldReplayDefaultEventIfNoOtherEventsHaveFired() {
        ReplayEventSubject<Integer> subject = ReplayEventSubject.create(1);
        subject.subscribe(observer1);
        verify(observer1).onNext(1);
        verifyNoMoreInteractions(observer1);
    }

    @Test
    public void shouldNotReplayDefaultEventIfOtherEventsHaveFired() {
        ReplayEventSubject<Integer> subject = ReplayEventSubject.create(1);
        subject.onNext(2);
        subject.subscribe(observer1);
        verify(observer1, never()).onNext(1);
        verify(observer1).onNext(2);
    }
}