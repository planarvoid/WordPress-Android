package com.soundcloud.android.payments;

import io.reactivex.Single;

class TransactionState {

    private final Single<String> purchase;
    private final Single<PurchaseStatus> status;

    TransactionState(Single<String> purchase, Single<PurchaseStatus> status) {
        this.purchase = purchase;
        this.status = status;
    }

    boolean isTransactionInProgress() {
        return status != null || purchase != null;
    }

    boolean isRetrievingStatus() {
        return status != null;
    }

    public Single<String> purchase() {
        return purchase;
    }

    public Single<PurchaseStatus> status() {
        return status;
    }
}
