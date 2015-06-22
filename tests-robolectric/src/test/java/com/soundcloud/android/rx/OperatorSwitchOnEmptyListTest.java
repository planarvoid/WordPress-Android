package com.soundcloud.android.rx;

import static com.soundcloud.android.Expect.expect;

import com.google.common.collect.Lists;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import rx.observers.TestObserver;
import rx.subjects.PublishSubject;

import java.util.Collections;
import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class OperatorSwitchOnEmptyListTest {
    public static final List<String> EMPTY_LIST = Collections.emptyList();
    public static final List<String> NON_EMPTY_LIST = Lists.newArrayList("first", "second");
    private PublishSubject<List<String>> originalSource;
    private PublishSubject<List<String>> switchSource;
    private TestObserver<List<String>> observer;

    @Before
    public void setUp() throws Exception {
        originalSource = PublishSubject.create();
        switchSource = PublishSubject.create();
        observer = new TestObserver<>();
        originalSource.lift(new OperatorSwitchOnEmptyList<>(switchSource)).subscribe(observer);
    }

    @Test
    public void switchOnEmptyList() {
        originalSource.onNext(EMPTY_LIST);
        switchSource.onNext(NON_EMPTY_LIST);

        expect(observer.getOnNextEvents()).toNumber(1);
        expect(observer.getOnNextEvents().get(0)).toBe(NON_EMPTY_LIST);
    }

    @Test
    public void emitsOnNonEmptyList() {
        originalSource.onNext(NON_EMPTY_LIST);

        expect(observer.getOnNextEvents()).toNumber(1);
        expect(observer.getOnNextEvents().get(0)).toBe(NON_EMPTY_LIST);
    }

    @Test
    public void forwardsCompleteWhenNotSwitched() {
        originalSource.onCompleted();
        expect(observer.getOnCompletedEvents()).toNumber(1);
    }

    @Test
    public void forwardsErrorWhenNotSwitched() {
        originalSource.onError(new IllegalStateException());
        expect(observer.getOnErrorEvents()).toNumber(1);
    }

    @Test
    public void doesNotCompleteWhenSwitched() {
        originalSource.onNext(EMPTY_LIST);
        originalSource.onCompleted();
        switchSource.onNext(NON_EMPTY_LIST);
        switchSource.onCompleted();

        expect(observer.getOnNextEvents().get(0)).toBe(NON_EMPTY_LIST);
        expect(observer.getOnCompletedEvents()).toNumber(1);
    }

    @Test
    public void doesNotEmitErrorWhenSwitched() {
        originalSource.onNext(EMPTY_LIST);
        originalSource.onError(new IllegalStateException());
        switchSource.onNext(NON_EMPTY_LIST);

        expect(observer.getOnNextEvents().get(0)).toBe(NON_EMPTY_LIST);
        expect(observer.getOnErrorEvents()).toBeEmpty();
    }

    @Test
    public void doesNotEmitWhenSwitched() {
        originalSource.onNext(EMPTY_LIST);
        originalSource.onNext(NON_EMPTY_LIST);

        expect(observer.getOnNextEvents()).toBeEmpty();
    }
}