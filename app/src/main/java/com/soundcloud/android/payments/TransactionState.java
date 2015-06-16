package com.soundcloud.android.payments;

import rx.Observable;

class TransactionState {

    private final Observable<String> purchase;
    private final Observable<PurchaseStatus> status;

    TransactionState(Observable<String> purchase, Observable<PurchaseStatus> status) {
        this.purchase = purchase;
        this.status = status;
    }

    public boolean isTransactionInProgress() {
        return status != null || purchase != null;
    }

    public boolean isRetrievingStatus() {
        return status != null;
    }

    public Observable<String> purchase() {
        return purchase;
    }

    public Observable<PurchaseStatus> status() {
        return status;
    }
}
