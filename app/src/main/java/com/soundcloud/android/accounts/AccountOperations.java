package com.soundcloud.android.accounts;


import static com.google.common.base.Preconditions.checkNotNull;
import static com.soundcloud.android.api.legacy.model.PublicApiUser.CRAWLER_USER;
import static com.soundcloud.android.rx.observers.DefaultSubscriber.fireAndForget;

import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.api.legacy.model.PublicApiResource;
import com.soundcloud.android.api.legacy.model.PublicApiUser;
import com.soundcloud.android.api.legacy.model.ScModelManager;
import com.soundcloud.android.api.oauth.Token;
import com.soundcloud.android.events.CurrentUserChangedEvent;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.onboarding.auth.SignupVia;
import com.soundcloud.android.playback.service.PlaybackService;
import com.soundcloud.android.rx.ScSchedulers;
import com.soundcloud.android.rx.ScheduledOperations;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.storage.LegacyUserStorage;
import com.soundcloud.android.utils.Log;

import dagger.Lazy;
import org.jetbrains.annotations.Nullable;
import rx.Observable;
import rx.Scheduler;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;

@Singleton
public class AccountOperations extends ScheduledOperations {

    public static final Urn ANONYMOUS_USER_URN = Urn.forUser(Consts.NOT_SET);

    private static final String TOKEN_TYPE = "access_token";

    private final Context context;
    private final AccountManager accountManager;
    private final SoundCloudTokenOperations tokenOperations;
    private final ScModelManager modelManager;
    private final LegacyUserStorage userStorage;
    private final EventBus eventBus;
    private final Lazy<AccountCleanupAction> accountCleanupAction;

    @Deprecated
    private volatile PublicApiUser loggedInUser;
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
    AccountOperations(Context context, AccountManager accountManager, SoundCloudTokenOperations tokenOperations,
                      ScModelManager modelManager, LegacyUserStorage userStorage, EventBus eventBus,
                      Lazy<AccountCleanupAction> accountCleanupAction) {
        this(context, accountManager, tokenOperations, modelManager, userStorage, eventBus, accountCleanupAction,
                ScSchedulers.HIGH_PRIO_SCHEDULER);
    }

    @VisibleForTesting
    AccountOperations(Context context, AccountManager accountManager, SoundCloudTokenOperations tokenOperations,
                      ScModelManager modelManager, LegacyUserStorage userStorage, EventBus eventBus,
                      Lazy<AccountCleanupAction> accountCleanupAction, Scheduler scheduler) {
        super(scheduler);
        this.context = context;
        this.accountManager = accountManager;
        this.tokenOperations = tokenOperations;
        this.modelManager = modelManager;
        this.userStorage = userStorage;
        this.eventBus = eventBus;
        this.accountCleanupAction = accountCleanupAction;
    }

    /**
     * Returns the logged in user. You should not rely on the return value unless you have checked the user is
     * actually logged in.
     */
    @Deprecated
    public PublicApiUser getLoggedInUser() {
        if (loggedInUser == null) {
            // this means we haven't received all user metadata yet, fall back temporarily to a minimal representation
            PublicApiUser user = new PublicApiUser();
            user.setId(getAccountDataLong(AccountInfoKeys.USER_ID.getKey()));
            user.username = getAccountDataString(AccountInfoKeys.USERNAME.getKey());
            user.permalink = getAccountDataString(AccountInfoKeys.USER_PERMALINK.getKey());
            return user;
        }
        return loggedInUser;
    }

    /**
     * Returns the ID of the logged in user. If we don't have the full meta data of the user yet, it will read it
     * from the account.
     * You should not rely on the return value unless you have checked the user is actually logged in.
     */
    @Deprecated // use URNs for anything above the storage layer
    public long getLoggedInUserId() {
        return loggedInUser == null ? getAccountDataLong(AccountInfoKeys.USER_ID.getKey()) : loggedInUser.getId();
    }

    public Urn getLoggedInUserUrn() {
        if (loggedInUserUrn == null){
            loggedInUserUrn = Urn.forUser(getLoggedInUserId());
        }
        return loggedInUserUrn;
    }

    public boolean isLoggedInUser(Urn user) {
        return user.equals(getLoggedInUserUrn());
    }

    public void loadLoggedInUser() {
        final long id = getAccountDataLong(AccountInfoKeys.USER_ID.getKey());
        if (id != Consts.NOT_SET) {
            fireAndForget(userStorage.getUserAsync(id).doOnNext(new Action1<PublicApiUser>() {
                @Override
                public void call(PublicApiUser user) {
                    updateLoggedInUser(user);
                }
            }));
        }
    }

    // adding a null check here because of https://github.com/soundcloud/SoundCloud-Android/issues/2486
    // This happens when you don't sign out, but clear data; it should correct itself by going
    // to AccountManager or constructing the Urn next time we try to get the logged in user.
    private void updateLoggedInUser(@Nullable PublicApiUser user) {
        if (user != null) {
            loggedInUser = modelManager.cache(user, PublicApiResource.CacheUpdateMode.FULL);
            loggedInUserUrn = user.getUrn();
        }
    }

