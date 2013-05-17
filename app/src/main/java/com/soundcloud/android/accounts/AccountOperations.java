package com.soundcloud.android.accounts;


import static com.google.common.base.Preconditions.checkNotNull;

import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.soundcloud.android.R;
import rx.Observable;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;

import java.io.IOException;

public class AccountOperations {

    private final AccountManager accountManager;
    private final Resources resources;
    private final Context context;

    public AccountOperations(Context context) {
        this(AccountManager.get(context), context.getResources(), context);
    }

    protected AccountOperations(AccountManager accountManager, Resources resources, Context context) {
        this.accountManager = accountManager;
        this.resources = resources;
        this.context = context;
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

    public Account getSoundCloudAccount() {
        Account[] accounts = accountManager.getAccountsByType(resources.getString(R.string.account_type));
        return accounts != null && accounts.length == 1 ? accounts[0] : null;
    }

    public Observable<Void> removeSoundCloudAccount() {
        Account soundCloudAccount = getSoundCloudAccount();
        checkNotNull(soundCloudAccount, "One does not simply remove something that does not exist");

        Observable<Void> accountRemovalObservable = Observable.create(new AccountRemovalFunction(soundCloudAccount, accountManager, context));
        Observable<Void> resetStateObservable = Observable.create(new AccountRemovalFunction(soundCloudAccount, accountManager, context));
        return Observable.concat(accountRemovalObservable, resetStateObservable);
    }
}
