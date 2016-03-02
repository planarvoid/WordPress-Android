package com.soundcloud.android.accounts;

import static org.mockito.Mockito.when;

import com.soundcloud.android.accounts.exception.OperationFailedException;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.observers.TestSubscriber;

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

    private TestSubscriber<Void> subscriber = new TestSubscriber<>();

    @Before
    public void setup(){
        when(accountManager.removeAccount(soundCloudAccount,null,null)).thenReturn(future);
        function = new AccountRemovalFunction(soundCloudAccount, accountManager);
    }

    @Test
    public void shouldCallOnErrorIfAccountRemovalFails() throws AuthenticatorException, OperationCanceledException, IOException {
        when(future.getResult()).thenReturn(false);
        function.call(subscriber);

        subscriber.assertError(OperationFailedException.class);
    }

    @Test
    public void shouldCallOnErrorIfExceptionIsThrown() throws AuthenticatorException, OperationCanceledException, IOException {
        when(future.getResult()).thenThrow(IOException.class);
        function.call(subscriber);

        subscriber.assertError(IOException.class);
    }

    @Test
    public void shouldCallOnCompleteIfAccountRemovalSucceeds() throws AuthenticatorException, OperationCanceledException, IOException {
        when(future.getResult()).thenReturn(true);
        function.call(subscriber);

        subscriber.assertCompleted();
    }
}

