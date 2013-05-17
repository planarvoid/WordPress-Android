package com.soundcloud.android.accounts;

import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.accounts.exception.OperationFailedException;
import com.soundcloud.android.dao.ActivitiesStorage;
import com.soundcloud.android.dao.CollectionStorage;
import com.soundcloud.android.record.SoundRecorder;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.service.playback.PlayQueueManager;
import com.soundcloud.android.service.sync.SyncStateManager;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.Observer;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.Context;
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


    @Before
    public void setup(){
        initMocks(this);
        function = new AccountRemovalFunction(soundCloudAccount, context, accountManager, syncStateManager,
                collectionStorage, activitiesStorage, soundRecorder, playQueueManager);

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
}

