package com.soundcloud.android.accounts;

import static com.soundcloud.android.Expect.expect;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.soundcloud.android.Actions;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.accounts.exception.OperationFailedException;
import com.soundcloud.android.c2dm.C2DMReceiver;
import com.soundcloud.android.dao.ActivitiesStorage;
import com.soundcloud.android.dao.CollectionStorage;
import com.soundcloud.android.record.SoundRecorder;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.service.playback.CloudPlaybackService;
import com.soundcloud.android.service.playback.PlayQueueManager;
import com.soundcloud.android.service.sync.SyncStateManager;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import rx.Observer;
import rx.subscriptions.Subscriptions;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import java.io.IOException;

@RunWith(SoundCloudTestRunner.class)
public class AccountRemovalFunctionTest {
    private AccountRemovalFunction function;
    @Mock
    private Account soundCloudAccount;
    @Mock
    private Context context;
    @Mock
    private AccountManager accountManager;
    @Mock
    private SyncStateManager syncStateManager;
    @Mock
    private CollectionStorage collectionStorage;
    @Mock
    private ActivitiesStorage activitiesStorage;
    @Mock
    private SoundRecorder soundRecorder;
    @Mock
    private PlayQueueManager playQueueManager;
    @Mock
    private Observer<Void> observer;
    @Mock
    private AccountManagerFuture<Boolean> future;
    @Mock
    private SharedPreferences sharedPreferences;
    @Mock
    private SharedPreferences.Editor editor;
    @Mock
    private SoundCloudApplication soundCloudApplication;
    @Mock
    private C2DMReceiver c2DMReceiver;


    @Before
    public void setup(){
        initMocks(this);
        function = new AccountRemovalFunction(soundCloudAccount, context, accountManager, syncStateManager,
                collectionStorage, activitiesStorage, soundRecorder, playQueueManager, c2DMReceiver);

        when(accountManager.removeAccount(soundCloudAccount,null,null)).thenReturn(future);
        when(context.getSharedPreferences(anyString(),anyInt())).thenReturn(sharedPreferences);
        when(sharedPreferences.edit()).thenReturn(editor);
        when(context.getApplicationContext()).thenReturn(soundCloudApplication);
    }

    @Test
    public void shouldCallOnErrorIfAccountRemovalFails() throws AuthenticatorException, OperationCanceledException, IOException {
        when(future.getResult()).thenReturn(false);
        function.call(observer);
        verify(observer).onError(isA(OperationFailedException.class));
    }

    @Test
    public void shouldCallOnErrorIfExceptionIsThrown() throws AuthenticatorException, OperationCanceledException, IOException {
        when(future.getResult()).thenThrow(IOException.class);
        function.call(observer);
        verify(observer).onError(isA(IOException.class));
    }

    @Test
    public void shouldCallOnCompleteIfAccountRemovalSucceeds() throws AuthenticatorException, OperationCanceledException, IOException {
        when(future.getResult()).thenReturn(true);
        function.call(observer);
        verify(observer).onCompleted();
    }

    @Test
    public void shouldReturnEmptySubscriptionAtEndOfProcessing(){
        expect(function.call(observer)).toEqual(Subscriptions.empty());
    }

    @Test
    public void shouldClearSyncStateIfAccountRemovalSucceeds() throws AuthenticatorException, OperationCanceledException, IOException {
        when(future.getResult()).thenReturn(true);
        function.call(observer);
        verify(syncStateManager).clear();
    }

    @Test
    public void shouldNotClearSyncStateIfAccountRemovalFails() throws AuthenticatorException, OperationCanceledException, IOException {
        when(future.getResult()).thenReturn(false);
        function.call(observer);
        verifyZeroInteractions(syncStateManager);
    }

    @Test
    public void shouldClearCollectionStorageIfAccountRemovalSucceeds() throws AuthenticatorException, OperationCanceledException, IOException {
        when(future.getResult()).thenReturn(true);
        function.call(observer);
        verify(collectionStorage).clear();
    }

    @Test
    public void shouldNotClearCollectionStorageIfAccountRemovalFails() throws AuthenticatorException, OperationCanceledException, IOException {
        when(future.getResult()).thenReturn(false);
        function.call(observer);
        verifyZeroInteractions(collectionStorage);
    }

