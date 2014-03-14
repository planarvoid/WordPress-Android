package com.soundcloud.android.accounts;

import com.soundcloud.android.accounts.exception.OperationFailedException;
import rx.Observable;
import rx.Subscriber;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerFuture;

class AccountRemovalFunction implements Observable.OnSubscribe<Void> {
    private final Account mSoundCloudAccount;
    private final AccountManager mAccountManager;

    public AccountRemovalFunction(Account soundCloudAccount, AccountManager accountManager) {
        mSoundCloudAccount = soundCloudAccount;
        mAccountManager = accountManager;
    }

    @Override
    public void call(Subscriber<? super Void> observer) {
        try {
            AccountManagerFuture<Boolean> accountRemovalFuture = mAccountManager.removeAccount(mSoundCloudAccount, null, null);

            if (accountRemovalFuture.getResult()) {
                observer.onCompleted();
            } else {
                observer.onError(new OperationFailedException());
            }

        } catch (Exception e) {
            observer.onError(e);
        }
    }
}
