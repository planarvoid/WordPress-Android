package com.soundcloud.android.accounts;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.facebook.login.LoginManager;
import com.soundcloud.android.BuildConfig;
import com.soundcloud.android.api.model.ApiUser;
import com.soundcloud.android.api.oauth.Token;
import com.soundcloud.android.configuration.ConfigurationOperations;
import com.soundcloud.android.events.CurrentUserChangedEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.offline.ClearTrackDownloadsCommand;
import com.soundcloud.android.onboarding.auth.SignupVia;
import com.soundcloud.android.playback.PlaySessionStateStorage;
import com.soundcloud.android.playback.PlaybackService;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.Assertions;
import com.soundcloud.android.testsupport.InjectionSupport;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.rx.eventbus.TestEventBus;
import com.tobedevoured.modelcitizen.CreateModelException;
import org.junit.Before;
import org.junit.Test;
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

@SuppressWarnings("MissingPermission")
public class AccountOperationsTest extends AndroidUnitTest {

    private static final String SC_ACCOUNT_TYPE = BuildConfig.APPLICATION_ID + ".account";
    private static final String KEY = "key";

    private AccountOperations accountOperations;

    private TestEventBus eventBus = new TestEventBus();

    @Mock private AccountManager accountManager;
    @Mock private SoundCloudTokenOperations tokenOperations;
    @Mock private Account scAccount;
    @Mock private Observer observer;
    @Mock private Token token;
    @Mock private ConfigurationOperations configurationOperations;
    @Mock private AccountCleanupAction accountCleanupAction;
    @Mock private ClearTrackDownloadsCommand clearTrackDownloadsCommand;
    @Mock private LoginManager facebookLoginManager;
    @Mock private PlaySessionStateStorage playSessionStateStorage;

    private ApiUser user;

    @Before
    public void setUp() throws CreateModelException {
        accountOperations = new AccountOperations(context(), accountManager, tokenOperations,
                                                  eventBus,
                                                  playSessionStateStorage,
                                                  InjectionSupport.lazyOf(configurationOperations),
                                                  InjectionSupport.lazyOf(accountCleanupAction),
                                                  InjectionSupport.lazyOf(clearTrackDownloadsCommand),
                                                  InjectionSupport.lazyOf(facebookLoginManager),
                                                  Schedulers.immediate());

        user = ModelFixtures.create(ApiUser.class);
    }

    @Test
    public void shouldReturnFalseIfAccountDoesNotExist() {
        assertThat(accountOperations.isUserLoggedIn()).isFalse();
    }

    @Test
    public void shouldReturnTrueIfAccountDoesExist() {
        when(accountManager.getAccountsByType(SC_ACCOUNT_TYPE)).thenReturn(new Account[]{scAccount});
        when(accountManager.getUserData(scAccount,
                                        AccountOperations.AccountInfoKeys.USER_ID.getKey())).thenReturn("123");
        assertThat(accountOperations.isUserLoggedIn()).isTrue();
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
        assertThat(account).isEqualTo(scAccount);
    }

    @Test
    public void shouldReturnNullIfAccountManagerReturnsNull() {
        when(accountManager.getAccountsByType(anyString())).thenReturn(null);
        Account account = accountOperations.getSoundCloudAccount();
        assertThat(account).isNull();
    }

    @Test
    public void shouldReturnNullIfAccountDoesNotExist() {
        when(accountManager.getAccountsByType(anyString())).thenReturn(new Account[]{});
        Account account = accountOperations.getSoundCloudAccount();
        assertThat(account).isNull();
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
        when(accountManager.addAccountExplicitly(any(Account.class), anyString(), any(Bundle.class))).thenReturn(false);
        assertThat(accountOperations.addOrReplaceSoundCloudAccount(user, token, SignupVia.API)).isNull();
    }

    @Test
    public void shouldReplaceExistingAccount() {
        Account old = new Account("oldUsername", SC_ACCOUNT_TYPE);
        when(accountManager.getAccountsByType(anyString())).thenReturn(new Account[]{old});
        when(accountManager.addAccountExplicitly(any(Account.class), isNull(), isNull())).thenReturn(true);

        final Account actual = accountOperations.addOrReplaceSoundCloudAccount(user, token, SignupVia.API);
        assertThat(actual).isInstanceOf(Account.class);
        verify(accountManager).removeAccount(old, null, null);
        verify(accountManager).addAccountExplicitly(any(Account.class), isNull(), isNull());
        verify(accountManager).setUserData(actual, "currentUsername", user.getUsername());
    }

