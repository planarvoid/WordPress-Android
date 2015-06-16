package com.soundcloud.android.payments;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import rx.Observable;

@RunWith(SoundCloudTestRunner.class)
public class TransactionStateTest {

    @Test
    public void transactionIsNotInProgressWithNoObservables() {
        expect(new TransactionState(null, null).isTransactionInProgress()).toBeFalse();
    }

    @Test
    public void transactionIsInProgressWithPurchaseObservable() {
        expect(new TransactionState(Observable.<String>empty(), null).isTransactionInProgress()).toBeTrue();
    }

    @Test
    public void transactionIsInProgressWithPurchaseAndStatusObservable() {
        expect(new TransactionState(Observable.<String>empty(), Observable.<PurchaseStatus>empty()).isTransactionInProgress()).toBeTrue();
    }

    @Test
    public void notRetrievingStatusWithNoStatusObservable() {
        expect(new TransactionState(Observable.<String>empty(), null).isRetrievingStatus()).toBeFalse();
    }

    @Test
    public void retrievingStatusWithStatusObservable() {
        expect(new TransactionState(Observable.<String>empty(), Observable.<PurchaseStatus>empty()).isRetrievingStatus()).toBeTrue();
    }

    @Test
    public void purchaseReturnsPurchaseObservable() {
        final Observable<String> purchase = Observable.empty();
        expect(new TransactionState(purchase, Observable.<PurchaseStatus>empty()).purchase()).toBe(purchase);
    }

    @Test
    public void statusReturnsStatusObservable() {
        final Observable<PurchaseStatus> status = Observable.empty();
        expect(new TransactionState(Observable.<String>empty(), status).status()).toBe(status);
    }

}
