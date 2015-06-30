package com.soundcloud.android.sync;


import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.ApiMapperException;
import com.soundcloud.android.api.ApiRequestException;
import com.soundcloud.android.api.legacy.PublicApiWrapper;
import com.soundcloud.android.api.legacy.UnexpectedResponseException;
import com.soundcloud.android.api.legacy.model.LocalCollection;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.sync.content.SyncStrategy;
import com.soundcloud.api.InvalidTokenException;
import com.soundcloud.api.Request;
import com.xtremelabs.robolectric.Robolectric;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;

import android.content.SharedPreferences;

import java.io.IOException;
import java.net.URLEncoder;

@RunWith(SoundCloudTestRunner.class)
public class LegacySyncJobTest {

    public static final String SOME_ACTION = "someAction";

    LegacySyncJob legacySyncItem;

    @Mock
    private ApiSyncerFactory apiSyncerFactory;
    @Mock
    private SyncStateManager syncStateManager;
    @Mock
    private SyncStrategy SyncStrategy;
    @Mock
    private LocalCollection localCollection;
    @Mock
    private SharedPreferences sharedPreferences;

    static final String NON_INTERACTIVE =
            "&" + URLEncoder.encode(PublicApiWrapper.BACKGROUND_PARAMETER) + "=1";
    private ApiSyncResult apiSyncResult;

    @Before
    public void setup() {
        legacySyncItem = new LegacySyncJob(Robolectric.application,
                Content.ME_FOLLOWINGS.uri, SOME_ACTION, false, apiSyncerFactory, syncStateManager);
    }

    @Test
    public void shouldHaveEquals() throws Exception {
        LegacySyncJob r1 = new LegacySyncJob(Robolectric.application,
                Content.ME_FOLLOWER.uri, SOME_ACTION, false, apiSyncerFactory, syncStateManager);

        LegacySyncJob r2 = new LegacySyncJob(Robolectric.application,
                Content.ME_FOLLOWER.uri, SOME_ACTION, false, apiSyncerFactory, syncStateManager);

        LegacySyncJob r3 = new LegacySyncJob(Robolectric.application,
                Content.ME_FOLLOWER.uri, "someOtherAction", false, apiSyncerFactory, syncStateManager);

        expect(r1).toEqual(r2);
        expect(r3).not.toEqual(r2);
    }

    @Test
    public void shouldSetTheBackgroundParameterIfNonUiRequest() throws Exception {
        LegacySyncJob nonUi = new LegacySyncJob(Robolectric.application,
                Content.ME_FOLLOWER.uri, SOME_ACTION, false, apiSyncerFactory, syncStateManager);
        LegacySyncJob ui = new LegacySyncJob(Robolectric.application,
                Content.ME_FOLLOWER.uri, SOME_ACTION, true, apiSyncerFactory, syncStateManager);

        ui.onQueued();
        nonUi.onQueued();

        Robolectric.addHttpResponseRule("/me/followers/ids?linked_partitioning=1"
                + NON_INTERACTIVE, "whatevs");

        nonUi.run();
        Robolectric.clearHttpResponseRules();
        Robolectric.addHttpResponseRule("/me/followers/ids?linked_partitioning=1", "whatevs");
        ui.run();
    }

    @Test
    public void shouldNotSyncWithNoLocalCollection() throws Exception {
        legacySyncItem.run();
        verifyZeroInteractions(apiSyncerFactory);
    }

    @Test
    public void shouldNotSyncIfLocalCollectionUpdateFails() throws Exception {
        when(syncStateManager.fromContent(Content.ME_FOLLOWINGS.uri)).thenReturn(localCollection);
        when(localCollection.getId()).thenReturn(1L);
        when(apiSyncerFactory.forContentUri(Robolectric.application, Content.ME_FOLLOWINGS.uri)).thenReturn(SyncStrategy);
        when(syncStateManager.updateSyncState(1L, LocalCollection.SyncState.SYNCING)).thenReturn(false);

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

        verify(syncStateManager).updateSyncState(1L, LocalCollection.SyncState.IDLE);
        expect(legacySyncItem.getResult().syncResult.stats.numIoExceptions).toEqual(1L);
        expect(legacySyncItem.getResult().syncResult.stats.numAuthExceptions).toEqual(0L);
    }