    @Test
    public void shouldClearActivitiesStorageIfAccountRemovalSucceeds() throws AuthenticatorException, OperationCanceledException, IOException {
        when(future.getResult()).thenReturn(true);
        function.call(observer);
        verify(collectionStorage).clear();
    }

    @Test
    public void shouldNotClearActivitiesStorageIfAccountRemovalFails() throws AuthenticatorException, OperationCanceledException, IOException {
        when(future.getResult()).thenReturn(false);
        function.call(observer);
        verifyZeroInteractions(collectionStorage);
    }

    @Test
    public void shouldResetSoundRecorderIfAccountRemovalSucceeds() throws AuthenticatorException, OperationCanceledException, IOException {
        when(future.getResult()).thenReturn(true);
        function.call(observer);
        verify(soundRecorder).reset();
    }

    @Test
    public void shouldNotResetSoundRecorderIfAccountRemovalFails() throws AuthenticatorException, OperationCanceledException, IOException {
        when(future.getResult()).thenReturn(false);
        function.call(observer);
        verifyZeroInteractions(soundRecorder);
    }

    @Test
    public void shouldClearPlayQueueManagersStateIfAccountRemovalSucceeds() throws AuthenticatorException, OperationCanceledException, IOException {
        when(future.getResult()).thenReturn(true);
        function.call(observer);
        verify(playQueueManager).clearState();
    }

    @Test
    public void shouldNotClearPlayQueueManagersStateIfAccountRemovalSucceeds() throws AuthenticatorException, OperationCanceledException, IOException {
        when(future.getResult()).thenReturn(false);
        function.call(observer);
        verifyZeroInteractions(playQueueManager);
    }

    @Test
    public void shouldBroadcastLogOutIntentIfAccountRemovalSucceeds() throws AuthenticatorException, OperationCanceledException, IOException {
        when(future.getResult()).thenReturn(true);
        function.call(observer);
        ArgumentCaptor<Intent> argumentCaptor = ArgumentCaptor.forClass(Intent.class);
        verify(context, times(2)).sendBroadcast(argumentCaptor.capture());
        Intent broadcastIntent = argumentCaptor.getAllValues().get(0);
        assertThat(broadcastIntent.getAction(), is(Actions.LOGGING_OUT));
    }

    @Test
    public void shouldBroadcastResetAllIntentIfAccountRemovalSucceeds() throws AuthenticatorException, OperationCanceledException, IOException {
        when(future.getResult()).thenReturn(true);
        function.call(observer);
        ArgumentCaptor<Intent> argumentCaptor = ArgumentCaptor.forClass(Intent.class);
        verify(context, times(2)).sendBroadcast(argumentCaptor.capture());
        Intent broadcastIntent = argumentCaptor.getAllValues().get(1);
        assertThat(broadcastIntent.getAction(), is(CloudPlaybackService.RESET_ALL));
    }

    @Test
    public void shouldNotBroadcastAnyIntentsIfAccountRemovalFails() throws AuthenticatorException, OperationCanceledException, IOException {
        when(future.getResult()).thenReturn(false);
        function.call(observer);
        verify(context, never()).sendBroadcast(any(Intent.class));
    }

    @Test
    public void shouldUnregisterFromC2DMIfAccountRemovalSucceeds() throws AuthenticatorException, OperationCanceledException, IOException {
        when(future.getResult()).thenReturn(true);
        function.call(observer);
        verify(c2DMReceiver).unregister(context);
    }

    @Test
    public void shouldNotUnregisterFromC2DMIfAccountRemovalFails() throws AuthenticatorException, OperationCanceledException, IOException {
        when(future.getResult()).thenReturn(false);
        function.call(observer);
        verifyZeroInteractions(c2DMReceiver);
    }

    @Test
    public void shouldClearLoggedInUserIfAccountRemovalSucceeds() throws AuthenticatorException, OperationCanceledException, IOException {
        when(future.getResult()).thenReturn(true);
        function.call(observer);
        verify(soundCloudApplication).clearLoggedInUser();
    }

    @Test
    public void shouldNotClearAnyStateOnApplicationAccountRemovalFails() throws AuthenticatorException, OperationCanceledException, IOException {
        when(future.getResult()).thenReturn(false);
        function.call(observer);
        verifyZeroInteractions(soundCloudApplication);
    }

}

