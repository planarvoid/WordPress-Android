package com.soundcloud.android.utils;

import static com.soundcloud.android.Expect.expect;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.xtremelabs.robolectric.RobolectricTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import rx.exceptions.OnErrorFailedException;

@RunWith(RobolectricTestRunner.class)
public class ErrorUtilsTest {

    @Test
    public void handleThrowableShouldNotRethrowCheckedExceptions() {
        try {
            ErrorUtils.handleThrowable(new Exception(), ErrorUtilsTest.class);
        } catch (Throwable t) {
            fail("Checked exception was raised, but shouldn't be");
        }
        // pass
    }

    @Test
    public void handleThrowableShouldLogTheStackTrace() {
        Throwable mockError = mock(Throwable.class);
        ErrorUtils.handleThrowable(mockError, ErrorUtilsTest.class);
        verify(mockError).printStackTrace();
    }

    @Test(expected = RuntimeException.class)
    public void handleThrowableShouldRethrowJavaUncheckedExceptionsAsOnErrorNotImplemented() {
        ErrorUtils.handleThrowable(new RuntimeException(), ErrorUtilsTest.class);
    }

    @Test
    public void shouldBeCausedByOOMWhenThrowableIsAnOOM() {
        Throwable e = new OutOfMemoryError();

        expect(ErrorUtils.isCausedByOutOfMemory(e)).toBeTrue();
    }

    @Test
    public void shouldBeCausedByOOMWhenThrowableHasOOMInChain() {
        Exception e = new Exception();
        Exception e1 = new Exception();
        e.initCause(e1);
        e1.initCause(new OutOfMemoryError());

        expect(ErrorUtils.isCausedByOutOfMemory(e)).toBeTrue();
    }

    @Test
    public void shouldNotBeCausedByOOMIfThrowableIsNotAnOOM() {
        Exception e = new Exception();

        expect(ErrorUtils.isCausedByOutOfMemory(e)).toBeFalse();
    }

    @Test
    public void shouldNotBeCausedByOOMIfThrowableHasNoOOMInChain() {
        Exception e = new Exception();
        e.initCause(new Exception());

        expect(ErrorUtils.isCausedByOutOfMemory(e)).toBeFalse();
    }

    @Test
    public void shouldExtractRootCauseFromCausalChain() {
        Exception rootCause = new Exception();
        expect(ErrorUtils.findRootCause(null)).toBeNull();
        expect(ErrorUtils.findRootCause(rootCause)).toBe(rootCause);
        expect(ErrorUtils.findRootCause(new Exception(new Exception(rootCause)))).toBe(rootCause);
    }

    @Test
    public void shouldExtractCauseFromExceptionsRethrownByDefaultSubscriberOnWorkerThread() {
        Thread.UncaughtExceptionHandler proxiedHandler = mock(Thread.UncaughtExceptionHandler.class);
        Thread.setDefaultUncaughtExceptionHandler(proxiedHandler);

        ErrorUtils.setupUncaughtExceptionHandler(mock(MemoryReporter.class));

        Thread.UncaughtExceptionHandler decoratedHandler = Thread.getDefaultUncaughtExceptionHandler();
        expect(proxiedHandler).not.toBe(decoratedHandler);

        // this is the causal chain we see when a worker thread crashes with an exception
        final Exception rootCause = new Exception("root cause");
        final IllegalStateException causalChain = new IllegalStateException(new OnErrorFailedException(rootCause));
        decoratedHandler.uncaughtException(Thread.currentThread(), causalChain);

        verify(proxiedHandler).uncaughtException(Thread.currentThread(), rootCause);
    }
}