    @Test
    public void shouldSetSyncStateToIdleAndRecordStatOnApiRequestExceptionFromNetworkError() throws Exception {
        setupExceptionThrowingSync(ApiRequestException.networkError(null, new IOException()));

        legacySyncItem.onQueued();
        legacySyncItem.run();

        verify(syncStateManager).updateSyncState(1L, LocalCollection.SyncState.IDLE);
        expect(legacySyncItem.getResult().syncResult.stats.numIoExceptions).toEqual(1L);
        expect(legacySyncItem.getResult().syncResult.stats.numAuthExceptions).toEqual(0L);
    }

    @Test
    public void shouldSetSyncStateToIdleAndNotSetStatsFoUnexpectedResponseException() throws Exception {
        setupExceptionThrowingSync(ApiRequestException.unexpectedResponse(null, HttpStatus.SC_CONFLICT));

        legacySyncItem.onQueued();
        legacySyncItem.run();

        verify(syncStateManager).updateSyncState(1L, LocalCollection.SyncState.IDLE);
        expect(legacySyncItem.getResult().syncResult.stats.numIoExceptions).toEqual(0L);
        expect(legacySyncItem.getResult().syncResult.stats.numAuthExceptions).toEqual(0L);
    }

    @Test
    public void shouldSetSyncStateToIdleAndSetStatsForAuthException() throws Exception {
        setupExceptionThrowingSync(ApiRequestException.authError(null, new InvalidTokenException(401, "status test")));

        legacySyncItem.onQueued();
        legacySyncItem.run();

        verify(syncStateManager).updateSyncState(1L, LocalCollection.SyncState.IDLE);
        expect(legacySyncItem.getResult().syncResult.stats.numAuthExceptions).toEqual(1L);
        expect(legacySyncItem.getResult().syncResult.stats.numIoExceptions).toEqual(0L);
    }

    @Test
    public void shouldSetSyncStateToIdleAndNotSetStatsForNotFoundException() throws Exception {
        setupExceptionThrowingSync(ApiRequestException.notFound(null));

        legacySyncItem.onQueued();
        legacySyncItem.run();

        verify(syncStateManager).updateSyncState(1L, LocalCollection.SyncState.IDLE);
        expect(legacySyncItem.getResult().syncResult.stats.numIoExceptions).toEqual(0L);
        expect(legacySyncItem.getResult().syncResult.stats.numAuthExceptions).toEqual(0L);
    }

    @Test
    public void shouldSetSyncStateToIdleAndNotSetStatsForNotAllowedException() throws Exception {
        setupExceptionThrowingSync(ApiRequestException.notAllowed(null));

        legacySyncItem.onQueued();
        legacySyncItem.run();

        verify(syncStateManager).updateSyncState(1L, LocalCollection.SyncState.IDLE);
        expect(legacySyncItem.getResult().syncResult.stats.numAuthExceptions).toEqual(1L);
        expect(legacySyncItem.getResult().syncResult.stats.numIoExceptions).toEqual(0L);
    }

    @Test
    public void shouldSetSyncStateToIdleAndNotSetStatsForRateLimitException() throws Exception {
        setupExceptionThrowingSync(ApiRequestException.rateLimited(null));

        legacySyncItem.onQueued();
        legacySyncItem.run();

        verify(syncStateManager).updateSyncState(1L, LocalCollection.SyncState.IDLE);
        expect(legacySyncItem.getResult().syncResult.stats.numIoExceptions).toEqual(0L);
        expect(legacySyncItem.getResult().syncResult.stats.numAuthExceptions).toEqual(0L);
    }

    @Test
    public void shouldSetSyncStateToIdleAndNotSetStatsForBadRequestException() throws Exception {
        setupExceptionThrowingSync(ApiRequestException.badRequest(null, "key test"));

        legacySyncItem.onQueued();
        legacySyncItem.run();

        verify(syncStateManager).updateSyncState(1L, LocalCollection.SyncState.IDLE);
        expect(legacySyncItem.getResult().syncResult.stats.numIoExceptions).toEqual(0L);
        expect(legacySyncItem.getResult().syncResult.stats.numAuthExceptions).toEqual(0L);
    }

