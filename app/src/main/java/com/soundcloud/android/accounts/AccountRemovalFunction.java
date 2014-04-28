package com.soundcloud.android.accounts;

import com.soundcloud.android.accounts.exception.OperationFailedException;
import rx.Observable;
import rx.Subscriber;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerFuture;

class AccountRemovalFunction implements Observable.OnSubscribe<Void> {
    private final Account account;
    private final AccountManager accountManager;

    public AccountRemovalFunction(Account account, AccountManager accountManager) {
        this.account = account;
        this.accountManager = accountManager;
    }

    @Override
    public void call(Subscriber<? super Void> observer) {
        try {
            AccountManagerFuture<Boolean> accountRemovalFuture = accountManager.removeAccount(account, null, null);

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
