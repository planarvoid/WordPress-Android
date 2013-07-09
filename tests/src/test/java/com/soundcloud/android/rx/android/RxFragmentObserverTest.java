package com.soundcloud.android.rx.android;

import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.refEq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import android.support.v4.app.Fragment;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@RunWith(SoundCloudTestRunner.class)
public class RxFragmentObserverTest {

    private TestCompletedHandler handler;
    private @Mock Fragment mockFragment;

    private class TestCompletedHandler extends RxFragmentObserver<Fragment, String> {

        public TestCompletedHandler(Fragment fragment) {
            super(fragment);
        }
    }

    @Before
    public void setup() {
        handler = spy(new TestCompletedHandler(mockFragment));
    }

    @Test
    public void shouldCallBackIfFragmentExistsAndAttachedToActivity() {
        when(mockFragment.isAdded()).thenReturn(true);

        invokeRxCallbacks();

        verifyCallbacksInvoked();
    }

    @Test
    public void shouldCallBackIfFragmentNotAttachedAndActivityNotRequired() {
        when(mockFragment.isAdded()).thenReturn(false);

        handler.setRequireActivity(false);
        invokeRxCallbacks();

        verifyCallbacksInvoked();
    }

    @Test
    public void shouldNotCallBackIfActivityRequiredAndFragmentNotAttached() {
        when(mockFragment.isAdded()).thenReturn(false);

        handler.setRequireActivity(true);
        invokeRxCallbacks();

        verifyCallbacksNotInvoked();
    }

    @Test
    public void shouldNotCallBackIfFragmentIsNull() {
        handler = spy(new TestCompletedHandler(null));

        invokeRxCallbacks();

        verifyCallbacksNotInvoked();
    }

    @Test(expected = IllegalThreadException.class)
    public void shouldThrowWhenCallbacksNotInvokedOnUiThread() throws Throwable {
        handler = new TestCompletedHandler(mockFragment);

        Future<Void> future = Executors.newSingleThreadExecutor().submit(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                handler.onCompleted();
                return null;
            }
        });

        try {
            future.get();
        } catch (ExecutionException e) {
            throw e.getCause();
        }
    }

    @Test
    public void shouldNotThrowAgainWhenOnErrorWasCalledWithIllegalThreadException() {
        handler = new TestCompletedHandler(mockFragment);

        Future<Void> future = Executors.newSingleThreadExecutor().submit(new Callable<Void>() {
            @Override
            public Void call() throws Exception {
                handler.onError(new IllegalThreadException(""));
                return null;
            }
        });

        try {
            future.get();
        } catch (Throwable t) {
            fail("Expected call not to throw, but got " + t.getCause());
        }
    }

    private void invokeRxCallbacks() {
        handler.onNext("item");
        handler.onCompleted();
        handler.onError(new Exception());
    }

    private void verifyCallbacksInvoked() {
        verify(handler).onNext(refEq(mockFragment), eq("item"));
        verify(handler).onCompleted(mockFragment);
        verify(handler).onError(refEq(mockFragment), any(Exception.class));
    }

    private void verifyCallbacksNotInvoked() {
        verify(handler, never()).onNext(any(Fragment.class), anyString());
        verify(handler, never()).onCompleted(any(Fragment.class));
        verify(handler, never()).onError(any(Fragment.class), any(Exception.class));
    }
}
