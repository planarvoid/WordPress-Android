package com.soundcloud.android.utils;

import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import org.junit.Test;

public class ErrorUtilsTest {

    @Test
    public void handleThrowableShouldNotRethrowCheckedExceptions() {
        try {
            ErrorUtils.handleThrowable(new Exception());
        } catch (Throwable t) {
            fail("Checked exception was raised, but shouldn't be");
        }
        // pass
    }

    @Test
    public void handleThrowableShouldLogTheStackTrace() {
        Throwable mockError = mock(Throwable.class);
        ErrorUtils.handleThrowable(mockError);
        verify(mockError).printStackTrace();
    }

    @Test(expected = StackOverflowError.class)
    public void handleThrowableShouldRethrowJavaErrors() {
        ErrorUtils.handleThrowable(new StackOverflowError());
    }

    @Test(expected = RuntimeException.class)
    public void handleThrowableShouldRethrowJavaUncheckedExceptions() {
        ErrorUtils.handleThrowable(new RuntimeException());
    }

}
