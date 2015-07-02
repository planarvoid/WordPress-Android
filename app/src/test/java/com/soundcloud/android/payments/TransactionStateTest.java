package com.soundcloud.android.payments;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import rx.Observable;

public class TransactionStateTest {

    @Test
    public void transactionIsNotInProgressWithNoObservables() {
        assertThat(new TransactionState(null, null).isTransactionInProgress()).isFalse();
    }

    @Test
    public void transactionIsInProgressWithPurchaseObservable() {
        assertThat(new TransactionState(Observable.<String>empty(), null).isTransactionInProgress()).isTrue();
    }

    @Test
    public void transactionIsInProgressWithPurchaseAndStatusObservable() {
        assertThat(new TransactionState(Observable.<String>empty(), Observable.<PurchaseStatus>empty()).isTransactionInProgress()).isTrue();
    }

    @Test
    public void notRetrievingStatusWithNoStatusObservable() {
        assertThat(new TransactionState(Observable.<String>empty(), null).isRetrievingStatus()).isFalse();
    }

    @Test
    public void retrievingStatusWithStatusObservable() {
        assertThat(new TransactionState(Observable.<String>empty(), Observable.<PurchaseStatus>empty()).isRetrievingStatus()).isTrue();
    }

    @Test
    public void purchaseReturnsPurchaseObservable() {
        final Observable<String> purchase = Observable.empty();
        assertThat(new TransactionState(purchase, Observable.<PurchaseStatus>empty()).purchase()).isSameAs(purchase);
    }

    @Test
    public void statusReturnsStatusObservable() {
        final Observable<PurchaseStatus> status = Observable.empty();
        assertThat(new TransactionState(Observable.<String>empty(), status).status()).isSameAs(status);
    }

}
