package com.soundcloud.android.accounts;

import static android.util.Log.DEBUG;
import static com.soundcloud.java.checks.Preconditions.checkNotNull;

import com.facebook.login.LoginManager;
import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.soundcloud.android.ApplicationModule;
import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
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
import com.soundcloud.android.utils.AndroidUtils;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.utils.GooglePlayServicesWrapper;
import com.soundcloud.java.strings.Strings;
import com.soundcloud.rx.eventbus.EventBus;
import dagger.Lazy;
import org.jetbrains.annotations.Nullable;
import rx.Observable;
import rx.Scheduler;
import rx.android.schedulers.AndroidSchedulers;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.IOException;

@Singleton
public class AccountOperations {

    public static final String CRAWLER_USER_PERMALINK = "SoundCloud";
    public static final Urn ANONYMOUS_USER_URN = Urn.forUser(0);

    // Why is this -2? I don't have a good reason. We decided it is safer than -1, which is NOT_SET all over the app
    public static final int CRAWLER_USER_ID = -2;
    public static final Urn CRAWLER_USER_URN = Urn.forUser(CRAWLER_USER_ID);

    private static final String TOKEN_TYPE = "access_token";

    private final Context context;
    private final AccountManager accountManager;
    private final SoundCloudTokenOperations tokenOperations;
    private final EventBus eventBus;
    private final Scheduler scheduler;

    private final GooglePlayServicesWrapper googlePlayServicesWrapper;
    private final PlaySessionStateStorage playSessionStateStorage;
    private final Lazy<ConfigurationOperations> configurationOperations;
    private final Lazy<AccountCleanupAction> accountCleanupAction;
    private final Lazy<ClearOfflineContentCommand> clearOfflineContentCommand;
    private final Lazy<LoginManager> facebookLoginManager;

    private volatile Urn loggedInUserUrn;

    public enum AccountInfoKeys {
        USERNAME("currentUsername"),
        USER_ID("currentUserId"),
        USER_PERMALINK("currentUserPermalink"),
        SIGNUP("signup");

        private final String key;

        AccountInfoKeys(String key) {
            this.key = key;
        }

        public String getKey() {
            return key;
        }
    }

    @Inject
    AccountOperations(Context context,
                      AccountManager accountManager,
                      SoundCloudTokenOperations tokenOperations,
                      EventBus eventBus,
                      PlaySessionStateStorage playSessionStateStorage,
                      Lazy<ConfigurationOperations> configurationOperations,
                      Lazy<AccountCleanupAction> accountCleanupAction,
                      Lazy<ClearOfflineContentCommand> clearOfflineContentCommand,
                      Lazy<LoginManager> facebookLoginManager,
                      GooglePlayServicesWrapper googlePlayServicesWrapper,
                      @Named(ApplicationModule.HIGH_PRIORITY) Scheduler scheduler) {
        this.context = context;
        this.accountManager = accountManager;
        this.tokenOperations = tokenOperations;
        this.eventBus = eventBus;
        this.playSessionStateStorage = playSessionStateStorage;
        this.configurationOperations = configurationOperations;
        this.accountCleanupAction = accountCleanupAction;
        this.clearOfflineContentCommand = clearOfflineContentCommand;
        this.facebookLoginManager = facebookLoginManager;
        this.googlePlayServicesWrapper = googlePlayServicesWrapper;
        this.scheduler = scheduler;
    }

    public String getLoggedInUsername() {
        return getAccountDataString(AccountInfoKeys.USERNAME.getKey());
    }

    public Urn getLoggedInUserUrn() {
        if (loggedInUserUrn == null) {
            final long loggedInUserId = getLoggedInUserId();
            loggedInUserUrn = loggedInUserId == Consts.NOT_SET ? ANONYMOUS_USER_URN : Urn.forUser(loggedInUserId);
        }
        return loggedInUserUrn;
    }

    public boolean isLoggedInUser(Urn user) {
        return user.equals(getLoggedInUserUrn());
    }

    private long getLoggedInUserId() {
        return getAccountDataLong(AccountInfoKeys.USER_ID.getKey());
    }

    public void clearLoggedInUser() {
        loggedInUserUrn = ANONYMOUS_USER_URN;
    }

    public String getGoogleAccountToken(String accountName,
                                        String scope,
                                        Bundle bundle) throws GoogleAuthException, IOException {
        return googlePlayServicesWrapper.getAuthToken(context, accountName, scope, bundle);
    }

    public void invalidateGoogleAccountToken(String token) {
        GoogleAuthUtil.invalidateToken(context, token);
    }

    //TODO: now that this class is a singleton, we should probably cache the current account?
    public boolean isUserLoggedIn() {
        return !isAnonymousUser() || isCrawler();
    }

