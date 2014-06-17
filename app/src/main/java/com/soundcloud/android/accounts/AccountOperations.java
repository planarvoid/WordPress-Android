package com.soundcloud.android.accounts;


import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.soundcloud.android.rx.observers.DefaultSubscriber.fireAndForget;

import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.events.CurrentUserChangedEvent;
import com.soundcloud.android.rx.eventbus.EventBus;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.model.ScModelManager;
import com.soundcloud.android.model.ScResource;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.model.User;
import com.soundcloud.android.model.UserUrn;
import com.soundcloud.android.onboarding.auth.SignupVia;
import com.soundcloud.android.playback.service.PlaybackService;
import com.soundcloud.android.rx.ScSchedulers;
import com.soundcloud.android.rx.ScheduledOperations;
import com.soundcloud.android.storage.UserStorage;
import com.soundcloud.android.utils.IOUtils;
import com.soundcloud.api.Token;
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
import java.util.concurrent.TimeUnit;

@Singleton
public class AccountOperations extends ScheduledOperations {

    private static final int NOT_SET = -1;
    private static final String TOKEN_TYPE = "access_token";
    @VisibleForTesting
    static final long EMAIL_CONFIRMATION_REMIND_PERIOD = TimeUnit.DAYS.toMillis(7);

    private final Context context;
    private final AccountManager accountManager;
    private final SoundCloudTokenOperations tokenOperations;
    private final ScModelManager modelManager;
    private final UserStorage userStorage;
    private final EventBus eventBus;
    private final Lazy<AccountCleanupAction> accountCleanupAction;

    private volatile User loggedInUser;

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
                      ScModelManager modelManager, UserStorage userStorage, EventBus eventBus,
                      Lazy<AccountCleanupAction> accountCleanupAction) {
        this(context, accountManager, tokenOperations, modelManager, userStorage, eventBus, accountCleanupAction,
                ScSchedulers.STORAGE_SCHEDULER);
    }

    @VisibleForTesting
    AccountOperations(Context context, AccountManager accountManager, SoundCloudTokenOperations tokenOperations,
                      ScModelManager modelManager, UserStorage userStorage, EventBus eventBus,
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
    public User getLoggedInUser() {
        if (loggedInUser == null) {
            // this means we haven't received all user metadata yet, fall back temporarily to a minimal representation
            User user = new User();
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

    public UserUrn getLoggedInUserUrn() {
        long loggedInUserId = getLoggedInUserId();
        checkArgument(loggedInUserId != NOT_SET, "Logged in User Id should not be null");
        return Urn.forUser(loggedInUserId);
    }

    public void loadLoggedInUser() {
        final long id = getAccountDataLong(AccountInfoKeys.USER_ID.getKey());
        if (id != AccountOperations.NOT_SET) {
            fireAndForget(userStorage.getUserAsync(id).doOnNext(new Action1<User>() {
                @Override
                public void call(User user) {
                    updateLoggedInUser(user);
                }
            }));
        }
    }

    private void updateLoggedInUser(final User user) {
        loggedInUser = modelManager.cache(user, ScResource.CacheUpdateMode.FULL);
    }

    public void clearLoggedInUser() {
        loggedInUser = null;
    }

    public String getGoogleAccountToken(String accountName, String scope, Bundle bundle) throws GoogleAuthException, IOException {
        return GoogleAuthUtil.getToken(context, accountName, scope, bundle);
    }

    public void invalidateGoogleAccountToken(String token) {
        GoogleAuthUtil.invalidateToken(context, token);
    }

    //TODO: now that this class is a singleton, we should probably cache the current account?
    public boolean isUserLoggedIn() {
        return getSoundCloudAccount() != null;
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
    public Account addOrReplaceSoundCloudAccount(User user, Token token, SignupVia via) {
        boolean accountexists = false;
        Account account = getSoundCloudAccount();
        if (account != null) {
            if (account.name.equals(user.getUsername())) {
                accountexists = true; // same username, do not replace account
            } else {
                accountManager.removeAccount(account, null, null);
            }
        }

        if (!accountexists) {
            account = new Account(user.getUsername(), context.getString(R.string.account_type));
            accountexists = accountManager.addAccountExplicitly(account, null, null);
        }

        if (accountexists) {
            tokenOperations.storeSoundCloudTokenData(account, token);
            accountManager.setUserData(account, AccountInfoKeys.USER_ID.getKey(), Long.toString(user.getId()));
            accountManager.setUserData(account, AccountInfoKeys.USERNAME.getKey(), user.getUsername());
            accountManager.setUserData(account, AccountInfoKeys.USER_PERMALINK.getKey(), user.getPermalink());
            accountManager.setUserData(account, AccountInfoKeys.SIGNUP.getKey(), via.getSignupIdentifier());
            updateLoggedInUser(user);
            eventBus.publish(EventQueue.CURRENT_USER_CHANGED, CurrentUserChangedEvent.forUserUpdated(user));
            return account;
        } else {
            return null;
        }
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
                clearLoggedInUser();
                eventBus.publish(EventQueue.CURRENT_USER_CHANGED, CurrentUserChangedEvent.forLogout());
                context.sendBroadcast(new Intent(PlaybackService.Actions.RESET_ALL));
                subscriber.onCompleted();
            }
        }));
    }

    @Nullable
    private String getAccountDataString(String key) {
        if (isUserLoggedIn()) {
            return accountManager.getUserData(getSoundCloudAccount(), key);
        }

        return null;
    }

    //TODO Should have a consistent anonymous user id Uri forUser(long id). ClientUri.forUser() is related with this issue
    private long getAccountDataLong(String key) {
        String data = getAccountDataString(key);
        return data == null ? NOT_SET : Long.parseLong(data);
    }

    //TODO this seems wrong to me, should we not differentiate between no data existing and a false value existing?
    //Also, shouldn't be public...
    public boolean getAccountDataBoolean(String key) {
        String data = getAccountDataString(key);
        return data != null && Boolean.parseBoolean(data);
    }

    public boolean setAccountData(String key, String value) {
        if (isUserLoggedIn()) {
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
            accountManager.setUserData(getSoundCloudAccount(), key, value);
            return true;
        } else {
            return false;
        }
    }

    @Nullable
    public Token getSoundCloudToken() {
        if (isUserLoggedIn()) {
            return tokenOperations.getSoundCloudToken(getSoundCloudAccount());
        }

        return null;
    }

    public void invalidateSoundCloudToken(Token token) {
        tokenOperations.invalidateToken(token, getSoundCloudAccount());
    }

    public void storeSoundCloudTokenData(Token token) {
        checkState(isUserLoggedIn(), "SoundCloud Account needs to exist before storing token info");
        tokenOperations.storeSoundCloudTokenData(getSoundCloudAccount(), token);
    }

    public boolean shouldCheckForConfirmedEmailAddress(User currentUser) {
        boolean alreadyConfirmed = currentUser.isPrimaryEmailConfirmed();

        long lastReminded = getAccountDataLong(Consts.PrefKeys.LAST_EMAIL_CONFIRMATION_REMINDER);
        boolean isTimeToRemindAgain = lastReminded <= 0 || System.currentTimeMillis() - lastReminded > EMAIL_CONFIRMATION_REMIND_PERIOD;

        return !alreadyConfirmed && isTimeToRemindAgain && IOUtils.isConnected(context) && isTokenValid();
    }

    private boolean isTokenValid() {
        final Token token = getSoundCloudToken();
        return token != null && token.valid();
    }
}
