package com.soundcloud.android.accounts;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.Consts;
import com.soundcloud.android.api.legacy.model.PublicApiResource;
import com.soundcloud.android.api.legacy.model.PublicApiUser;
import com.soundcloud.android.api.legacy.model.ScModelManager;
import com.soundcloud.android.events.CurrentUserChangedEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.onboarding.auth.SignupVia;
import com.soundcloud.android.playback.service.PlaybackService;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.soundcloud.android.rx.eventbus.TestEventBus;
import com.soundcloud.android.storage.UserStorage;
import com.soundcloud.api.Token;
import com.xtremelabs.robolectric.Robolectric;
import dagger.Lazy;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import rx.Observable;
import rx.Observer;
import rx.schedulers.Schedulers;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import java.util.List;

@RunWith(SoundCloudTestRunner.class)
public class AccountOperationsTest {
    private static final String SC_ACCOUNT_TYPE = "com.soundcloud.android.account";
    private static final String KEY = "key";

    private AccountOperations accountOperations;

    private TestEventBus eventBus = new TestEventBus();

    @Mock
    private AccountManager accountManager;
    @Mock
    private SoundCloudTokenOperations tokenOperations;
    @Mock
    private ScModelManager modelManager;
    @Mock
    private UserStorage userStorage;
    @Mock
    private Account scAccount;
    @Mock
    private Observer observer;
    @Mock
    private PublicApiUser user;
    @Mock
    private Token token;
    @Mock
    private AccountCleanupAction accountCleanupAction;

    @Before
    public void setUp() {
        accountOperations = new AccountOperations(Robolectric.application, accountManager, tokenOperations,
                modelManager, userStorage, eventBus, new Lazy<AccountCleanupAction>() {
            @Override
            public AccountCleanupAction get() {
                return accountCleanupAction;
            }
        }, Schedulers.immediate());
    }

    @Test
    public void shouldReturnFalseIfAccountDoesNotExist() {
        expect(accountOperations.isUserLoggedIn()).toBeFalse();
    }

    @Test
    public void shouldReturnTrueIfAccountDoesExist() {
        when(accountManager.getAccountsByType(SC_ACCOUNT_TYPE)).thenReturn(new Account[]{scAccount});
        when(accountManager.getUserData(scAccount, AccountOperations.AccountInfoKeys.USER_ID.getKey())).thenReturn("123");
        expect(accountOperations.isUserLoggedIn()).toBeTrue();
    }

    @Test
    public void shouldCheckForExistenceOfSoundCloudAccount() {
        accountOperations.isUserLoggedIn();
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
        accountOperations.logout();
    }

