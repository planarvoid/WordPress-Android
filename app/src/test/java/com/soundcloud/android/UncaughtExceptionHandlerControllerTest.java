package com.soundcloud.android;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static rx.exceptions.OnErrorThrowable.addValueAsLastCause;
import static rx.exceptions.OnErrorThrowable.from;

import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.utils.MemoryReporter;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.exceptions.OnErrorFailedException;
import rx.exceptions.OnErrorThrowable;

import android.database.sqlite.SQLiteException;

import java.util.HashMap;

public class UncaughtExceptionHandlerControllerTest extends AndroidUnitTest {

    @Mock private Thread.UncaughtExceptionHandler anOtherHandler;
    @Mock private MemoryReporter memoryReporter;
    private UncaughtExceptionHandlerController controller;

    @Before
    public void setUp() throws Exception {
        controller = new UncaughtExceptionHandlerController(memoryReporter);
    }

    @Test
    public void resetHandlerAndReportErrorWhenHandlerUnset() {
        controller.setHandler();
        Thread.setDefaultUncaughtExceptionHandler(anOtherHandler);
        controller.assertHandlerIsSet();

        Thread.getDefaultUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), new OutOfMemoryError());
        verify(memoryReporter).reportOomStats();
    }

    @Test
    public void shouldExtractCauseFromExceptionsRethrownByDefaultSubscriberOnWorkerThread() {
        Thread.UncaughtExceptionHandler proxiedHandler = mock(Thread.UncaughtExceptionHandler.class);
        Thread.setDefaultUncaughtExceptionHandler(proxiedHandler);

        controller.setHandler();

        Thread.UncaughtExceptionHandler decoratedHandler = Thread.getDefaultUncaughtExceptionHandler();
        assertThat(proxiedHandler).isNotSameAs(decoratedHandler);

        // this is the causal chain we see when a worker thread crashes with an exception
        final Exception rootCause = new Exception("root cause");
        final IllegalStateException causalChain = new IllegalStateException(new OnErrorFailedException(rootCause));
        decoratedHandler.uncaughtException(Thread.currentThread(), causalChain);

        verify(proxiedHandler).uncaughtException(Thread.currentThread(), rootCause);
    }

    @Test
    public void shouldExtractCauseFromExceptionsRethrownFromRxOperators() {

        Thread.UncaughtExceptionHandler proxiedHandler = mock(Thread.UncaughtExceptionHandler.class);
        Thread.setDefaultUncaughtExceptionHandler(proxiedHandler);

        controller.setHandler();

        Thread.UncaughtExceptionHandler decoratedHandler = Thread.getDefaultUncaughtExceptionHandler();
        assertThat(proxiedHandler).isNotSameAs(decoratedHandler);

        // this is the causal chain we see when a worker thread crashes with an exception
        final SQLiteException cause = new SQLiteException();
        final OnErrorThrowable chain = from(addValueAsLastCause(cause, new HashMap<>()));

        final Exception causalChain = new IllegalStateException(new OnErrorFailedException(chain));
        decoratedHandler.uncaughtException(Thread.currentThread(), causalChain);

        verify(proxiedHandler).uncaughtException(Thread.currentThread(), cause);
    }
}