    @Test
    public void shouldNotReplaceExistingAccountWithSameName() {
        Account old = new Account(user.getPermalink(), SC_ACCOUNT_TYPE);
        when(accountManager.getAccountsByType(anyString())).thenReturn(new Account[]{old});

        final Account actual = accountOperations.addOrReplaceSoundCloudAccount(user, token, SignupVia.API);
        assertThat(actual).isInstanceOf(Account.class);
        verify(accountManager, Mockito.never()).removeAccount(any(Account.class),
                                                              any(AccountManagerCallback.class),
                                                              any(Handler.class));
        verify(accountManager, Mockito.never()).addAccountExplicitly(any(Account.class),
                                                                     anyString(),
                                                                     any(Bundle.class));
        verify(accountManager).setUserData(actual, "currentUsername", user.getUsername());
    }

    @Test
    public void shouldSetUserDataIfAccountAdditionSucceeds() {
        Account account = new Account(user.getPermalink(), SC_ACCOUNT_TYPE);

        when(accountManager.addAccountExplicitly(account, null, null)).thenReturn(true);

        accountOperations.addOrReplaceSoundCloudAccount(user, token, SignupVia.API);
        verify(accountManager).setUserData(account, "currentUserId", String.valueOf(user.getId()));
        verify(accountManager).setUserData(account, "currentUsername", user.getUsername());
        verify(accountManager).setUserData(account, "currentUserPermalink", user.getPermalink());
        verify(accountManager).setUserData(account, "signup", SignupVia.API.getSignupIdentifier());
    }

    @Test
    public void shouldSetAuthTokenInformationIfAccountAdditionSucceeds() {
        Account account = new Account(user.getPermalink(), SC_ACCOUNT_TYPE);
        when(accountManager.addAccountExplicitly(account, null, null)).thenReturn(true);

        accountOperations.addOrReplaceSoundCloudAccount(user, token, SignupVia.API);

        verify(tokenOperations).storeSoundCloudTokenData(account, token);
    }

    @Test
    public void shouldPublishUserChangedEventIfAccountAdditionSucceeds() {
        Account account = new Account(user.getPermalink(), SC_ACCOUNT_TYPE);
        when(accountManager.addAccountExplicitly(account, null, null)).thenReturn(true);

        accountOperations.addOrReplaceSoundCloudAccount(user, token, SignupVia.API);

        final CurrentUserChangedEvent event = eventBus.lastEventOn(EventQueue.CURRENT_USER_CHANGED);
        assertThat(event.isUserUpdated()).isTrue();
    }

    @Test
    public void shouldReturnAddedAccountIfAccountAdditionSucceeds() {
        Account account = new Account(user.getPermalink(), SC_ACCOUNT_TYPE);
        when(accountManager.addAccountExplicitly(account, null, null)).thenReturn(true);

        assertThat(accountOperations.addOrReplaceSoundCloudAccount(user, token, SignupVia.API)).isEqualTo(account);
    }

    @Test
    public void shouldReturnFalseIfBooleanAccountDataDoesNotExist() {
        when(accountManager.getAccountsByType(SC_ACCOUNT_TYPE)).thenReturn(null);
        assertThat(accountOperations.getAccountDataBoolean(KEY)).isSameAs(false);
    }

    @Test
    public void shouldReturnTrueValueIfTrueBoolAccountDataDoesExist() {
        mockSoundCloudAccount();
        when(accountManager.getUserData(scAccount, KEY)).thenReturn("true");
        assertThat(accountOperations.getAccountDataBoolean(KEY)).isSameAs(true);
    }

    @Test
    public void shouldReturnFalseValueIfFalseBoolAccountDataDoesExist() {
        mockSoundCloudAccount();
        when(accountManager.getUserData(scAccount, KEY)).thenReturn("false");
        assertThat(accountOperations.getAccountDataBoolean(KEY)).isSameAs(false);
        verify(accountManager).getUserData(scAccount, KEY);
    }

    @Test
    public void shouldReturnFalseIfSoundCloudDoesNotExist() {
        when(accountManager.getAccountsByType(SC_ACCOUNT_TYPE)).thenReturn(null);
        assertThat(accountOperations.setAccountData(KEY, "ads")).isFalse();
    }

    @Test
    public void shouldStoreDataIfSoundCloudDoesExist() {
        mockSoundCloudAccount();
        assertThat(accountOperations.setAccountData(KEY, "ads")).isTrue();
        verify(accountManager).setUserData(scAccount, KEY, "ads");
    }

    @Test
    public void shouldReturnNullTokenIfSoundCloudAccountDoesNotExist() {
        when(accountManager.getAccountsByType(anyString())).thenReturn(null);
        assertThat(accountOperations.getSoundCloudToken()).isNull();
    }

    @Test
    public void shouldReturnNullTokenIfSoundCloudAccountDoesExist() {
        when(accountManager.getAccountsByType(anyString())).thenReturn(new Account[]{scAccount});
        when(tokenOperations.getTokenFromAccount(scAccount)).thenReturn(token);
        assertThat(accountOperations.getSoundCloudToken()).isSameAs(token);

    }

