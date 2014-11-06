package com.soundcloud.android.sync;


import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.legacy.PublicCloudAPI;
import com.soundcloud.android.api.legacy.PublicApiWrapper;
import com.soundcloud.android.api.legacy.model.LocalCollection;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.storage.provider.Content;
import com.soundcloud.android.sync.content.SyncStrategy;
import com.soundcloud.api.Request;
import com.xtremelabs.robolectric.Robolectric;
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
public class CollectionSyncRequestTest {

    public static final String SOME_ACTION = "someAction";

    CollectionSyncRequest collectionSyncRequest;

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
        collectionSyncRequest = new CollectionSyncRequest(Robolectric.application,
                Content.ME_FOLLOWINGS.uri, SOME_ACTION, false, apiSyncerFactory, syncStateManager);
    }

    @Test
    public void shouldHaveEquals() throws Exception {
        CollectionSyncRequest r1 = new CollectionSyncRequest(Robolectric.application,
                Content.ME_FOLLOWER.uri, SOME_ACTION, false, apiSyncerFactory, syncStateManager);

        CollectionSyncRequest r2 = new CollectionSyncRequest(Robolectric.application,
                Content.ME_FOLLOWER.uri, SOME_ACTION, false, apiSyncerFactory, syncStateManager);

        CollectionSyncRequest r3 = new CollectionSyncRequest(Robolectric.application,
                Content.ME_FOLLOWER.uri, "someOtherAction", false, apiSyncerFactory, syncStateManager);

        expect(r1).toEqual(r2);
        expect(r3).not.toEqual(r2);
    }

    @Test
    public void shouldSetTheBackgroundParameterIfNonUiRequest() throws Exception {
        CollectionSyncRequest nonUi = new CollectionSyncRequest(Robolectric.application,
                Content.ME_FOLLOWER.uri, SOME_ACTION, false, apiSyncerFactory, syncStateManager);
        CollectionSyncRequest ui = new CollectionSyncRequest(Robolectric.application,
                Content.ME_FOLLOWER.uri, SOME_ACTION, true, apiSyncerFactory, syncStateManager);

        ui.onQueued();
        nonUi.onQueued();

        Robolectric.addHttpResponseRule("/me/followers/ids?linked_partitioning=1"
                + NON_INTERACTIVE, "whatevs");

        nonUi.execute();
        Robolectric.clearHttpResponseRules();
        Robolectric.addHttpResponseRule("/me/followers/ids?linked_partitioning=1", "whatevs");
        ui.execute();
    }

    @Test
    public void shouldNotSyncWithNoLocalCollection() throws Exception {
        collectionSyncRequest.execute();
        verifyZeroInteractions(apiSyncerFactory);
    }

    @Test
    public void shouldNotSyncIfLocalCollectionUpdateFails() throws Exception {
        when(syncStateManager.fromContent(Content.ME_FOLLOWINGS.uri)).thenReturn(localCollection);
        when(localCollection.getId()).thenReturn(1L);
        when(apiSyncerFactory.forContentUri(Robolectric.application, Content.ME_FOLLOWINGS.uri)).thenReturn(SyncStrategy);
        when(syncStateManager.updateSyncState(1L, LocalCollection.SyncState.SYNCING)).thenReturn(false);

        collectionSyncRequest.onQueued();
        collectionSyncRequest.execute();
        verifyZeroInteractions(SyncStrategy);
    }

    @Test
    public void shouldSetSyncStateToIdleAndRecordStatOnIOException() throws Exception {
        final IOException ioException = new IOException();
        setupExceptionThrowingSync(ioException);

        collectionSyncRequest.onQueued();
        collectionSyncRequest.execute();

        verify(syncStateManager).updateSyncState(1L, LocalCollection.SyncState.IDLE);
        expect(collectionSyncRequest.getResult().syncResult.stats.numIoExceptions).toEqual(1L);
    }

    @Test
    public void shouldSetSyncStateToIdleAndNotSetStatsForBadResponseException() throws Exception {
        final PublicCloudAPI.UnexpectedResponseException unexpectedResponseException = new PublicCloudAPI.UnexpectedResponseException(Mockito.mock(Request.class), Mockito.mock(StatusLine.class));
        setupExceptionThrowingSync(unexpectedResponseException);

        collectionSyncRequest.onQueued();
        collectionSyncRequest.execute();

        verify(syncStateManager).updateSyncState(1L, LocalCollection.SyncState.IDLE);
        expect(collectionSyncRequest.getResult().syncResult.stats.numIoExceptions).toEqual(0L);
    }

    @Test
    public void shouldSetSyncStateToIdleAndNotSetStatsForNullPointerException() throws Exception {
        setupExceptionThrowingSync(new NullPointerException());

        collectionSyncRequest.onQueued();
        collectionSyncRequest.execute();

        verify(syncStateManager).updateSyncState(1L, LocalCollection.SyncState.IDLE);
        expect(collectionSyncRequest.getResult().syncResult.stats.numIoExceptions).toEqual(0L);
    }

    @Test
    public void shouldCallOnSyncComplete() throws Exception {
        setupSuccessfulSync();
        when(SyncStrategy.syncContent(Content.ME_FOLLOWINGS.uri, SOME_ACTION)).thenReturn(apiSyncResult);
        collectionSyncRequest.onQueued();
        collectionSyncRequest.execute();
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
