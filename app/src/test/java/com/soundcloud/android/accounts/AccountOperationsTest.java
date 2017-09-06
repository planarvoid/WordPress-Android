package com.soundcloud.android.accounts;

import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Matchers.any;
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
import com.soundcloud.android.offline.ClearOfflineContentCommand;
import com.soundcloud.android.onboarding.auth.SignupVia;
import com.soundcloud.android.playback.PlaySessionStateStorage;
import com.soundcloud.android.playback.PlaybackService;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.Assertions;
import com.soundcloud.android.testsupport.InjectionSupport;
import com.soundcloud.android.testsupport.fixtures.ModelFixtures;
import com.soundcloud.android.utils.GooglePlayServicesWrapper;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.rx.eventbus.TestEventBusV2;
import com.tobedevoured.modelcitizen.CreateModelException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observable;

import android.accounts.Account;
import android.accounts.AccountManagerFuture;
import android.app.Activity;
import android.content.Intent;

@SuppressWarnings("MissingPermission")
public class AccountOperationsTest extends AndroidUnitTest {

    private final String SC_ACCOUNT_TYPE = BuildConfig.APPLICATION_ID + ".account";
    private final Account ACCOUNT = new Account("account", SC_ACCOUNT_TYPE);

    private AccountOperations accountOperations;

    private TestEventBusV2 eventBus = new TestEventBusV2();

    @Mock private ScAccountManager accountManager;
    @Mock private SoundCloudTokenOperations tokenOperations;
    @Mock private Token token;
    @Mock private ConfigurationOperations configurationOperations;
    @Mock private AccountCleanupAction accountCleanupAction;
    @Mock private ClearOfflineContentCommand clearOfflineContentCommand;
    @Mock private LoginManager facebookLoginManager;
    @Mock private PlaySessionStateStorage playSessionStateStorage;
    @Mock private GooglePlayServicesWrapper googlePlayServicesWrapper;

    private ApiUser user;
    private SessionProvider sessionProvider;

    @Before
    public void setUp() throws CreateModelException {
        sessionProvider = new SessionProvider(accountManager, io.reactivex.schedulers.Schedulers.trampoline());
        accountOperations = new AccountOperations(context(), accountManager, tokenOperations,
                                                  eventBus,
                                                  playSessionStateStorage,
                                                  InjectionSupport.lazyOf(configurationOperations),
                                                  InjectionSupport.lazyOf(accountCleanupAction),
                                                  InjectionSupport.lazyOf(clearOfflineContentCommand),
                                                  InjectionSupport.lazyOf(facebookLoginManager),
                                                  googlePlayServicesWrapper,
                                                  io.reactivex.schedulers.Schedulers.trampoline(),
                                                  sessionProvider);

        user = ModelFixtures.create(ApiUser.class);
    }

    @Test
    public void shouldReturnFalseIfAccountDoesNotExist() {
        mockNoAccount();

        assertThat(accountOperations.isUserLoggedIn()).isFalse();
    }

    @Test
    public void shouldReturnTrueIfAccountDoesExist() {
        mockValidAccount();

        assertThat(accountOperations.isUserLoggedIn()).isTrue();
    }

    @Test
    public void shouldSayNormalAccountIsLoggedIn() {
        mockValidAccount();

        accountOperations.isUserLoggedIn();

        assertThat(accountOperations.isUserLoggedIn()).isTrue();
    }

    @Test
    public void shouldSayAbsentAccountNotLoggedIn() {
        mockNoAccount();

        accountOperations.isUserLoggedIn();

        assertThat(accountOperations.isUserLoggedIn()).isFalse();
    }

    @Test
    public void shouldReturnAccountIfItExists() {
        mockValidAccount();

        assertThat(accountOperations.getSoundCloudAccount()).isEqualTo(Optional.of(ACCOUNT));
    }

    @Test(expected = IllegalStateException.class)
    public void shouldThrowExceptionWhenRemovingAccountIfAccountDoesNotExist() {
        mockNoAccount();

        accountOperations.logout();
    }

    @Test
    public void shouldAddAccountUsingAccountManager() {
        mockNoAccount();

        Activity activity = mock(Activity.class);
        accountOperations.triggerLoginFlow(activity);
        verify(accountManager).addAccount("access_token", activity);
    }

    @Test
    public void shouldReturnNullIfAccountAdditionFails() {
        mockNoAccount();

        when(accountManager.addOrReplaceSoundCloudAccount(any(Urn.class), anyString(), any(Token.class), any(SignupVia.class))).thenReturn(Optional.absent());
        assertThat(accountOperations.addOrReplaceSoundCloudAccount(user, token, SignupVia.API)).isNull();
    }

    @Test
    public void shouldSetAuthTokenInformationIfAccountAdditionSucceeds() {
        mockNoAccount();

        Account account = new Account(user.getPermalink(), SC_ACCOUNT_TYPE);
        when(accountManager.addOrReplaceSoundCloudAccount(user.getUrn(), user.getPermalink(), token, SignupVia.API)).thenReturn(Optional.of(account));

        accountOperations.addOrReplaceSoundCloudAccount(user, token, SignupVia.API);

        verify(tokenOperations).storeSoundCloudTokenData(account, token);
    }

