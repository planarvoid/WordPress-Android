package com.soundcloud.android.accounts;


import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.common.annotations.VisibleForTesting;
import com.soundcloud.android.Consts;
import com.soundcloud.android.R;
import com.soundcloud.android.SoundCloudApplication;
import com.soundcloud.android.model.User;
import com.soundcloud.android.onboarding.auth.SignupVia;
import com.soundcloud.android.rx.ScSchedulers;
import com.soundcloud.android.rx.ScheduledOperations;
import com.soundcloud.android.utils.IOUtils;
import com.soundcloud.android.utils.Log;
import com.soundcloud.api.Token;
import org.jetbrains.annotations.Nullable;
import rx.Observable;
import rx.Scheduler;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;

import javax.inject.Inject;
import java.io.IOException;

public class AccountOperations extends ScheduledOperations {

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

    public static final int NOT_SET = -1;

    private static final String TOKEN_TYPE = "access_token";
    @VisibleForTesting
    static final int EMAIL_CONFIRMATION_REMIND_PERIOD = 86400 * 1000 * 7; // 1 week

    private final AccountManager accountManager;
    private final SoundCloudTokenOperations tokenOperations;
    private final Context context;

    public AccountOperations(Context context) {
        this(AccountManager.get(context.getApplicationContext()),
                context.getApplicationContext(),
                new SoundCloudTokenOperations(context.getApplicationContext()));
    }


    @Inject
    public AccountOperations(AccountManager accountManager, Context context, SoundCloudTokenOperations tokenOperations) {
        this(accountManager, context, tokenOperations, ScSchedulers.STORAGE_SCHEDULER);
    }

    @VisibleForTesting
    AccountOperations(AccountManager accountManager, Context context, SoundCloudTokenOperations tokenOperations,
                      Scheduler scheduler) {
        super(scheduler);
        this.accountManager = accountManager;
        this.context = context;
        this.tokenOperations = tokenOperations;
    }

    public String getGoogleAccountToken(String accountName, String scope, Bundle bundle) throws GoogleAuthException, IOException {
        return GoogleAuthUtil.getToken(context, accountName, scope, bundle);
    }

    public void invalidateGoogleAccountToken(String token) {
        GoogleAuthUtil.invalidateToken(context, token);
    }

    public boolean soundCloudAccountExists() {
        return getSoundCloudAccount() != null;
    }

    public void addSoundCloudAccountManually(Activity currentActivityContext) {
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

    public Observable<Void> removeSoundCloudAccount() {
        Account soundCloudAccount = getSoundCloudAccount();
        checkNotNull(soundCloudAccount, "One does not simply remove something that does not exist");

        return schedule(Observable.create(new AccountRemovalFunction(soundCloudAccount, accountManager)))
                .observeOn(AndroidSchedulers.mainThread());
    }

    public Observable<Void> purgeUserData() {
        return Observable.create(new Observable.OnSubscribe<Void>() {
            @Override
            public void call(Subscriber<? super Void> subscriber) {
                new AccountCleanupAction(context).call();
                subscriber.onCompleted();
            }
        });
    }

    @Nullable
    public String getAccountDataString(String key) {
        Log.d(AccountOperations.class.getSimpleName(), key);
        Log.d(AccountOperations.class.getSimpleName(), String.format("%s", getSoundCloudAccount() == null ? "null" : "account" +
                ""));
        if (soundCloudAccountExists()) {
            return accountManager.getUserData(getSoundCloudAccount(), key);
        }

        return null;
    }

    //TODO Create a class which works as a service to store preference data instead of exposing these lowlevel constructs
    //TODO Should have a consistent anonymous user id Uri forUser(long id). ClientUri.forUser() is related with this issue
    public long getAccountDataLong(String key) {
        String data = getAccountDataString(key);
        return data == null ? -1 : Long.parseLong(data);
    }

    //TODO this seems wrong to me, should we not differentiate between no data existing and a false value existing?
    public boolean getAccountDataBoolean(String key) {
        String data = getAccountDataString(key);
        return data != null && Boolean.parseBoolean(data);
    }

    public boolean setAccountData(String key, String value) {
        if (!soundCloudAccountExists()) {
            return false;
        } else {
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
        }
    }

    @Nullable
    public Token getSoundCloudToken() {
        if (soundCloudAccountExists()) {
            return tokenOperations.getSoundCloudToken(getSoundCloudAccount());
        }

        return null;
    }

    public long getLoggedInUserId(){
        return SoundCloudApplication.getUserId();
    }

    public void invalidateSoundCloudToken(Token token) {
        tokenOperations.invalidateToken(token, getSoundCloudAccount());
    }

    public void storeSoundCloudTokenData(Token token) {
        checkState(soundCloudAccountExists(), "SoundCloud Account needs to exist before storing token info");
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
