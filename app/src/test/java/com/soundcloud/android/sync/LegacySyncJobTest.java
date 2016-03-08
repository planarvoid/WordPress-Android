package com.soundcloud.android.sync;


import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.ApiMapperException;
import com.soundcloud.android.api.ApiRequestException;
import com.soundcloud.android.api.ApiResponse;
import com.soundcloud.android.api.legacy.Request;
import com.soundcloud.android.api.legacy.UnexpectedResponseException;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import rx.Observable;

import android.content.SharedPreferences;
import android.net.Uri;

import java.io.IOException;

public class LegacySyncJobTest extends AndroidUnitTest {

    public static final String SOME_ACTION = "someAction";
    private static final Uri CONTENT_URI = Content.ME_FOLLOWINGS.uri;

    private LegacySyncJob legacySyncItem;

    @Mock private ApiSyncerFactory apiSyncerFactory;
    @Mock private SyncStateManager syncStateManager;
    @Mock private SyncStrategy SyncStrategy;
    @Mock private SharedPreferences sharedPreferences;

    private ApiSyncResult apiSyncResult;

    @Before
    public void setup() {
        legacySyncItem = new LegacySyncJob(
                CONTENT_URI, SOME_ACTION, false, apiSyncerFactory, syncStateManager);
    }

    @Test
    public void shouldHaveEquals() throws Exception {
        LegacySyncJob r1 = new LegacySyncJob(
                CONTENT_URI, SOME_ACTION, false, apiSyncerFactory, syncStateManager);

        LegacySyncJob r2 = new LegacySyncJob(
                CONTENT_URI, SOME_ACTION, false, apiSyncerFactory, syncStateManager);

        LegacySyncJob r3 = new LegacySyncJob(
                CONTENT_URI, "someOtherAction", false, apiSyncerFactory, syncStateManager);

        assertThat(r1).isEqualTo(r2);
        assertThat(r3).isNotEqualTo(r2);
    }

    @Test
    public void shouldNotSyncWithNoLocalCollection() throws Exception {
        legacySyncItem.run();
        verifyZeroInteractions(apiSyncerFactory);
    }

    @Test
    public void shouldNotSyncIfLocalCollectionUpdateFails() throws Exception {
        when(apiSyncerFactory.forContentUri(CONTENT_URI)).thenReturn(SyncStrategy);
        when(syncStateManager.updateLastSyncAttemptAsync(CONTENT_URI)).thenReturn(Observable.just(false));

        legacySyncItem.onQueued();
        legacySyncItem.run();
        verifyZeroInteractions(SyncStrategy);
    }

    @Test
    public void shouldSetSyncStateToIdleAndRecordStatOnIOException() throws Exception {
        final IOException ioException = new IOException();
        setupExceptionThrowingSync(ioException);

        legacySyncItem.onQueued();
        legacySyncItem.run();

        assertThat(legacySyncItem.getResult().syncResult.stats.numIoExceptions).isEqualTo(1L);
        assertThat(legacySyncItem.getResult().syncResult.stats.numAuthExceptions).isEqualTo(0L);
    }

    @Test
    public void shouldSetSyncStateToIdleAndRecordStatOnApiRequestExceptionFromNetworkError() throws Exception {
        setupExceptionThrowingSync(ApiRequestException.networkError(null, new IOException()));

        legacySyncItem.onQueued();
        legacySyncItem.run();

        assertThat(legacySyncItem.getResult().syncResult.stats.numIoExceptions).isEqualTo(1L);
        assertThat(legacySyncItem.getResult().syncResult.stats.numAuthExceptions).isEqualTo(0L);
    }

    @Test
    public void shouldSetSyncStateToIdleAndNotSetStatsFoUnexpectedResponseException() throws Exception {
        setupExceptionThrowingSync(ApiRequestException.unexpectedResponse(null, new ApiResponse(null, HttpStatus.SC_CONFLICT, "conflict")));

        legacySyncItem.onQueued();
        legacySyncItem.run();

        assertThat(legacySyncItem.getResult().syncResult.stats.numIoExceptions).isEqualTo(0L);
        assertThat(legacySyncItem.getResult().syncResult.stats.numAuthExceptions).isEqualTo(0L);
    }

    @Test
    public void shouldSetSyncStateToIdleAndSetStatsForAuthException() throws Exception {
        setupExceptionThrowingSync(ApiRequestException.authError(null, new ApiResponse(null, 401, "status test")));

        legacySyncItem.onQueued();
        legacySyncItem.run();

        assertThat(legacySyncItem.getResult().syncResult.stats.numAuthExceptions).isEqualTo(1L);
        assertThat(legacySyncItem.getResult().syncResult.stats.numIoExceptions).isEqualTo(0L);
    }

    @Test
    public void shouldSetSyncStateToIdleAndNotSetStatsForNotFoundException() throws Exception {
        setupExceptionThrowingSync(ApiRequestException.notFound(null, null));

        legacySyncItem.onQueued();
        legacySyncItem.run();

        assertThat(legacySyncItem.getResult().syncResult.stats.numIoExceptions).isEqualTo(0L);
        assertThat(legacySyncItem.getResult().syncResult.stats.numAuthExceptions).isEqualTo(0L);
    }