    @Test
    public void shouldPublishUserChangedEventIfAccountAdditionSucceeds() {
        mockNoAccount();

        Account account = new Account(user.getPermalink(), SC_ACCOUNT_TYPE);
        when(accountManager.addOrReplaceSoundCloudAccount(user.getUrn(), user.getPermalink(), token, SignupVia.API)).thenReturn(Optional.of(account));

        accountOperations.addOrReplaceSoundCloudAccount(user, token, SignupVia.API);

        final CurrentUserChangedEvent event = eventBus.lastEventOn(EventQueue.CURRENT_USER_CHANGED);
        assertThat(event.isUserUpdated()).isTrue();
    }

    @Test
    public void shouldReturnAddedAccountIfAccountAdditionSucceeds() {
        mockNoAccount();

        Account account = new Account(user.getPermalink(), SC_ACCOUNT_TYPE);
        when(accountManager.addOrReplaceSoundCloudAccount(user.getUrn(), user.getPermalink(), token, SignupVia.API)).thenReturn(Optional.of(account));

        assertThat(accountOperations.addOrReplaceSoundCloudAccount(user, token, SignupVia.API)).isEqualTo(account);
    }

    @Test
    public void shouldReturnNullTokenIfSoundCloudAccountDoesNotExist() {
        mockNoAccount();

        when(accountManager.getSoundCloudAccount()).thenReturn(Optional.absent());
        assertThat(accountOperations.getSoundCloudToken()).isNull();
    }

    @Test
    public void shouldReturnNullTokenIfSoundCloudAccountDoesExist() {
        mockValidAccount();

        when(accountManager.getSoundCloudAccount()).thenReturn(Optional.of(ACCOUNT));
        when(tokenOperations.getTokenFromAccount(ACCOUNT)).thenReturn(token);

        assertThat(accountOperations.getSoundCloudToken()).isSameAs(token);

    }

    @Test
    public void logoutShouldRemoveAccount() throws Exception {
        when(configurationOperations.deregisterDevice()).thenReturn(Observable.just(RxUtils.EMPTY_VALUE));
        when(accountManager.getSoundCloudAccount()).thenReturn(Optional.of(ACCOUNT));

        accountOperations.logout().test().assertComplete();

        verify(accountManager).remove(ACCOUNT);
    }

    @Test
    public void shouldAttemptToDeregisterDeviceOnLogout() {
        when(configurationOperations.deregisterDevice()).thenReturn(Observable.just(RxUtils.EMPTY_VALUE));
        when(accountManager.getSoundCloudAccount()).thenReturn(Optional.of(ACCOUNT));

        accountOperations.logout().test();

        verify(configurationOperations).deregisterDevice();
    }

    @Test
    public void shouldGetLoggedInUserUrn() {
        mockValidAccount();

        assertThat(accountOperations.getLoggedInUserUrn()).isEqualTo(Urn.forUser(123L));
    }

    @Test
    public void shouldClearUserUrn() {
        mockValidAccount();

        accountOperations.clearLoggedInUser();

        assertThat(accountOperations.isUserLoggedIn()).isFalse();
    }

    @Test
    public void shouldReturnTrueIfGivenUserIsLoggedInUser() {
        mockValidAccount();

        assertThat(accountOperations.isLoggedInUser(Urn.forUser(123))).isTrue();
    }

    @Test
    public void shouldReturnFalseIfGivenUserIsNotTheLoggedInUser() {
        mockValidAccount();

        assertThat(accountOperations.isLoggedInUser(Urn.forUser(1))).isFalse();
    }

    @Test
    public void purgingUserDataShouldCallAccountCleanupAction() {
        mockValidAccount();

        accountOperations.purgeUserData().test();

        verify(accountCleanupAction).call();
    }

    @Test
    public void purgingUserDataShouldResetOAuthToken() {
        mockValidAccount();

        accountOperations.purgeUserData().test();

        verify(tokenOperations).resetToken();
    }

    @Test
    public void purgingUserDataShouldClearPlaySessionState() {
        mockValidAccount();

        accountOperations.purgeUserData().test();

        verify(playSessionStateStorage).clear();
    }

    @Test
    public void purgingUserDataShouldLogOutFacebook() {
        mockValidAccount();

        accountOperations.purgeUserData().test();

        verify(facebookLoginManager).logOut();
    }

    @Test
    public void shouldPublishUserRemovalIfPurgingUserDataSucceeds() {
        mockValidAccount();

        accountOperations.purgeUserData().test();

        final CurrentUserChangedEvent event = eventBus.lastEventOn(EventQueue.CURRENT_USER_CHANGED);
        assertThat(event.isUserRemoved()).isTrue();
    }

    @Test
    public void shouldBroadcastResetAllIntentIfAccountRemovalSucceeds() {
        mockValidAccount();

        accountOperations.purgeUserData().test();

        Intent nextService = getNextStartedService();

        Assertions.assertThat(nextService).containsAction(PlaybackService.Action.RESET_ALL);
    }

    @Test
    public void purgeUserDataShouldRemoveOfflineContent() {
        mockValidAccount();

        accountOperations.purgeUserData().test();

        verify(clearOfflineContentCommand).call(null);
    }

    private void mockNoAccount() {
        when(accountManager.getSoundCloudAccount()).thenReturn(Optional.absent());
    }

    private void mockValidAccount() {
        when(accountManager.getSoundCloudAccount()).thenReturn(Optional.of(ACCOUNT));
        when(accountManager.getUserUrn(ACCOUNT)).thenReturn(Urn.forUser(123));
        when(tokenOperations.getTokenFromAccount(ACCOUNT)).thenReturn(new Token("123", "456"));
    }
}
