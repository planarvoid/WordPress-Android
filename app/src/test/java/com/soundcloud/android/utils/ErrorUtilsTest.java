package com.soundcloud.android.utils;

import static com.soundcloud.android.Expect.expect;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.soundcloud.android.api.ApiMapperException;
import com.soundcloud.android.api.ApiRequestException;
import com.soundcloud.android.sync.ApiSyncService;
import com.soundcloud.android.sync.SyncFailedException;
import com.xtremelabs.robolectric.RobolectricTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import sun.plugin.dom.exception.InvalidStateException;

import android.content.SyncResult;
import android.os.Bundle;

import java.io.IOException;

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
        expect(ErrorUtils.includeInReports(new InvalidStateException("foo"))).toBeTrue();
    }
}
