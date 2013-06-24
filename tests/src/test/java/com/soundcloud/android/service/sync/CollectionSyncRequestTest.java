package com.soundcloud.android.service.sync;


import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.soundcloud.android.api.http.Wrapper;
import com.soundcloud.android.model.LocalCollection;
import com.soundcloud.android.provider.Content;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.service.sync.content.SyncStrategy;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;

import android.content.SharedPreferences;

import java.io.IOException;
import java.net.URLEncoder;

@RunWith(SoundCloudTestRunner.class)
public class CollectionSyncRequestTest {

    public static final String SOME_ACTION = "someAction";
    public static final String RESULT_PREF_KEY = CollectionSyncRequest.PREFIX_LAST_SYNC_RESULT + Content.ME_FOLLOWINGS.uri.toString();
    CollectionSyncRequest collectionSyncRequest;

    @Mock
    ApiSyncerFactory apiSyncerFactory;
    @Mock
    SyncStateManager syncStateManager;
    @Mock
    SyncStrategy syncStrategy;
    @Mock
    LocalCollection localCollection;
    @Mock
    SharedPreferences sharedPreferences;
    @Mock
    SharedPreferences.Editor sharedPreferencesEditor;

    static final String NON_INTERACTIVE =
            "&" + URLEncoder.encode(Wrapper.BACKGROUND_PARAMETER) + "=1";
    private ApiSyncResult apiSyncResult;

    @Before
    public void setup() {
        initMocks(this);
        collectionSyncRequest = new CollectionSyncRequest(Robolectric.application,
                Content.ME_FOLLOWINGS.uri, SOME_ACTION, true, apiSyncerFactory, syncStateManager, sharedPreferences);
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
    public void shouldSetSyncStateToIdleAndRecordErrorOnException() throws IOException {
        setupSync();

        final IOException ioException = new IOException();
        when(syncStrategy.syncContent(Content.ME_FOLLOWINGS.uri, SOME_ACTION)).thenThrow(ioException);

        when(sharedPreferencesEditor.putString(RESULT_PREF_KEY, ioException.toString())).thenReturn(sharedPreferencesEditor);

        collectionSyncRequest.onQueued();
        collectionSyncRequest.execute();

        verify(syncStateManager).updateSyncState(1L, LocalCollection.SyncState.IDLE);
        verify(sharedPreferencesEditor).putString(RESULT_PREF_KEY, ioException.toString());
        verify(sharedPreferencesEditor).commit();
    }

    @Test
    public void shouldCallOnSyncComplete() throws IOException {
        setupSuccessfulSync();
        when(syncStrategy.syncContent(Content.ME_FOLLOWINGS.uri, SOME_ACTION)).thenReturn(apiSyncResult);

        collectionSyncRequest.onQueued();
        collectionSyncRequest.execute();
        verify(syncStateManager).onSyncComplete(apiSyncResult, localCollection);
    }

    @Test
    public void shouldSetLastResultToSuccessInPrefsWithSuccessResult() throws IOException {
        setupSuccessfulSync();

        collectionSyncRequest.onQueued();
        collectionSyncRequest.execute();

        verify(sharedPreferencesEditor).putString(RESULT_PREF_KEY, CollectionSyncRequest.PREF_VAL_SUCCESS);
        verify(sharedPreferencesEditor).commit();
    }

    @Test
    public void shouldSetLastResultToFailureInPrefsWithFailureResult() throws IOException {
        setupFailedSync();
        collectionSyncRequest.onQueued();
        collectionSyncRequest.execute();

        verify(sharedPreferencesEditor).putString(RESULT_PREF_KEY, CollectionSyncRequest.PREF_VAL_FAILED);
        verify(sharedPreferencesEditor).commit();
    }

    @Test
    public void shouldSendSilentRetryViolationWithLastResult() throws IOException {
        setupSuccessfulSync();

        localCollection.last_sync_attempt = System.currentTimeMillis();

        final String some_kind_of_error = "SOME_KIND_OF_ERROR";
        when(sharedPreferences.getString(RESULT_PREF_KEY, CollectionSyncRequest.PREF_VAL_NULL)).thenReturn(some_kind_of_error);

        CollectionSyncRequest spyRequest = Mockito.spy(collectionSyncRequest);
        spyRequest.onQueued();
        spyRequest.execute();

        ArgumentCaptor<CollectionSyncRequest.SyncRetryViolation> captor = ArgumentCaptor.forClass(CollectionSyncRequest.SyncRetryViolation.class);
        verify(spyRequest).sendRetryViolation(anyString(), captor.capture());

        final String exceptionString = captor.getValue().toString();
        expect(exceptionString.contains(Content.ME_FOLLOWINGS.uri.toString())).toBeTrue();
        expect(exceptionString.contains(some_kind_of_error)).toBeTrue();
    }

    private void setupSuccessfulSync() throws IOException {
        setupSync();
        apiSyncResult = new ApiSyncResult(Content.ME_FOLLOWINGS.uri);
        apiSyncResult.success = true;
        when(syncStrategy.syncContent(Content.ME_FOLLOWINGS.uri, SOME_ACTION)).thenReturn(apiSyncResult);
        when(sharedPreferencesEditor.putString(RESULT_PREF_KEY, CollectionSyncRequest.PREF_VAL_SUCCESS)).thenReturn(sharedPreferencesEditor);
    }

    private void setupFailedSync() throws IOException {
        setupSync();
        apiSyncResult = new ApiSyncResult(Content.ME_FOLLOWINGS.uri);
        apiSyncResult.success = false;
        when(syncStrategy.syncContent(Content.ME_FOLLOWINGS.uri, SOME_ACTION)).thenReturn(apiSyncResult);
        when(sharedPreferencesEditor.putString(RESULT_PREF_KEY, CollectionSyncRequest.PREF_VAL_FAILED)).thenReturn(sharedPreferencesEditor);
    }

    private void setupSync() {
        when(syncStateManager.fromContent(Content.ME_FOLLOWINGS.uri)).thenReturn(localCollection);
        when(localCollection.getId()).thenReturn(1L);
        when(apiSyncerFactory.forContentUri(Robolectric.application, Content.ME_FOLLOWINGS.uri)).thenReturn(syncStrategy);
        when(syncStateManager.updateSyncState(1L, LocalCollection.SyncState.SYNCING)).thenReturn(true);
        when(sharedPreferences.edit()).thenReturn(sharedPreferencesEditor);
    }
}
