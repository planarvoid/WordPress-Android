package com.soundcloud.android.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

@RunWith(MockitoJUnitRunner.class)
public class TryWithBackOffTest {

    private static final int MAX_ATTEMPTS = 2;
    private static final int BACK_OFF_MULTIPLIER = 2;
    private static final int INITIAL_BACK_OFF_TIME = 1;
    private static final TimeUnit TIME_UNIT = TimeUnit.MILLISECONDS;

    private TryWithBackOff<Object> tryWithBackOff;

    @Mock private Sleeper sleeper;
    @Mock private Callable<Object> callable;

    @Before
    public void setUp() throws Exception {
        tryWithBackOff = new TryWithBackOff<>(sleeper, INITIAL_BACK_OFF_TIME, TIME_UNIT,
                BACK_OFF_MULTIPLIER, MAX_ATTEMPTS);
    }

    @Test
    public void shouldNotRetrySuccessfulTasks() throws Exception {
        when(callable.call()).thenReturn("success");

        Object result = tryWithBackOff.call(callable);

        assertThat(result).isEqualTo("success");
        verifyZeroInteractions(sleeper);
    }

    @Test(expected = Exception.class)
    public void shouldRetryFailedTasksWithExponentialBackOff() throws Exception {
        final Exception thrownException = new Exception("Kaputt");
        when(callable.call()).thenThrow(thrownException);

        try {
            tryWithBackOff.call(callable);
        } finally {
            InOrder inOrder = inOrder(sleeper);
            inOrder.verify(sleeper).sleep(INITIAL_BACK_OFF_TIME, TIME_UNIT);
            inOrder.verify(sleeper).sleep(INITIAL_BACK_OFF_TIME * BACK_OFF_MULTIPLIER, TIME_UNIT);
            inOrder.verifyNoMoreInteractions();
        }
    }

    @Test
    public void shouldRethrowErrorsFromFailedTasks() throws Exception {
        final Exception thrownException = new Exception("Kaputt");
        when(callable.call()).thenThrow(thrownException);

        Exception errorCaught = null;
        try {
            tryWithBackOff.call(callable);
        } catch (Exception e) {
            errorCaught = e;
        }

        assertThat(errorCaught).isSameAs(thrownException);
    }
}
