package com.soundcloud.android.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.core.JsonParseException;
import com.soundcloud.android.api.ApiMapperException;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.ApiRequestException;
import com.soundcloud.android.onboarding.exceptions.TokenRetrievalException;
import com.soundcloud.android.sync.ApiSyncService;
import com.soundcloud.android.sync.SyncFailedException;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.view.EmptyView;
import org.junit.Test;
import rx.exceptions.OnErrorFailedException;

import android.content.SyncResult;
import android.os.Bundle;

import java.io.IOException;

public class ErrorUtilsTest extends AndroidUnitTest {

    @Test
    public void handleThrowableShouldNotRethrowCheckedExceptions() {
        try {
            ErrorUtils.handleThrowable(new Exception(), ErrorUtilsTest.class);
        } catch (Throwable t) {
            fail("Checked exception was raised, but shouldn't be");
        }
        // Pass
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

        assertThat(ErrorUtils.isCausedByOutOfMemory(e)).isTrue();
    }

    @Test
    public void shouldBeCausedByOOMWhenOOMIsRootCause() {
        Exception e = new Exception();
        Exception e1 = new Exception();
        e.initCause(e1);
        e1.initCause(new OutOfMemoryError());

        assertThat(ErrorUtils.isCausedByOutOfMemory(e)).isTrue();
    }

    @Test
    public void shouldBeCausedByOOMWhenThrowableHasOOMInChain() {
        Exception e = new Exception();
        OutOfMemoryError oom = new OutOfMemoryError();
        oom.initCause(new RuntimeException("wrapping an OOM"));
        e.initCause(oom);

        assertThat(ErrorUtils.isCausedByOutOfMemory(e)).isTrue();
    }

    @Test
    public void shouldNotBeCausedByOOMIfThrowableIsNotAnOOM() {
        Exception e = new Exception();

        assertThat(ErrorUtils.isCausedByOutOfMemory(e)).isFalse();
    }

    @Test
    public void shouldNotBeCausedByOOMIfThrowableHasNoOOMInChain() {
        Exception e = new Exception();
        e.initCause(new Exception());

        assertThat(ErrorUtils.isCausedByOutOfMemory(e)).isFalse();
    }

    @Test
    public void shouldExtractRootCauseFromCausalChain() {
        Exception rootCause = new Exception();
        assertThat(ErrorUtils.findRootCause(null)).isNull();
        assertThat(ErrorUtils.findRootCause(rootCause)).isSameAs(rootCause);
        assertThat(ErrorUtils.findRootCause(new Exception(new Exception(rootCause)))).isSameAs(rootCause);
    }

    @Test
    public void shouldExtractCauseFromExceptionsRethrownByDefaultSubscriberOnWorkerThread() {
        Thread.UncaughtExceptionHandler proxiedHandler = mock(Thread.UncaughtExceptionHandler.class);
        Thread.setDefaultUncaughtExceptionHandler(proxiedHandler);

        ErrorUtils.setupUncaughtExceptionHandler(mock(MemoryReporter.class));

        Thread.UncaughtExceptionHandler decoratedHandler = Thread.getDefaultUncaughtExceptionHandler();
        assertThat(proxiedHandler).isNotSameAs(decoratedHandler);

        // this is the causal chain we see when a worker thread crashes with an exception
        final Exception rootCause = new Exception("root cause");
        final IllegalStateException causalChain = new IllegalStateException(new OnErrorFailedException(rootCause));
        decoratedHandler.uncaughtException(Thread.currentThread(), causalChain);

        verify(proxiedHandler).uncaughtException(Thread.currentThread(), rootCause);
    }

    @Test
    public void shouldExcludeApiNetworkErrors() {
        final ApiRequestException apiRequestException = ApiRequestException.networkError(null, new IOException());

        assertThat(ErrorUtils.includeInReports(apiRequestException)).isFalse();
    }

    @Test
    public void shouldIncludeMappingErrors() {
        final ApiRequestException apiRequestException = ApiRequestException.malformedInput(null, new ApiMapperException("foo"));

        assertThat(ErrorUtils.includeInReports(apiRequestException)).isTrue();
    }

    @Test
    public void shouldExcludeSyncFailures() {
        final Bundle syncResultBundle = new Bundle();
        syncResultBundle.putParcelable(ApiSyncService.EXTRA_SYNC_RESULT, new SyncResult());

        assertThat(ErrorUtils.includeInReports(new SyncFailedException(syncResultBundle))).isFalse();
    }

    @Test
    public void shouldExcludeIOExceptions() {
        assertThat(ErrorUtils.includeInReports(new IOException())).isFalse();
    }

    @Test
    public void shouldIncludeJsonProcessingExceptions() {
        assertThat(ErrorUtils.includeInReports(new JsonParseException(null, null))).isTrue();
    }

    @Test
    public void shouldIncludeOtherExceptions() {
        assertThat(ErrorUtils.includeInReports(new IllegalStateException("foo"))).isTrue();
    }

    @Test
    public void emptyViewStatusFromApiNetworkError() {
        ApiRequestException exception = ApiRequestException.networkError(mock(ApiRequest.class), new IOException());
        assertThat(ErrorUtils.emptyViewStatusFromError(exception)).isEqualTo(EmptyView.Status.CONNECTION_ERROR);
    }

    @Test
    public void emptyViewStatusFromApiServerError() {
        ApiRequestException exception = ApiRequestException.serverError(mock(ApiRequest.class), null);
        assertThat(ErrorUtils.emptyViewStatusFromError(exception)).isEqualTo(EmptyView.Status.SERVER_ERROR);
    }

    @Test
    public void emptyViewStatusFromSyncError() {
        SyncFailedException exception = new SyncFailedException(new Bundle());
        assertThat(ErrorUtils.emptyViewStatusFromError(exception)).isEqualTo(EmptyView.Status.CONNECTION_ERROR);
    }

    @Test
    public void emptyViewStatusFromGenericError() {
        Exception exception = new Exception();
        assertThat(ErrorUtils.emptyViewStatusFromError(exception)).isEqualTo(EmptyView.Status.ERROR);
    }

    @Test
    public void removeTokenRetrievalExceptionIfAny() {
        final Exception exception = new Exception();
        assertThat(ErrorUtils.removeTokenRetrievalException(new TokenRetrievalException(exception))).isSameAs(exception);
    }

    @Test
    public void removeTokenRetrievalExceptionIsNoOpWhenWrappedWithATokenRetrievalException() {
        final Exception exception = new Exception();
        assertThat(ErrorUtils.removeTokenRetrievalException(exception)).isSameAs(exception);
    }

}