    @Test
    public void shouldStoreSoundCloudAccountDataIfAccountExists() {
        when(accountManager.getAccountsByType(anyString())).thenReturn(new Account[]{scAccount});
        accountOperations.storeSoundCloudTokenData(token);
        verify(tokenOperations).storeSoundCloudTokenData(scAccount, token);
    }

    @Test
    public void shouldReturnObservableWithAccountRemovalFunction() throws Exception {
        when(configurationOperations.deregisterDevice()).thenReturn(Observable.empty());
        when(accountManager.getAccountsByType(anyString())).thenReturn(new Account[]{scAccount});

        AccountManagerFuture future = mock(AccountManagerFuture.class);
        when(future.getResult()).thenReturn(Boolean.TRUE);
        when(accountManager.removeAccount(scAccount, null, null)).thenReturn(future);

        accountOperations.logout().subscribe(observer);
        verify(observer).onCompleted();
    }

    @Test
    public void shouldAttemptToDeregisterDeviceOnLogout() {
        when(configurationOperations.deregisterDevice()).thenReturn(Observable.just(RxUtils.EMPTY_VALUE));
        when(accountManager.getAccountsByType(anyString())).thenReturn(new Account[]{scAccount});

        accountOperations.logout().subscribe(observer);

        verify(configurationOperations).deregisterDevice();
    }

    @Test
    public void shouldGetLoggedInUserUrn() {
        mockSoundCloudAccount();

        assertThat(accountOperations.getLoggedInUserUrn()).isEqualTo(Urn.forUser(123L));
    }

    @Test
    public void shouldClearUserUrn() {
        mockSoundCloudAccount();

        accountOperations.clearLoggedInUser();

        assertThat(accountOperations.getLoggedInUserUrn()).isEqualTo(AccountOperations.ANONYMOUS_USER_URN);
    }

    @Test
    public void shouldReturnTrueIfGivenUserIsLoggedInUser() {
        mockSoundCloudAccount();
        assertThat(accountOperations.isLoggedInUser(Urn.forUser(123))).isTrue();
    }

    @Test
    public void shouldReturnFalseIfGivenUserIsNotTheLoggedInUser() {
        mockSoundCloudAccount();
        assertThat(accountOperations.isLoggedInUser(Urn.forUser(1))).isFalse();
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
    public void purgingUserDataShouldResetOAuthToken() {
        mockSoundCloudAccount();
        mockValidToken();

        accountOperations.purgeUserData().subscribe(observer);

        verify(tokenOperations).resetToken();
    }

    @Test
    public void purgingUserDataShouldClearPlaySessionState() {
        mockSoundCloudAccount();
        mockValidToken();

        accountOperations.purgeUserData().subscribe(observer);

        verify(playSessionStateStorage).clear();
    }

    @Test
    public void purgingUserDataShouldLogOutFacebook() {
        accountOperations.purgeUserData().subscribe(observer);

        InOrder inOrder = inOrder(observer, facebookLoginManager);
        inOrder.verify(facebookLoginManager).logOut();
        inOrder.verify(observer).onCompleted();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    public void shouldPublishUserRemovalIfPurgingUserDataSucceeds() {
        accountOperations.purgeUserData().subscribe(observer);

        final CurrentUserChangedEvent event = eventBus.lastEventOn(EventQueue.CURRENT_USER_CHANGED);
        assertThat(event.isUserRemoved()).isTrue();
    }

    @Test
    public void shouldBroadcastResetAllIntentIfAccountRemovalSucceeds() {
        accountOperations.purgeUserData().subscribe(observer);

        Intent nextService = getNextStartedService();

        Assertions.assertThat(nextService).containsAction(PlaybackService.Action.RESET_ALL);
    }

    @Test
    public void purgeUserDataShouldRemoveOfflineContent() {
        accountOperations.purgeUserData().subscribe(observer);

        verify(clearTrackDownloadsCommand).call(null);
    }

    private void mockSoundCloudAccount() {
        when(accountManager.getAccountsByType(SC_ACCOUNT_TYPE)).thenReturn(new Account[]{scAccount});
        when(accountManager.getUserData(scAccount,
                                        AccountOperations.AccountInfoKeys.USER_ID.getKey())).thenReturn("123");
        when(accountManager.getUserData(scAccount, AccountOperations.AccountInfoKeys.USERNAME.getKey())).thenReturn(
                "username");
        when(accountManager.getUserData(scAccount,
                                        AccountOperations.AccountInfoKeys.USER_PERMALINK.getKey())).thenReturn(
                "permalink");
    }

    private void mockValidToken() {
        when(tokenOperations.getTokenFromAccount(scAccount)).thenReturn(new Token("123", "456"));
    }

}
