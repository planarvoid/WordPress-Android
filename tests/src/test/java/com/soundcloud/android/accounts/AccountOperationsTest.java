package com.soundcloud.android.accounts;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.soundcloud.android.activity.auth.SignupVia;
import com.soundcloud.android.model.User;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.api.Token;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.Observer;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.os.Bundle;

@RunWith(SoundCloudTestRunner.class)
public class AccountOperationsTest {
    private static final String SC_ACCOUNT_TYPE = "com.soundcloud.android.account";
    public static final String KEY = "key";

    private AccountOperations accountOperations;
    @Mock
    private AccountManager accountManager;
    @Mock
    private SoundCloudTokenOperations tokenOperations;
    @Mock
    private Account scAccount;
    @Mock
    private Observer<Void> observer;
    @Mock
    private User user;
    @Mock
    private Token token;

    @Before
    public void setUp() {
        initMocks(this);
        accountOperations = new AccountOperations(accountManager, Robolectric.application, tokenOperations);
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
        accountOperations.soundCloudAccountExists();
        verify(accountManager).getAccountsByType(SC_ACCOUNT_TYPE);
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
    public void shouldAddAccountUsingAccountManager(){
        Activity activity = mock(Activity.class);
        accountOperations.addSoundCloudAccountManually(activity);
        verify(accountManager).addAccount(SC_ACCOUNT_TYPE, "access_token",null,null,activity,null,null);
    }

    @Test
    public void shouldReturnNullIfAccountAdditionFails(){
        when(user.username()).thenReturn("username");
        when(accountManager.addAccountExplicitly(any(Account.class), anyString(), any(Bundle.class))).thenReturn(false);
        expect(accountOperations.addSoundCloudAccountExplicitly(user, token, SignupVia.API)).toBeNull();

    }

    @Test
    public void shouldSetUserDataIfAccountAdditionSucceeds(){
        Account account = new Account("username", SC_ACCOUNT_TYPE);

        when(accountManager.addAccountExplicitly(account,null,null)).thenReturn(true);
        when(user.getId()).thenReturn(2L);
        when(user.username()).thenReturn("username");
        when(user.permalink()).thenReturn("permalink");

        accountOperations.addSoundCloudAccountExplicitly(user, token, SignupVia.API);
        verify(accountManager).setUserData(account, User.DataKeys.USER_ID, "2");
        verify(accountManager).setUserData(account, User.DataKeys.USERNAME, "username");
        verify(accountManager).setUserData(account, User.DataKeys.USER_PERMALINK, "permalink");
        verify(accountManager).setUserData(account, User.DataKeys.SIGNUP, SignupVia.API.signupIdentifier());
    }

    @Test
    public void shouldSetAuthTokenInformationIfAccountAdditionSucceeds(){
        Account account = new Account("username", SC_ACCOUNT_TYPE);

        when(user.username()).thenReturn("username");
        when(accountManager.addAccountExplicitly(account, null, null)).thenReturn(true);

        accountOperations.addSoundCloudAccountExplicitly(user, token, SignupVia.API);

        verify(tokenOperations).storeSoundCloudTokenData(account, token);
    }

    @Test
    public void shouldReturnAddedAccountIfAccountAdditionSucceeds(){
        Account account = new Account("username", SC_ACCOUNT_TYPE);

        when(user.username()).thenReturn("username");
        when(accountManager.addAccountExplicitly(account,null,null)).thenReturn(true);

        expect(accountOperations.addSoundCloudAccountExplicitly(user, token, SignupVia.API)).toEqual(account);

    }

    @Test
    public void shouldReturnNullStringDataIfAccountDoesNotExist(){
        when(accountManager.getAccountsByType(anyString())).thenReturn(null);
        String data = accountOperations.getAccountDataString(KEY);
        expect(data).toBeNull();
        verify(accountManager, never()).getUserData(any(Account.class), any(String.class));
    }

    @Test
    public void shouldReturnStringAccountDataIfAccountExists(){
        when(accountManager.getAccountsByType(anyString())).thenReturn(new Account[]{scAccount});
        when(accountManager.getUserData(scAccount, KEY)).thenReturn("data");
        String data = accountOperations.getAccountDataString(KEY);
        expect(data).toBe("data");
    }

    @Test
    public void shouldReturnNegativeOneIfLongAccountDataDoesNotExist(){
        when(accountManager.getAccountsByType(SC_ACCOUNT_TYPE)).thenReturn(null);
        expect(accountOperations.getAccountDataLong(KEY)).toBe(-1L);
    }

    @Test
    public void shouldReturnExpectedValueIfLongAccountDataDoesExist(){
        when(accountManager.getAccountsByType(SC_ACCOUNT_TYPE)).thenReturn(new Account[]{scAccount});
        when(accountManager.getUserData(scAccount, KEY)).thenReturn("23");
        expect(accountOperations.getAccountDataLong(KEY)).toBe(23L);
    }

    @Test
    public void shouldReturnFalseIfBooleanAccountDataDoesNotExist(){
        when(accountManager.getAccountsByType(SC_ACCOUNT_TYPE)).thenReturn(null);
        expect(accountOperations.getAccountDataBoolean(KEY)).toBe(false);
    }

    @Test
    public void shouldReturnTrueValueIfTrueBoolAccountDataDoesExist(){
        when(accountManager.getAccountsByType(SC_ACCOUNT_TYPE)).thenReturn(new Account[]{scAccount});
        when(accountManager.getUserData(scAccount, KEY)).thenReturn("true");
        expect(accountOperations.getAccountDataBoolean(KEY)).toBe(true);
    }

    @Test
    public void shouldReturnFalseValueIfFalseBoolAccountDataDoesExist(){
        when(accountManager.getAccountsByType(SC_ACCOUNT_TYPE)).thenReturn(new Account[]{scAccount});
        when(accountManager.getUserData(scAccount, KEY)).thenReturn("false");
        expect(accountOperations.getAccountDataBoolean(KEY)).toBe(false);
        verify(accountManager).getUserData(scAccount, KEY);
    }

    @Test
    public void shouldReturnFalseIfSoundCloudDoesNotExist(){
        when(accountManager.getAccountsByType(SC_ACCOUNT_TYPE)).thenReturn(null);
        expect(accountOperations.setAccountData(KEY, "ads")).toBeFalse();
    }

    @Test
    public void shouldStoreDataIfSoundCloudDoesExist(){
        when(accountManager.getAccountsByType(SC_ACCOUNT_TYPE)).thenReturn(new Account[]{scAccount});
        expect(accountOperations.setAccountData(KEY, "ads")).toBeTrue();
        verify(accountManager).setUserData(scAccount,KEY,"ads");
    }

    @Test
    public void shouldReturnNullTokenIfSoundCloudAccountDoesNotExist(){
        when(accountManager.getAccountsByType(anyString())).thenReturn(null);
        expect(accountOperations.getSoundCloudToken()).toBeNull();

    }

    @Test
    public void shouldReturnNullTokenIfSoundCloudAccountDoesExist(){
        when(accountManager.getAccountsByType(anyString())).thenReturn(new Account[]{scAccount});
        when(tokenOperations.getSoundCloudToken(scAccount)).thenReturn(token);
        expect(accountOperations.getSoundCloudToken()).toBe(token);

    }

    @Test(expected = IllegalStateException.class)
    public void shouldThrowExceptionIfSoundCloudDoesNotExistWhenStoringAccountData(){
        when(accountManager.getAccountsByType(anyString())).thenReturn(new Account[]{});
        accountOperations.storeSoundCloudTokenData(token);

    }

    @Test
    public void shouldStoreSoundCloudAccountDataIfAccountExists(){
        when(accountManager.getAccountsByType(anyString())).thenReturn(new Account[]{scAccount});
        accountOperations.storeSoundCloudTokenData(token);
        verify(tokenOperations).storeSoundCloudTokenData(scAccount,token);

    }

    @Test
    @Ignore("SCApplication needs more refactoring")
    public void shouldReturnObservableWithAccountRemovalFunction(){
        when(accountManager.getAccountsByType(anyString())).thenReturn(new Account[]{scAccount});
        accountOperations.removeSoundCloudAccount(observer);
        verify(observer).onCompleted();
    }
}
