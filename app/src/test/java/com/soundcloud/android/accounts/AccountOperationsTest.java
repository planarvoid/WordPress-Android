package com.soundcloud.android.accounts;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.soundcloud.android.Consts;
import com.soundcloud.android.onboarding.auth.SignupVia;
import com.soundcloud.android.model.User;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.api.Token;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import rx.Observer;
import rx.schedulers.Schedulers;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;

@RunWith(SoundCloudTestRunner.class)
public class AccountOperationsTest {
    private static final String SC_ACCOUNT_TYPE = "com.soundcloud.android.account";
    private static final String KEY = "key";

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
        accountOperations = new AccountOperations(
                accountManager, Robolectric.application, tokenOperations, Schedulers.immediate());
    }

    @Test
    public void shouldReturnFalseIfAccountDoesNotExist() {
        when(accountManager.getAccountsByType(anyString())).thenReturn(new Account[]{});
        expect(accountOperations.soundCloudAccountExists()).toBeFalse();
    }

    @Test
    public void shouldReturnTrueIfAccountDoesExist() {
        when(accountManager.getAccountsByType(anyString())).thenReturn(new Account[]{scAccount});
        expect(accountOperations.soundCloudAccountExists()).toBeTrue();
    }

    @Test
    public void shouldCheckForExistenceOfSoundCloudAccount() {
        accountOperations.soundCloudAccountExists();
        verify(accountManager).getAccountsByType(SC_ACCOUNT_TYPE);
    }

    @Test
    public void shouldReturnAccountIfItExists() {
        when(accountManager.getAccountsByType(anyString())).thenReturn(new Account[]{scAccount});
        Account account = accountOperations.getSoundCloudAccount();
        expect(account).toEqual(scAccount);
    }

    @Test
    public void shouldReturnNullIfAccountManagerReturnsNull() {
        when(accountManager.getAccountsByType(anyString())).thenReturn(null);
        Account account = accountOperations.getSoundCloudAccount();
        expect(account).toBeNull();
    }

    @Test
    public void shouldReturnNullIfAccountDoesNotExist() {
        when(accountManager.getAccountsByType(anyString())).thenReturn(new Account[]{});
        Account account = accountOperations.getSoundCloudAccount();
        expect(account).toBeNull();
    }

    @Test(expected = NullPointerException.class)
    public void shouldThrowExceptionWhenRemovingAccountIfAccountDoesNotExist() {
        when(accountManager.getAccountsByType(anyString())).thenReturn(null);
        accountOperations.removeSoundCloudAccount();
    }

    @Test
    public void shouldAddAccountUsingAccountManager() {
        Activity activity = mock(Activity.class);
        accountOperations.addSoundCloudAccountManually(activity);
        verify(accountManager).addAccount(SC_ACCOUNT_TYPE, "access_token", null, null, activity, null, null);
    }

    @Test
    public void shouldReturnNullIfAccountAdditionFails() {
        when(user.getUsername()).thenReturn("username");
        when(accountManager.addAccountExplicitly(any(Account.class), anyString(), any(Bundle.class))).thenReturn(false);
        expect(accountOperations.addOrReplaceSoundCloudAccount(user, token, SignupVia.API)).toBeNull();

    }

    @Test
    public void shouldReplaceExistingAccount() {
        Account old = new Account("oldUsername", SC_ACCOUNT_TYPE);
        when(accountManager.getAccountsByType(anyString())).thenReturn(new Account[]{old});
        when(user.getUsername()).thenReturn("username");
        when(accountManager.addAccountExplicitly(any(Account.class), anyString(), any(Bundle.class))).thenReturn(true);

        final Account actual = accountOperations.addOrReplaceSoundCloudAccount(user, token, SignupVia.API);
        expect(actual).toBeInstanceOf(Account.class);
        verify(accountManager).removeAccount(old, null, null);
        verify(accountManager).addAccountExplicitly(any(Account.class), anyString(), any(Bundle.class));
        verify(accountManager).setUserData(actual, "currentUsername", "username");
    }

    @Test
    public void shouldNotReplaceExistingAccountWithSameName() {
        Account old = new Account("username", SC_ACCOUNT_TYPE);
        when(accountManager.getAccountsByType(anyString())).thenReturn(new Account[]{old});
        when(user.getUsername()).thenReturn("username");

        final Account actual = accountOperations.addOrReplaceSoundCloudAccount(user, token, SignupVia.API);
        expect(actual).toBeInstanceOf(Account.class);
        verify(accountManager, Mockito.never()).removeAccount(any(Account.class), any(AccountManagerCallback.class), any(Handler.class));
        verify(accountManager, Mockito.never()).addAccountExplicitly(any(Account.class), anyString(), any(Bundle.class));
        verify(accountManager).setUserData(actual, "currentUsername", "username");
    }

    @Test
    public void shouldSetUserDataIfAccountAdditionSucceeds() {
        Account account = new Account("username", SC_ACCOUNT_TYPE);

        when(accountManager.addAccountExplicitly(account, null, null)).thenReturn(true);
        when(user.getId()).thenReturn(2L);
        when(user.getUsername()).thenReturn("username");
        when(user.getPermalink()).thenReturn("permalink");

        accountOperations.addOrReplaceSoundCloudAccount(user, token, SignupVia.API);
        verify(accountManager).setUserData(account, "currentUserId", "2");
        verify(accountManager).setUserData(account, "currentUsername", "username");
        verify(accountManager).setUserData(account, "currentUserPermalink", "permalink");
        verify(accountManager).setUserData(account, "signup", SignupVia.API.getSignupIdentifier());
    }

    @Test
    public void shouldSetAuthTokenInformationIfAccountAdditionSucceeds() {
        Account account = new Account("username", SC_ACCOUNT_TYPE);

        when(user.getUsername()).thenReturn("username");
        when(accountManager.addAccountExplicitly(account, null, null)).thenReturn(true);

        accountOperations.addOrReplaceSoundCloudAccount(user, token, SignupVia.API);

        verify(tokenOperations).storeSoundCloudTokenData(account, token);
    }

    @Test
    public void shouldReturnAddedAccountIfAccountAdditionSucceeds() {
        Account account = new Account("username", SC_ACCOUNT_TYPE);

        when(user.getUsername()).thenReturn("username");
        when(accountManager.addAccountExplicitly(account, null, null)).thenReturn(true);

        expect(accountOperations.addOrReplaceSoundCloudAccount(user, token, SignupVia.API)).toEqual(account);

    }

    @Test
    public void shouldReturnNullStringDataIfAccountDoesNotExist() {
        when(accountManager.getAccountsByType(anyString())).thenReturn(null);
        String data = accountOperations.getAccountDataString(KEY);
        expect(data).toBeNull();
        verify(accountManager, never()).getUserData(any(Account.class), any(String.class));
    }

    @Test
    public void shouldReturnStringAccountDataIfAccountExists() {
        when(accountManager.getAccountsByType(anyString())).thenReturn(new Account[]{scAccount});
        when(accountManager.getUserData(scAccount, KEY)).thenReturn("data");
        String data = accountOperations.getAccountDataString(KEY);
        expect(data).toBe("data");
    }

    @Test
    public void shouldReturnNegativeOneIfLongAccountDataDoesNotExist() {
        when(accountManager.getAccountsByType(SC_ACCOUNT_TYPE)).thenReturn(null);
        expect(accountOperations.getAccountDataLong(KEY)).toBe(-1L);
    }

    @Test
    public void shouldReturnExpectedValueIfLongAccountDataDoesExist() {
        mockSoundCloudAccount();
        when(accountManager.getUserData(scAccount, KEY)).thenReturn("23");
        expect(accountOperations.getAccountDataLong(KEY)).toBe(23L);
    }

    @Test
    public void shouldReturnFalseIfBooleanAccountDataDoesNotExist() {
        when(accountManager.getAccountsByType(SC_ACCOUNT_TYPE)).thenReturn(null);
        expect(accountOperations.getAccountDataBoolean(KEY)).toBe(false);
    }

    @Test
    public void shouldReturnTrueValueIfTrueBoolAccountDataDoesExist() {
        mockSoundCloudAccount();
        when(accountManager.getUserData(scAccount, KEY)).thenReturn("true");
        expect(accountOperations.getAccountDataBoolean(KEY)).toBe(true);
    }

    @Test
    public void shouldReturnFalseValueIfFalseBoolAccountDataDoesExist() {
        mockSoundCloudAccount();
        when(accountManager.getUserData(scAccount, KEY)).thenReturn("false");
        expect(accountOperations.getAccountDataBoolean(KEY)).toBe(false);
        verify(accountManager).getUserData(scAccount, KEY);
    }

    @Test
    public void shouldReturnFalseIfSoundCloudDoesNotExist() {
        when(accountManager.getAccountsByType(SC_ACCOUNT_TYPE)).thenReturn(null);
        expect(accountOperations.setAccountData(KEY, "ads")).toBeFalse();
    }

    @Test
    public void shouldStoreDataIfSoundCloudDoesExist() {
        mockSoundCloudAccount();
        expect(accountOperations.setAccountData(KEY, "ads")).toBeTrue();
        verify(accountManager).setUserData(scAccount, KEY, "ads");
    }

    @Test
    public void shouldReturnNullTokenIfSoundCloudAccountDoesNotExist() {
        when(accountManager.getAccountsByType(anyString())).thenReturn(null);
        expect(accountOperations.getSoundCloudToken()).toBeNull();

    }

    @Test
    public void shouldReturnNullTokenIfSoundCloudAccountDoesExist() {
        when(accountManager.getAccountsByType(anyString())).thenReturn(new Account[]{scAccount});
        when(tokenOperations.getSoundCloudToken(scAccount)).thenReturn(token);
        expect(accountOperations.getSoundCloudToken()).toBe(token);

    }

    @Test(expected = IllegalStateException.class)
    public void shouldThrowExceptionIfSoundCloudDoesNotExistWhenStoringAccountData() {
        when(accountManager.getAccountsByType(anyString())).thenReturn(new Account[]{});
        accountOperations.storeSoundCloudTokenData(token);

    }

    @Test
    public void shouldStoreSoundCloudAccountDataIfAccountExists() {
        when(accountManager.getAccountsByType(anyString())).thenReturn(new Account[]{scAccount});
        accountOperations.storeSoundCloudTokenData(token);
        verify(tokenOperations).storeSoundCloudTokenData(scAccount, token);

    }

    @Test
    public void shouldReturnObservableWithAccountRemovalFunction() throws Exception {
        when(accountManager.getAccountsByType(anyString())).thenReturn(new Account[]{scAccount});

        AccountManagerFuture future = mock(AccountManagerFuture.class);
        when(future.getResult()).thenReturn(Boolean.TRUE);
        when(accountManager.removeAccount(scAccount, null, null)).thenReturn(future);

        accountOperations.removeSoundCloudAccount().subscribe(observer);
        verify(observer).onCompleted();
    }

    @Test
    public void shouldCheckForConfirmedEmailAddressWhenTimeToRemindAndEmailNotConfirmedYet() {
        mockSoundCloudAccount();
        mockValidToken();
        mockExpiredEmailConfirmationReminder();

        User currentUser = new User(123L);
        currentUser.setPrimaryEmailConfirmed(false);

        expect(accountOperations.shouldCheckForConfirmedEmailAddress(currentUser)).toBeTrue();
    }

    @Test
    public void shouldNotCheckForConfirmedEmailAddressIfAlreadyConfirmed() {
        mockSoundCloudAccount();
        mockValidToken();
        mockExpiredEmailConfirmationReminder();

        User currentUser = new User(123L);
        currentUser.setPrimaryEmailConfirmed(true);

        expect(accountOperations.shouldCheckForConfirmedEmailAddress(currentUser)).toBeFalse();
    }

    @Test
    public void shouldNotCheckForConfirmedEmailAddressIfAlreadyRemindedRecently() {
        mockSoundCloudAccount();
        mockValidToken();
        mockRecentEmailConfirmationReminder();

        User currentUser = new User(123L);
        currentUser.setPrimaryEmailConfirmed(false);

        expect(accountOperations.shouldCheckForConfirmedEmailAddress(currentUser)).toBeFalse();
    }

    @Test
    public void shouldNotCheckForConfirmedEmailAddressIfTokenIsInvalid() {
        mockSoundCloudAccount();
        mockInvalidToken();
        mockExpiredEmailConfirmationReminder();

        User currentUser = new User(123L);
        currentUser.setPrimaryEmailConfirmed(false);

        expect(accountOperations.shouldCheckForConfirmedEmailAddress(currentUser)).toBeFalse();
    }

    @Test
    public void shouldResolveContextToApplicationContextToPreventMemoryLeaks() {
        Activity activity = mock(Activity.class);
        new AccountOperations(activity);
        verify(activity, atLeastOnce()).getApplicationContext();
    }

    private void mockExpiredEmailConfirmationReminder() {
        // last reminder was longer ago than the reminder priod
        when(accountManager.getUserData(scAccount, Consts.PrefKeys.LAST_EMAIL_CONFIRMATION_REMINDER)).thenReturn(
                Long.toString(System.currentTimeMillis() - AccountOperations.EMAIL_CONFIRMATION_REMIND_PERIOD - 1));
    }

    private void mockRecentEmailConfirmationReminder() {
        // last reminder just happened
        when(accountManager.getUserData(scAccount, Consts.PrefKeys.LAST_EMAIL_CONFIRMATION_REMINDER)).thenReturn(
                Long.toString(System.currentTimeMillis() - 1));
    }

    private void mockSoundCloudAccount() {
        when(accountManager.getAccountsByType(SC_ACCOUNT_TYPE)).thenReturn(new Account[]{scAccount});
    }

    private void mockValidToken() {
        when(tokenOperations.getSoundCloudToken(scAccount)).thenReturn(new Token("123", "456"));
    }

    private void mockInvalidToken() {
        when(tokenOperations.getSoundCloudToken(scAccount)).thenReturn(null);
    }

}
