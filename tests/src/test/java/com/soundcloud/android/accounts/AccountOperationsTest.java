package com.soundcloud.android.accounts;

import static com.soundcloud.android.Expect.expect;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.soundcloud.android.R;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observable;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.content.res.Resources;
import rx.Observer;

public class AccountOperationsTest {
    private AccountOperations accountOperations;
    @Mock
    private AccountManager accountManager;
    @Mock
    private Resources resources;
    @Mock
    private Account scAccount;
    @Mock
    private Context context;
    @Mock
    private Observer<Void> observer;

    @Before
    public void setUp() {
        initMocks(this);
        accountOperations = new AccountOperations(accountManager, resources, context);
    }

    @Test
    public void shouldReturnFalseIfAccountDoesNotExist() {
        when(accountManager.getAccountsByType(anyString())).thenReturn(new Account[]{});
        expect(accountOperations.soundCloudAccountExists()).toBeFalse();
    }

    @Test
    public void shouldReturnTrueIfAccountDoesExist(){
        when(accountManager.getAccountsByType(anyString())).thenReturn(new Account[]{scAccount});
        expect(accountOperations.soundCloudAccountExists()).toBeTrue();
    }

    @Test
    public void shouldCheckForExistenceOfSoundCloudAccount() {
        when(resources.getString(R.string.account_type)).thenReturn("soundcloudaccount");
        accountOperations.soundCloudAccountExists();
        verify(accountManager).getAccountsByType("soundcloudaccount");
    }

    @Test
    public void shouldReturnAccountIfItExists(){
        when(accountManager.getAccountsByType(anyString())).thenReturn(new Account[]{scAccount});
        Account account = accountOperations.getSoundCloudAccount();
        expect(account).toEqual(scAccount);
    }

    @Test
    public void shouldReturnNullIfAccountManagerReturnsNull(){
        when(accountManager.getAccountsByType(anyString())).thenReturn(null);
        Account account = accountOperations.getSoundCloudAccount();
        expect(account).toBeNull();
    }

    @Test
    public void shouldReturnNullIfAccountDoesNotExist(){
        when(accountManager.getAccountsByType(anyString())).thenReturn(new Account[]{});
        Account account = accountOperations.getSoundCloudAccount();
        expect(account).toBeNull();
    }

    @Test(expected = NullPointerException.class)
    public void shouldThrowExceptionWhenRemovingAccountIfAccountDoesNotExist(){
        when(accountManager.getAccountsByType(anyString())).thenReturn(null);
        accountOperations.removeSoundCloudAccount(observer);
    }

    @Test
    @Ignore("SCApplication needs more refactoring")
    public void shouldReturnObservableWithAccountRemovalFunction(){
        when(accountManager.getAccountsByType(anyString())).thenReturn(new Account[]{scAccount});
        accountOperations.removeSoundCloudAccount(observer);
        verify(observer).onCompleted();
    }
}
