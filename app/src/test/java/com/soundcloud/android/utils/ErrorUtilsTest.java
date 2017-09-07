package com.soundcloud.android.utils;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.soundcloud.android.api.ApiMapperException;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.ApiRequestException;
import com.soundcloud.android.image.BitmapLoadingAdapter;
import com.soundcloud.android.onboarding.exceptions.TokenRetrievalException;
import com.soundcloud.android.sync.ApiSyncService;
import com.soundcloud.android.sync.SyncFailedException;
import com.soundcloud.android.view.EmptyView;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import rx.exceptions.OnErrorFailedException;
import rx.exceptions.OnErrorThrowable;

import android.content.SyncResult;
import android.os.Bundle;

import java.io.IOException;

@RunWith(MockitoJUnitRunner.class)
public class ErrorUtilsTest {

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
    public void handleThrowableShouldNotRethrowNonFatalUncheckedExceptions() {
        try {
            ErrorUtils.handleThrowable(new NonFatalRuntimeException("Test"), ErrorUtilsTest.class);
        } catch (Throwable t) {
            fail("Non Fatal exception was raised, but shouldn't be");
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
        assertThat(ErrorUtils.findRootCause(new OnErrorFailedException(rootCause))).isSameAs(rootCause);
        assertThat(ErrorUtils.findRootCause(OnErrorThrowable.from(rootCause))).isSameAs(rootCause);
    }

    @Test
    public void notAllowedNetworkErrorTranslatesInHelperMethod() {
        final ApiRequestException apiRequestException = ApiRequestException.notAllowed(null, null);

        assertThat(ErrorUtils.isForbiddenError(apiRequestException)).isTrue();
    }

    @Test
    public void shouldExcludeApiNetworkErrors() {
        final ApiRequestException apiRequestException = ApiRequestException.networkError(null, new IOException());

        assertThat(ErrorUtils.includeInReports(apiRequestException)).isFalse();
    }

    @Test
    public void shouldExcludeBitmapFailedExceptionForIOException() throws Exception {
        Exception bitmapLoadingException = new BitmapLoadingAdapter.BitmapLoadingException(new IOException());

        assertThat(ErrorUtils.includeInReports(bitmapLoadingException)).isFalse();
    }

    @Test
    public void shouldIncludeBitmapFailedExceptionForIllegalStateException() throws Exception {
        Exception bitmapLoadingException = new BitmapLoadingAdapter.BitmapLoadingException(new IllegalStateException());

        assertThat(ErrorUtils.includeInReports(bitmapLoadingException)).isTrue();
    }

    @Test
    public void shouldIncludeBitmapFailedExceptionWithourCause() throws Exception {
        Exception bitmapLoadingException = new BitmapLoadingAdapter.BitmapLoadingException(null);

        assertThat(ErrorUtils.includeInReports(bitmapLoadingException)).isTrue();
    }

    @Test
    public void shouldIncludeMappingErrors() {
        final ApiRequestException apiRequestException = ApiRequestException.malformedInput(null,
                                                                                           new ApiMapperException("foo"));

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
        assertThat(ErrorUtils.includeInReports(new JsonParseException((JsonParser) null, null))).isTrue();
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

    @Test
    public void removesOnNextValueSimpleCause() {
        final Throwable onNextValueCause = new OnErrorThrowable.OnNextValue("cause");
        final Throwable throwable = new Throwable("exception", onNextValueCause);

        final Throwable purgedThrowable = ErrorUtils.purgeOnNextValueCause(throwable);

        assertThat(purgedThrowable.getCause()).isNull();
    }

    @Test
    public void removesOnNextValueInChain() {
        final Throwable onNextValueCause = new OnErrorThrowable.OnNextValue("cause");
        final Throwable cause = new Throwable("exception2", onNextValueCause);
        final Throwable throwable = new Throwable("exception1", cause);

        final Throwable purgedThrowable = ErrorUtils.purgeOnNextValueCause(throwable);

        assertThat(purgedThrowable.getCause().getCause()).isNull();
    }

    @Test
    public void doesNotRemoveGenericException() {
        final Throwable cause1 = new Throwable("cause1");
        final Throwable throwable = new Throwable("exception1", cause1);

        final Throwable purgedThrowable = ErrorUtils.purgeOnNextValueCause(throwable);

        assertThat(purgedThrowable.getCause()).isNotNull();
    }

    @Test
    public void breaksOnCycleInExceptions() {
        final Throwable throwable = new Throwable("exception1");
        final Throwable cause1 = new Throwable("cause1");
        throwable.initCause(cause1);
        final Throwable cause2 = new Throwable("cause2");
        cause1.initCause(cause2);
        cause2.initCause(throwable);

        final Throwable purgedThrowable = ErrorUtils.purgeOnNextValueCause(throwable);

        assertThat(purgedThrowable).isEqualTo(throwable);
    }

    @Test
    public void breaksOnMoreThan25Exceptions() {
        Throwable originalThrowable = new Throwable("exception");
        Throwable throwable = originalThrowable;
        for (int i = 0; i < 30; i++) {
            final Throwable cause = new Throwable("cause"+i);
            throwable.initCause(cause);
            throwable = cause;
        }
        final Throwable onNextValueCause = new OnErrorThrowable.OnNextValue("cause");
        throwable.initCause(onNextValueCause);

        final Throwable purgedThrowable = ErrorUtils.purgeOnNextValueCause(originalThrowable);

        assertThat(purgedThrowable).isEqualTo(originalThrowable);
    }
}