    public void clearLoggedInUser() {
        loggedInUser = null;
        loggedInUserUrn = null;
    }

    public String getGoogleAccountToken(String accountName, String scope, Bundle bundle) throws GoogleAuthException, IOException {
        return GoogleAuthUtil.getToken(context, accountName, scope, bundle);
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
    public Account addOrReplaceSoundCloudAccount(PublicApiUser user, Token token, SignupVia via) {
        Log.i(Log.ONBOARDING_TAG, "adding or replacing SoundCloud account");
        boolean accountexists = false;
        Account account = getSoundCloudAccount();
        if (account != null) {
            Log.i(Log.ONBOARDING_TAG, "SoundCloud account found");
            if (account.name.equals(user.getPermalink())) {
                Log.i(Log.ONBOARDING_TAG, "SoundCloud account matches current user");
                accountexists = true; // same username, do not replace account
            } else {
                Log.i(Log.ONBOARDING_TAG, "SoundCloud account does not match, will replace");
                accountManager.removeAccount(account, null, null);
            }
        }

        if (!accountexists) {
            account = new Account(user.getPermalink(), context.getString(R.string.account_type));
            accountexists = accountManager.addAccountExplicitly(account, null, null);
            Log.i(Log.ONBOARDING_TAG, "SoundCloud account has been added");
        }

        if (accountexists) {
            Log.i(Log.ONBOARDING_TAG, "will updated stored account information");
            tokenOperations.storeSoundCloudTokenData(account, token);
            accountManager.setUserData(account, AccountInfoKeys.USER_ID.getKey(), Long.toString(user.getId()));
            accountManager.setUserData(account, AccountInfoKeys.USERNAME.getKey(), user.getUsername());
            accountManager.setUserData(account, AccountInfoKeys.USER_PERMALINK.getKey(), user.getPermalink());
            accountManager.setUserData(account, AccountInfoKeys.SIGNUP.getKey(), via.getSignupIdentifier());
            updateLoggedInUser(user);
            eventBus.publish(EventQueue.CURRENT_USER_CHANGED, CurrentUserChangedEvent.forUserUpdated(user));
            Log.i(Log.ONBOARDING_TAG, "stored account information updated");
            return account;
        } else {
            Log.i(Log.ONBOARDING_TAG, "SoundCloud account was not added");
            return null;
        }
    }

    public void loginCrawlerUser() {
        Account account = new Account(CRAWLER_USER.getPermalink(), context.getString(R.string.account_type));
        updateLoggedInUser(CRAWLER_USER);
        tokenOperations.storeSoundCloudTokenData(account, Token.EMPTY);
        eventBus.publish(EventQueue.CURRENT_USER_CHANGED, CurrentUserChangedEvent.forUserUpdated(CRAWLER_USER));
    }

    @Nullable
    public Account getSoundCloudAccount() {
        Account[] accounts = accountManager.getAccountsByType(context.getString(R.string.account_type));
        return accounts != null && accounts.length == 1 ? accounts[0] : null;
    }

    public Observable<Void> logout() {
        Account soundCloudAccount = getSoundCloudAccount();
        checkNotNull(soundCloudAccount, "One does not simply remove something that does not exist");

        return schedule(Observable.create(new AccountRemovalFunction(soundCloudAccount, accountManager)))
                .observeOn(AndroidSchedulers.mainThread());
    }

    public Observable<Void> purgeUserData() {
        return schedule(Observable.create(new Observable.OnSubscribe<Void>() {
            @Override
            public void call(Subscriber<? super Void> subscriber) {
                accountCleanupAction.get().call();
                tokenOperations.resetToken();
                clearLoggedInUser();
                eventBus.publish(EventQueue.CURRENT_USER_CHANGED, CurrentUserChangedEvent.forLogout());
                resetPlaybackService();
                subscriber.onCompleted();
            }
        }));
    }

    // TODO: This should be made in the playback operations, which is not used at the moment, since it will cause a circular dependency
    private void resetPlaybackService() {
        Intent intent = new Intent(context, PlaybackService.class);
        intent.setAction(PlaybackService.Actions.RESET_ALL);
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

    public void invalidateSoundCloudToken(Token token) {
        tokenOperations.invalidateToken(token, getSoundCloudAccount());
    }

    public void storeSoundCloudTokenData(Token token) {
        tokenOperations.storeSoundCloudTokenData(getSoundCloudAccount(), token);
    }

    public boolean hasValidToken() {
        return getSoundCloudToken().valid();
    }

    public boolean isCrawler() {
        return loggedInUser != null && loggedInUser.isCrawler();
    }

    private boolean isAnonymousUser() {
        return getLoggedInUserUrn().equals(ANONYMOUS_USER_URN);
    }
}