    @Test
    public void shouldSetSyncStateToIdleAndNotSetStatsForMalformedInputException() throws Exception {
        setupExceptionThrowingSync(ApiRequestException.malformedInput(null, new ApiMapperException("Test exception")));

        legacySyncItem.onQueued();
        legacySyncItem.run();

        verify(syncStateManager).updateSyncState(1L, LocalCollection.SyncState.IDLE);
        expect(legacySyncItem.getResult().syncResult.stats.numIoExceptions).toEqual(0L);
        expect(legacySyncItem.getResult().syncResult.stats.numAuthExceptions).toEqual(0L);
    }

    @Test
    public void shouldSetSyncStateToIdleSetDelayAndNotSetStatsForApiExceptionFromServerError() throws Exception {
        setupExceptionThrowingSync(ApiRequestException.serverError(null));

        legacySyncItem.onQueued();
        legacySyncItem.run();

        verify(syncStateManager).updateSyncState(1L, LocalCollection.SyncState.IDLE);
        expect(legacySyncItem.getResult().syncResult.stats.numIoExceptions).toEqual(0L);
        expect(legacySyncItem.getResult().syncResult.stats.numAuthExceptions).toEqual(0L);
        expect(legacySyncItem.getResult().syncResult.delayUntil).toBeGreaterThan(0l);
    }

    @Test
    public void shouldSetSyncStateToIdleAndNotSetStatsForUnexpectedResponseException() throws Exception {
        final UnexpectedResponseException unexpectedResponseException = new UnexpectedResponseException(Mockito.mock(Request.class), Mockito.mock(StatusLine.class));
        setupExceptionThrowingSync(unexpectedResponseException);

        legacySyncItem.onQueued();
        legacySyncItem.run();

        verify(syncStateManager).updateSyncState(1L, LocalCollection.SyncState.IDLE);
        expect(legacySyncItem.getResult().syncResult.stats.numIoExceptions).toEqual(0L);
    }

    @Test
    public void shouldSetSyncStateToIdleAndNotSetStatsForNullPointerException() throws Exception {
        setupExceptionThrowingSync(new NullPointerException());

        legacySyncItem.onQueued();
        legacySyncItem.run();

        verify(syncStateManager).updateSyncState(1L, LocalCollection.SyncState.IDLE);
        expect(legacySyncItem.getResult().syncResult.stats.numIoExceptions).toEqual(0L);
    }

    @Test
    public void shouldCallOnSyncComplete() throws Exception {
        setupSuccessfulSync();
        when(SyncStrategy.syncContent(Content.ME_FOLLOWINGS.uri, SOME_ACTION)).thenReturn(apiSyncResult);
        legacySyncItem.onQueued();
        legacySyncItem.run();
        verify(syncStateManager).onSyncComplete(apiSyncResult, localCollection);
    }

    private void setupSuccessfulSync() throws Exception {
        setupSync();
        apiSyncResult = new ApiSyncResult(Content.ME_FOLLOWINGS.uri);
        apiSyncResult.success = true;
        when(SyncStrategy.syncContent(Content.ME_FOLLOWINGS.uri, SOME_ACTION)).thenReturn(apiSyncResult);
    }

    private void setupExceptionThrowingSync(Exception e) throws Exception {
        setupSync();
        when(SyncStrategy.syncContent(Content.ME_FOLLOWINGS.uri, SOME_ACTION)).thenThrow(e);

    }

    private void setupSync() {
        when(syncStateManager.fromContent(Content.ME_FOLLOWINGS.uri)).thenReturn(localCollection);
        when(localCollection.getId()).thenReturn(1L);
        when(apiSyncerFactory.forContentUri(Robolectric.application, Content.ME_FOLLOWINGS.uri)).thenReturn(SyncStrategy);
        when(syncStateManager.updateSyncState(1L, LocalCollection.SyncState.SYNCING)).thenReturn(true);
    }
}