    public void triggerLoginFlow(Activity currentActivityContext) {
        accountManager.addAccount(
                context.getString(R.string.account_type),
                TOKEN_TYPE, null, null, currentActivityContext, null, null);
    }

    /**
     * Adds the given user as a SoundCloud account to the device's list of accounts. Idempotent, will be a no op if
     * already called.
     *
     * @return the new account, or null if account already existed or adding it failed
     */
    @Nullable
    public Account addOrReplaceSoundCloudAccount(ApiUser user, Token token, SignupVia via) {
        ErrorUtils.logInBetaAndBelow(DEBUG, "AddAccountFlow", "entering: addOrReplaceSoundCloudAccount");
        if (user == null) {
            ErrorUtils.logInBetaAndBelow(DEBUG, "AddAccountFlow", "user: null!");
        } else {
            ErrorUtils.logInBetaAndBelow(DEBUG, "AddAccountFlow", "user: %s", user.getPermalink());
        }
        if (token == null) {
            ErrorUtils.logInBetaAndBelow(DEBUG, "AddAccountFlow", "token: null!");
        } else {
            ErrorUtils.logInBetaAndBelow(DEBUG, "AddAccountFlow", "token valid: %b" +
                                                 " accessToken empty: %b" +
                                                 " refreshToken empty: %b" +
                                                 " scope empty: %b" +
                                                 " signup empty: %b" +
                                                 " expiresAt: %d",
                                         token.valid(), Strings.isNullOrEmpty(token.getAccessToken()),
                                         Strings.isNullOrEmpty(token.getRefreshToken()),
                                         Strings.isNullOrEmpty(token.getScope()),
                                         Strings.isNullOrEmpty(token.getSignup()),
                                         token.getExpiresAt());
        }

        boolean accountExists = false;
        Account account = getSoundCloudAccount();
        if (account != null) {
            if (account.name.equals(user.getPermalink())) {
                ErrorUtils.logInBetaAndBelow(DEBUG, "AddAccountFlow", "already have SoundCloud account, same account!");
                accountExists = true; // same username, do not replace account
            } else {
                ErrorUtils.logInBetaAndBelow(DEBUG, "AddAccountFlow", "already have SoundCloud account, removing account");
                accountManager.removeAccount(account, null, null);
            }
        }

        ErrorUtils.logInBetaAndBelow(DEBUG, "AddAccountFlow", "accountExists: " + accountExists);
        if (!accountExists) {
            String accountType = context.getString(R.string.account_type);
            String permalink = user.getPermalink();
            ErrorUtils.logInBetaAndBelow(DEBUG, "AddAccountFlow", "creating account for user: %s of type: %s", permalink, accountType);
            account = new Account(permalink, accountType);

            // workaround for https://code.google.com/p/android/issues/detail?id=210992
            // This is a no-op even if the bug is fixed. Remove upon release of Android N
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
                ErrorUtils.logInBetaAndBelow(DEBUG, "AddAccountFlow", "removing account due to the Android N preview issue");
                accountManager.removeAccountExplicitly(account);
            }

            ErrorUtils.logInBetaAndBelow(DEBUG, "AddAccountFlow", "adding account");
            accountExists = accountManager.addAccountExplicitly(account, null, null);
        }

