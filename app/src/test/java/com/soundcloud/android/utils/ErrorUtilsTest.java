package com.soundcloud.android.utils;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.Test;
import rx.exceptions.OnErrorNotImplementedException;

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

    @Test(expected = OnErrorNotImplementedException.class)
    public void handleThrowableShouldRethrowJavaErrorsAsOnErrorNotImplemented() {
        ErrorUtils.handleThrowable(new StackOverflowError(), ErrorUtilsTest.class);
    }

    @Test(expected = OnErrorNotImplementedException.class)
    public void handleThrowableShouldRethrowJavaUncheckedExceptionsAsOnErrorNotImplemented() {
        ErrorUtils.handleThrowable(new RuntimeException(), ErrorUtilsTest.class);
    }

    @Test(expected = RuntimeException.class)
    public void handleThrowableShouldRethrowCauseFromOnErrorNotImplemented() {
        ErrorUtils.handleThrowable(new OnErrorNotImplementedException(new RuntimeException()), ErrorUtilsTest.class);
    }
}
