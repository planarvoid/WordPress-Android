package com.soundcloud.android.service.sync;


import static com.soundcloud.android.Expect.expect;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.AndroidCloudAPI;
import com.soundcloud.android.api.http.Wrapper;
import com.soundcloud.android.model.LocalCollection;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.service.sync.content.SyncStrategy;
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
    private SyncStrategy syncStrategy;
    @Mock
    private LocalCollection localCollection;
    @Mock
    private SharedPreferences sharedPreferences;

    static final String NON_INTERACTIVE =
            "&" + URLEncoder.encode(Wrapper.BACKGROUND_PARAMETER) + "=1";
    private ApiSyncResult apiSyncResult;

    @Before
    public void setup() {
        collectionSyncRequest = new CollectionSyncRequest(Robolectric.application,
                Content.ME_FOLLOWINGS.uri, SOME_ACTION, false, apiSyncerFactory, syncStateManager);
    }

    @Test
    public void shouldHaveEquals() throws Exception {
        CollectionSyncRequest r1 = new CollectionSyncRequest(Robolectric.application,
                Content.ME_FOLLOWER.uri, SOME_ACTION, false);

        CollectionSyncRequest r2 = new CollectionSyncRequest(Robolectric.application,
                Content.ME_FOLLOWER.uri, SOME_ACTION, false);

        CollectionSyncRequest r3 = new CollectionSyncRequest(Robolectric.application,
                Content.ME_FOLLOWER.uri, "someOtherAction", false);

        expect(r1).toEqual(r2);
        expect(r3).not.toEqual(r2);
    }

    @Test
    public void shouldSetTheBackgroundParameterIfNonUiRequest() throws Exception {
        CollectionSyncRequest nonUi = new CollectionSyncRequest(Robolectric.application,
                Content.ME_FOLLOWER.uri, SOME_ACTION, false);
        CollectionSyncRequest ui = new CollectionSyncRequest(Robolectric.application,
                Content.ME_FOLLOWER.uri, SOME_ACTION, true);

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
        when(apiSyncerFactory.forContentUri(Robolectric.application, Content.ME_FOLLOWINGS.uri)).thenReturn(syncStrategy);
        when(syncStateManager.updateSyncState(1L, LocalCollection.SyncState.SYNCING)).thenReturn(false);

        collectionSyncRequest.onQueued();
        collectionSyncRequest.execute();
        verifyZeroInteractions(syncStrategy);
    }

    @Test
    public void shouldSetSyncStateToIdleAndRecordStatOnIOException() throws IOException {
        final IOException ioException = new IOException();
        setupExceptionThrowingSync(ioException);

        collectionSyncRequest.onQueued();
        collectionSyncRequest.execute();

        verify(syncStateManager).updateSyncState(1L, LocalCollection.SyncState.IDLE);
        expect(collectionSyncRequest.getResult().syncResult.stats.numIoExceptions).toEqual(1L);
    }

    @Test
    public void shouldSetSyncStateToIdleAndNotSetStatsForBadResponseException() throws IOException {
        final AndroidCloudAPI.UnexpectedResponseException unexpectedResponseException = new AndroidCloudAPI.UnexpectedResponseException(Mockito.mock(Request.class), Mockito.mock(StatusLine.class));
        setupExceptionThrowingSync(unexpectedResponseException);

        collectionSyncRequest.onQueued();
        collectionSyncRequest.execute();

        verify(syncStateManager).updateSyncState(1L, LocalCollection.SyncState.IDLE);
        expect(collectionSyncRequest.getResult().syncResult.stats.numIoExceptions).toEqual(0L);
    }

    @Test
    public void shouldCallOnSyncComplete() throws IOException {
        setupSuccessfulSync();
        when(syncStrategy.syncContent(Content.ME_FOLLOWINGS.uri, SOME_ACTION)).thenReturn(apiSyncResult);
        collectionSyncRequest.onQueued();
        collectionSyncRequest.execute();
        verify(syncStateManager).onSyncComplete(apiSyncResult, localCollection);
    }

    private void setupSuccessfulSync() throws IOException {
        setupSync();
        apiSyncResult = new ApiSyncResult(Content.ME_FOLLOWINGS.uri);
        apiSyncResult.success = true;
        when(syncStrategy.syncContent(Content.ME_FOLLOWINGS.uri, SOME_ACTION)).thenReturn(apiSyncResult);
    }

    private void setupExceptionThrowingSync(IOException e) throws IOException {
        setupSync();
        when(syncStrategy.syncContent(Content.ME_FOLLOWINGS.uri, SOME_ACTION)).thenThrow(e);

    }

    private void setupSync() {
        when(syncStateManager.fromContent(Content.ME_FOLLOWINGS.uri)).thenReturn(localCollection);
        when(localCollection.getId()).thenReturn(1L);
        when(apiSyncerFactory.forContentUri(Robolectric.application, Content.ME_FOLLOWINGS.uri)).thenReturn(syncStrategy);
        when(syncStateManager.updateSyncState(1L, LocalCollection.SyncState.SYNCING)).thenReturn(true);
    }
}
