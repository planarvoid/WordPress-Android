package com.soundcloud.android.accounts;

import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.accounts.exception.OperationFailedException;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.Subscriber;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;

import java.io.IOException;

public class AccountRemovalFunctionTest extends AndroidUnitTest {

    private AccountRemovalFunction function;

    @Mock private Account soundCloudAccount;
    @Mock private AccountManager accountManager;
    @Mock private AccountManagerFuture<Boolean> future;
    @Mock private Subscriber<Void> observer;

    @Before
    public void setup(){
        when(accountManager.removeAccount(soundCloudAccount,null,null)).thenReturn(future);
        function = new AccountRemovalFunction(soundCloudAccount, accountManager);
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

