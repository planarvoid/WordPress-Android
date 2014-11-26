package com.soundcloud.android.payments;

enum PurchaseStatus {

    PENDING,
    SUCCESS,
    UPDATE_FAIL,
    VERIFY_FAIL,
    VERIFY_TIMEOUT,
    NONE;

    public boolean isPending() {
        return this == PurchaseStatus.PENDING;
    }

}
