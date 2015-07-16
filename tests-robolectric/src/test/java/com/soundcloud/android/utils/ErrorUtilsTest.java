package com.soundcloud.android.utils;

import static com.soundcloud.android.Expect.expect;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.core.JsonParseException;
import com.soundcloud.android.api.ApiMapperException;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.ApiRequestException;
import com.soundcloud.android.onboarding.exceptions.TokenRetrievalException;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.sync.ApiSyncService;
import com.soundcloud.android.sync.SyncFailedException;
import com.soundcloud.android.view.EmptyView;
import org.junit.Test;
import org.junit.runner.RunWith;
import rx.exceptions.OnErrorFailedException;

import android.content.SyncResult;
import android.os.Bundle;

import java.io.IOException;

@RunWith(SoundCloudTestRunner.class)
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
    public void shouldBeCausedByOOMWhenOOMIsRootCause() {
        Exception e = new Exception();
        Exception e1 = new Exception();
        e.initCause(e1);
        e1.initCause(new OutOfMemoryError());

        expect(ErrorUtils.isCausedByOutOfMemory(e)).toBeTrue();
    }

    @Test
    public void shouldBeCausedByOOMWhenThrowableHasOOMInChain() {
        Exception e = new Exception();
        OutOfMemoryError oom = new OutOfMemoryError();
        oom.initCause(new RuntimeException("wrapping an OOM"));
        e.initCause(oom);

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

    public void shouldExcludeApiNetworkErrors() {
        final ApiRequestException apiRequestException = ApiRequestException.networkError(null, new IOException());

        expect(ErrorUtils.includeInReports(apiRequestException)).toBeFalse();
    }

    @Test
    public void shouldIncludeMappingErrors() {
        final ApiRequestException apiRequestException = ApiRequestException.malformedInput(null, new ApiMapperException("foo"));

        expect(ErrorUtils.includeInReports(apiRequestException)).toBeTrue();
    }

    @Test
    public void shouldExcludeSyncFailures() {
        final Bundle syncResultBundle = new Bundle();
        syncResultBundle.putParcelable(ApiSyncService.EXTRA_SYNC_RESULT, new SyncResult());

        expect(ErrorUtils.includeInReports(new SyncFailedException(syncResultBundle))).toBeFalse();
    }

    @Test
    public void shouldExcludeIOExceptions() {
        expect(ErrorUtils.includeInReports(new IOException())).toBeFalse();
    }

    @Test
    public void shouldIncludeJsonProcessingExceptions() {
        expect(ErrorUtils.includeInReports(new JsonParseException(null, null))).toBeTrue();
    }

    @Test
    public void shouldIncludeOtherExceptions() {
        expect(ErrorUtils.includeInReports(new IllegalStateException("foo"))).toBeTrue();
    }

    @Test
    public void emptyViewStatusFromApiNetworkError() {
        ApiRequestException exception = ApiRequestException.networkError(mock(ApiRequest.class), new IOException());
        expect(ErrorUtils.emptyViewStatusFromError(exception)).toEqual(EmptyView.Status.CONNECTION_ERROR);
    }

    @Test
    public void emptyViewStatusFromApiServerError() {
        ApiRequestException exception = ApiRequestException.serverError(mock(ApiRequest.class), null);
        expect(ErrorUtils.emptyViewStatusFromError(exception)).toEqual(EmptyView.Status.SERVER_ERROR);
    }

    @Test
    public void emptyViewStatusFromSyncError() {
        SyncFailedException exception = new SyncFailedException(new Bundle());
        expect(ErrorUtils.emptyViewStatusFromError(exception)).toEqual(EmptyView.Status.CONNECTION_ERROR);
    }

    @Test
    public void emptyViewStatusFromGenericError() {
        Exception exception = new Exception();
        expect(ErrorUtils.emptyViewStatusFromError(exception)).toEqual(EmptyView.Status.ERROR);
    }

    @Test
    public void removeTokenRetrievalExceptionIfAny() {
        final Exception exception = new Exception();
        expect(ErrorUtils.removeTokenRetrievalException(new TokenRetrievalException(exception))).toBe(exception);
    }

    @Test
    public void removeTokenRetrievalExceptionIsNoOpWhenWrappedWithATokenRetrievalException() {
        final Exception exception = new Exception();
        expect(ErrorUtils.removeTokenRetrievalException(exception)).toBe(exception);
    }

}