        ErrorUtils.logInBetaAndBelow(DEBUG, "AddAccountFlow", "addAccountExplicitly result: %b", accountExists);
        if (accountExists) {
            ErrorUtils.logInBetaAndBelow(DEBUG, "AddAccountFlow", "storing account data");
            tokenOperations.storeSoundCloudTokenData(account, token);
            accountManager.setUserData(account, AccountInfoKeys.USER_ID.getKey(), Long.toString(user.getId()));
            accountManager.setUserData(account, AccountInfoKeys.USERNAME.getKey(), user.getUsername());
            accountManager.setUserData(account, AccountInfoKeys.USER_PERMALINK.getKey(), user.getPermalink());
            accountManager.setUserData(account, AccountInfoKeys.SIGNUP.getKey(), via.getSignupIdentifier());
            loggedInUserUrn = user.getUrn();
            eventBus.publish(EventQueue.CURRENT_USER_CHANGED, CurrentUserChangedEvent.forUserUpdated(user.getUrn()));
            ErrorUtils.logInBetaAndBelow(DEBUG, "AddAccountFlow", "addOrReplaceSoundCloudAccount returning account");
            return account;
        } else {
            ErrorUtils.logInBetaAndBelow(DEBUG, "AddAccountFlow", "addOrReplaceSoundCloudAccount returning: null");
            return null;
        }
    }

    public void loginCrawlerUser() {
        Account account = new Account(CRAWLER_USER_PERMALINK, context.getString(R.string.account_type));
        loggedInUserUrn = CRAWLER_USER_URN;
        tokenOperations.storeSoundCloudTokenData(account, Token.EMPTY);
        eventBus.publish(EventQueue.CURRENT_USER_CHANGED, CurrentUserChangedEvent.forUserUpdated(CRAWLER_USER_URN));
    }

    @Nullable
    public Account getSoundCloudAccount() {
        final Account[] accounts = AndroidUtils.getAccounts(accountManager, context.getString(R.string.account_type));
        return accounts != null && accounts.length == 1 ? accounts[0] : null;
    }

    public Observable<Void> logout() {
        Account soundCloudAccount = getSoundCloudAccount();
        checkNotNull(soundCloudAccount, "One does not simply remove something that does not exist");

        return configurationOperations.get()
                                      .deregisterDevice()
                                      .flatMap(o -> Observable.create(new AccountRemovalFunction(
                                              soundCloudAccount,
                                              accountManager)))
                                      .observeOn(AndroidSchedulers.mainThread())
                                      .subscribeOn(scheduler);
    }

    public Observable<Void> purgeUserData() {
        return Observable.<Void>create(subscriber -> {
            clearOfflineContentCommand.get().call(null);
            accountCleanupAction.get().call();
            tokenOperations.resetToken();
            clearFacebookStorage();
            clearLoggedInUser();
            eventBus.publish(EventQueue.CURRENT_USER_CHANGED, CurrentUserChangedEvent.forLogout());
            resetPlaybackService();
            subscriber.onCompleted();
            playSessionStateStorage.clear();
        }).subscribeOn(scheduler);
    }

    private void clearFacebookStorage() {
        facebookLoginManager.get().logOut();
    }

    // TODO: This should be made in the playback operations, which is not used at the moment, since it will cause a circular dependency
    private void resetPlaybackService() {
        Intent intent = new Intent(context, PlaybackService.class);
        intent.setAction(PlaybackService.Action.RESET_ALL);
        context.startService(intent);
    }

    @Nullable
    private String getAccountDataString(String key) {
        final Account soundCloudAccount = getSoundCloudAccount();
        if (soundCloudAccount != null) {
            return accountManager.getUserData(soundCloudAccount, key);
        }
        return null;
    }

    //TODO Should have a consistent anonymous user id Uri forUser(long id). ClientUri.forUser() is related with this issue
    private long getAccountDataLong(String key) {
        String data = getAccountDataString(key);
        return data == null ? Consts.NOT_SET : Long.parseLong(data);
    }

    //TODO this seems wrong to me, should we not differentiate between no data existing and a false value existing?
    //Also, shouldn't be public...
    public boolean getAccountDataBoolean(String key) {
        String data = getAccountDataString(key);
        return data != null && Boolean.parseBoolean(data);
    }

    public boolean setAccountData(String key, String value) {
        final Account soundCloudAccount = getSoundCloudAccount();
        if (soundCloudAccount != null) {
            /*
            TODO: not sure : setUserData off the ui thread??
                StrictMode policy violation; ~duration=161 ms: android.os.StrictMode$StrictModeDiskWriteViolation: policy=279 violation=1

                D/StrictMode(15333): 	at android.os.StrictMode.readAndHandleBinderCallViolations(StrictMode.java:1617)
                D/StrictMode(15333): 	at android.os.Parcel.readExceptionCode(Parcel.java:1309)
                D/StrictMode(15333): 	at android.os.Parcel.readException(Parcel.java:1278)
                D/StrictMode(15333): 	at android.accounts.IAccountManager$Stub$Proxy.setUserData(IAccountManager.java:701)
                D/StrictMode(15333): 	at android.accounts.AccountManager.setUserData(AccountManager.java:684)
                D/StrictMode(15333): 	at com.soundcloud.android.SoundCloudApplication.setAccountData(SoundCloudApplication.java:314)
             */
            accountManager.setUserData(soundCloudAccount, key, value);
            return true;
        }
        return false;
    }

    public Token getSoundCloudToken() {
        return tokenOperations.getTokenFromAccount(getSoundCloudAccount());
    }

    public void updateToken(Token token) {
        tokenOperations.setToken(token);
    }

    public void storeSoundCloudTokenData(Token token) {
        tokenOperations.storeSoundCloudTokenData(getSoundCloudAccount(), token);
    }

    public boolean hasValidToken() {
        return getSoundCloudToken().valid();
    }

    public boolean isCrawler() {
        return getLoggedInUserUrn().equals(CRAWLER_USER_URN);
    }

    public void clearCrawler() {
        if (isCrawler()) {
            clearLoggedInUser();
        }
    }

    private boolean isAnonymousUser() {
        return getLoggedInUserUrn().equals(ANONYMOUS_USER_URN);
    }

}
