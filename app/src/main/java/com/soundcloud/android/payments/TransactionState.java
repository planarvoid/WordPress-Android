package com.soundcloud.android.payments;

import rx.Observable;

class TransactionState {

    private final Observable<String> purchase;
    private final Observable<PurchaseStatus> verify;

    TransactionState(Observable<String> purchase, Observable<PurchaseStatus> verify) {
        this.purchase = purchase;
        this.verify = verify;
    }

    public boolean transactionInProgress() {
        return verify != null || purchase != null;
    }

    public boolean isVerifying(){
        return verify != null;
    }

    public Observable<String> purchase() {
        return purchase;
    }

    public Observable<PurchaseStatus> verify() {
        return verify;
    }
}