    @Test
    public void shouldSetSyncStateToIdleAndNotSetStatsForNotAllowedException() throws Exception {
        setupExceptionThrowingSync(ApiRequestException.notAllowed(null, null));

        legacySyncItem.onQueued();
        legacySyncItem.run();

        assertThat(legacySyncItem.getResult().syncResult.stats.numAuthExceptions).isEqualTo(1L);
        assertThat(legacySyncItem.getResult().syncResult.stats.numIoExceptions).isEqualTo(0L);
    }

    @Test
    public void shouldSetSyncStateToIdleAndNotSetStatsForRateLimitException() throws Exception {
        setupExceptionThrowingSync(ApiRequestException.rateLimited(null, null));

        legacySyncItem.onQueued();
        legacySyncItem.run();

        assertThat(legacySyncItem.getResult().syncResult.stats.numIoExceptions).isEqualTo(0L);
        assertThat(legacySyncItem.getResult().syncResult.stats.numAuthExceptions).isEqualTo(0L);
    }

    @Test
    public void shouldSetSyncStateToIdleAndNotSetStatsForBadRequestException() throws Exception {
        setupExceptionThrowingSync(ApiRequestException.badRequest(null, null, "key test"));

        legacySyncItem.onQueued();
        legacySyncItem.run();

        assertThat(legacySyncItem.getResult().syncResult.stats.numIoExceptions).isEqualTo(0L);
        assertThat(legacySyncItem.getResult().syncResult.stats.numAuthExceptions).isEqualTo(0L);
    }

    @Test
    public void shouldSetSyncStateToIdleAndNotSetStatsForMalformedInputException() throws Exception {
        setupExceptionThrowingSync(ApiRequestException.malformedInput(null, new ApiMapperException("Test exception")));

        legacySyncItem.onQueued();
        legacySyncItem.run();

        assertThat(legacySyncItem.getResult().syncResult.stats.numIoExceptions).isEqualTo(0L);
        assertThat(legacySyncItem.getResult().syncResult.stats.numAuthExceptions).isEqualTo(0L);
    }

    @Test
    public void shouldSetSyncStateToIdleSetDelayAndNotSetStatsForApiExceptionFromServerError() throws Exception {
        setupExceptionThrowingSync(ApiRequestException.serverError(null, null));

        legacySyncItem.onQueued();
        legacySyncItem.run();

        assertThat(legacySyncItem.getResult().syncResult.stats.numIoExceptions).isEqualTo(0L);
        assertThat(legacySyncItem.getResult().syncResult.stats.numAuthExceptions).isEqualTo(0L);
        assertThat(legacySyncItem.getResult().syncResult.delayUntil).isGreaterThan(0l);
    }

    @Test
    public void shouldSetSyncStateToIdleAndNotSetStatsForUnexpectedResponseException() throws Exception {
        final UnexpectedResponseException unexpectedResponseException = new UnexpectedResponseException(Mockito.mock(Request.class), Mockito.mock(StatusLine.class));
        setupExceptionThrowingSync(unexpectedResponseException);

        legacySyncItem.onQueued();
        legacySyncItem.run();

        assertThat(legacySyncItem.getResult().syncResult.stats.numIoExceptions).isEqualTo(0L);
    }

    @Test
    public void shouldSetSyncStateToIdleAndNotSetStatsForNullPointerException() throws Exception {
        setupExceptionThrowingSync(new NullPointerException());

        legacySyncItem.onQueued();
        legacySyncItem.run();

        assertThat(legacySyncItem.getResult().syncResult.stats.numIoExceptions).isEqualTo(0L);
    }

    @Test
    public void shouldCallOnSyncComplete() throws Exception {
        setupSuccessfulSync();
        when(SyncStrategy.syncContent(CONTENT_URI, SOME_ACTION)).thenReturn(apiSyncResult);
        legacySyncItem.onQueued();
        legacySyncItem.run();
        verify(syncStateManager).onSyncComplete(apiSyncResult, CONTENT_URI);
    }

    private void setupSuccessfulSync() throws Exception {
        setupSync();
        apiSyncResult = new ApiSyncResult(CONTENT_URI);
        apiSyncResult.success = true;
        when(SyncStrategy.syncContent(CONTENT_URI, SOME_ACTION)).thenReturn(apiSyncResult);
    }

    private void setupExceptionThrowingSync(Exception e) throws Exception {
        setupSync();
        when(SyncStrategy.syncContent(CONTENT_URI, SOME_ACTION)).thenThrow(e);

    }

    private void setupSync() {
        when(apiSyncerFactory.forContentUri(CONTENT_URI)).thenReturn(SyncStrategy);
        when(syncStateManager.updateLastSyncAttempt(CONTENT_URI)).thenReturn(true);
        when(syncStateManager.updateLastSyncAttemptAsync(CONTENT_URI)).thenReturn(Observable.just(true));
    }
}
