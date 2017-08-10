package com.soundcloud.android.payments;

import static org.assertj.core.api.Java6Assertions.assertThat;

import io.reactivex.Single;
import org.junit.Test;

public class TransactionStateTest {

    @Test
    public void transactionIsNotInProgressWithNoObservables() {
        assertThat(new TransactionState(null, null).isTransactionInProgress()).isFalse();
    }

    @Test
    public void transactionIsInProgressWithPurchaseObservable() {
        assertThat(new TransactionState(Single.never(), null).isTransactionInProgress()).isTrue();
    }

    @Test
    public void transactionIsInProgressWithPurchaseAndStatusObservable() {
        assertThat(new TransactionState(Single.never(), Single.never()).isTransactionInProgress()).isTrue();
    }

    @Test
    public void notRetrievingStatusWithNoStatusObservable() {
        assertThat(new TransactionState(Single.never(), null).isRetrievingStatus()).isFalse();
    }

    @Test
    public void retrievingStatusWithStatusObservable() {
        assertThat(new TransactionState(Single.never(),
                                        Single.never()).isRetrievingStatus()).isTrue();
    }

    @Test
    public void purchaseReturnsPurchaseObservable() {
        final Single<String> purchase = Single.never();
        assertThat(new TransactionState(purchase, Single.never()).purchase()).isSameAs(purchase);
    }

    @Test
    public void statusReturnsStatusObservable() {
        final Single<PurchaseStatus> status = Single.never();
        assertThat(new TransactionState(Single.never(), status).status()).isSameAs(status);
    }

}