    @Test
    public void shouldAddAccountUsingAccountManager() {
        Activity activity = mock(Activity.class);
        accountOperations.triggerLoginFlow(activity);
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
    public void shouldSetLoggedInUserToNewUserIfAccountAdditionSucceeds() {
        Account account = new Account("username", SC_ACCOUNT_TYPE);
        when(user.getUsername()).thenReturn("username");
        when(modelManager.cache(user, PublicApiResource.CacheUpdateMode.FULL)).thenReturn(user);
        when(accountManager.addAccountExplicitly(account, null, null)).thenReturn(true);

        accountOperations.addOrReplaceSoundCloudAccount(user, token, SignupVia.API);

        expect(accountOperations.getLoggedInUser()).toBe(user);
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
    public void shouldPublishUserChangedEventIfAccountAdditionSucceeds() {
        Account account = new Account("username", SC_ACCOUNT_TYPE);
        when(user.getUsername()).thenReturn("username");
        when(accountManager.addAccountExplicitly(account, null, null)).thenReturn(true);

        accountOperations.addOrReplaceSoundCloudAccount(user, token, SignupVia.API);

        final CurrentUserChangedEvent event = eventBus.lastEventOn(EventQueue.CURRENT_USER_CHANGED);
        expect(event.getKind()).toBe(CurrentUserChangedEvent.USER_UPDATED);
    }

    @Test
    public void shouldReturnAddedAccountIfAccountAdditionSucceeds() {
        Account account = new Account("username", SC_ACCOUNT_TYPE);

        when(user.getUsername()).thenReturn("username");
        when(accountManager.addAccountExplicitly(account, null, null)).thenReturn(true);

        expect(accountOperations.addOrReplaceSoundCloudAccount(user, token, SignupVia.API)).toEqual(account);

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

        accountOperations.logout().subscribe(observer);
        verify(observer).onCompleted();
    }

    @Test
    public void shouldCheckForConfirmedEmailAddressWhenTimeToRemindAndEmailNotConfirmedYet() {
        mockSoundCloudAccount();
        mockValidToken();
        mockExpiredEmailConfirmationReminder();

        PublicApiUser currentUser = new PublicApiUser(123L);
        currentUser.setPrimaryEmailConfirmed(false);

        expect(accountOperations.shouldCheckForConfirmedEmailAddress(currentUser)).toBeTrue();
    }

    @Test
    public void shouldNotCheckForConfirmedEmailAddressIfAlreadyConfirmed() {
        mockSoundCloudAccount();
        mockValidToken();
        mockExpiredEmailConfirmationReminder();

        PublicApiUser currentUser = new PublicApiUser(123L);
        currentUser.setPrimaryEmailConfirmed(true);

        expect(accountOperations.shouldCheckForConfirmedEmailAddress(currentUser)).toBeFalse();
    }

    @Test
    public void shouldNotCheckForConfirmedEmailAddressIfAlreadyRemindedRecently() {
        mockSoundCloudAccount();
        mockValidToken();
        mockRecentEmailConfirmationReminder();

        PublicApiUser currentUser = new PublicApiUser(123L);
        currentUser.setPrimaryEmailConfirmed(false);

        expect(accountOperations.shouldCheckForConfirmedEmailAddress(currentUser)).toBeFalse();
    }

    @Test
    public void shouldNotCheckForConfirmedEmailAddressIfTokenIsInvalid() {
        mockSoundCloudAccount();
        mockInvalidToken();
        mockExpiredEmailConfirmationReminder();

        PublicApiUser currentUser = new PublicApiUser(123L);
        currentUser.setPrimaryEmailConfirmed(false);

        expect(accountOperations.shouldCheckForConfirmedEmailAddress(currentUser)).toBeFalse();
    }

    @Test
    public void shouldReturnDummyUserWithMinimumAccountInfoIfNotYetLoaded() {
        mockSoundCloudAccount();

        final PublicApiUser loggedInUser = accountOperations.getLoggedInUser();

        expect(loggedInUser.getId()).toEqual(123L);
        expect(loggedInUser.getUsername()).toEqual("username");
        expect(loggedInUser.getPermalink()).toEqual("permalink");
    }

    @Test
    public void shouldLoadUserFromLocalStorageBasedOnAccountIdAndUpdateLoggedInUser() {
        mockSoundCloudAccount();
        when(userStorage.getUserAsync(123L)).thenReturn(Observable.just(user));
        when(modelManager.cache(user, PublicApiResource.CacheUpdateMode.FULL)).thenReturn(user);

        accountOperations.loadLoggedInUser();

        expect(accountOperations.getLoggedInUser()).toBe(user);
    }

    @Test
    public void shouldNotLoadUserFromLocalStorageIfAccountIdIsNotSet() {
        when(userStorage.getUserAsync(123L)).thenReturn(Observable.just(user));
        when(modelManager.cache(user, PublicApiResource.CacheUpdateMode.FULL)).thenReturn(user);

        accountOperations.loadLoggedInUser();

        verifyZeroInteractions(userStorage);

        expect(accountOperations.getLoggedInUser()).not.toBe(user);
    }

    @Test
    public void shouldGetLoggedInUserUrn() {
        mockSoundCloudAccount();

        expect(accountOperations.getLoggedInUserUrn()).toEqual(Urn.forUser(123L));
    }

    @Test
    public void shouldReturnTrueIfGivenUserIsLoggedInUser() {
        mockSoundCloudAccount();
        expect(accountOperations.isLoggedInUser(Urn.forUser(123))).toBeTrue();
    }

    @Test
    public void shouldReturnFalseIfGivenUserIsNotTheLoggedInUser() {
        mockSoundCloudAccount();
        expect(accountOperations.isLoggedInUser(Urn.forUser(1))).toBeFalse();
    }

    @Test
    public void purgingUserDataShouldCallAccountCleanupAction() {
        accountOperations.purgeUserData().subscribe(observer);
        InOrder inOrder = inOrder(observer, accountCleanupAction);
        inOrder.verify(accountCleanupAction).call();
        inOrder.verify(observer).onCompleted();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void purgingUserDataShouldClearLoggedInUser() {
        mockSoundCloudAccount();
        when(userStorage.getUserAsync(123L)).thenReturn(Observable.just(user));
        when(modelManager.cache(user, PublicApiResource.CacheUpdateMode.FULL)).thenReturn(user);
        accountOperations.loadLoggedInUser();
        expect(accountOperations.getLoggedInUser()).toBe(user);

        accountOperations.purgeUserData().subscribe(observer);

        expect(accountOperations.getLoggedInUser()).not.toBe(user);
    }


    @Test
    public void shouldPublishUserRemovalIfPurgingUserDataSucceeds() {
        accountOperations.purgeUserData().subscribe(observer);

        final CurrentUserChangedEvent event = eventBus.lastEventOn(EventQueue.CURRENT_USER_CHANGED);
        expect(event.getKind()).toBe(CurrentUserChangedEvent.USER_REMOVED);
    }

    @Test
    public void shouldBroadcastResetAllIntentIfAccountRemovalSucceeds() {
        accountOperations.purgeUserData().subscribe(observer);

        final List<Intent> intents = Robolectric.getShadowApplication().getBroadcastIntents();
        expect(intents).not.toBeEmpty();
        expect(intents.get(0).getAction()).toEqual(PlaybackService.Actions.RESET_ALL);
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
        when(accountManager.getUserData(scAccount, AccountOperations.AccountInfoKeys.USER_ID.getKey())).thenReturn("123");
        when(accountManager.getUserData(scAccount, AccountOperations.AccountInfoKeys.USERNAME.getKey())).thenReturn("username");
        when(accountManager.getUserData(scAccount, AccountOperations.AccountInfoKeys.USER_PERMALINK.getKey())).thenReturn("permalink");
    }

    private void mockValidToken() {
        when(tokenOperations.getSoundCloudToken(scAccount)).thenReturn(new Token("123", "456"));
    }

    private void mockInvalidToken() {
        when(tokenOperations.getSoundCloudToken(scAccount)).thenReturn(null);
    }

}
