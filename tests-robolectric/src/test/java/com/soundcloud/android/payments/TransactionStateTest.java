package com.soundcloud.android.payments;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import rx.Observable;

@RunWith(SoundCloudTestRunner.class)
public class TransactionStateTest {

    @Test
    public void transactionInProgressFalseWithNoObservables() throws Exception {
        expect(new TransactionState(null, null).transactionInProgress()).toBeFalse();
    }

    @Test
    public void transactionInProgressTrueWithPurchaseObservable() throws Exception {
        expect(new TransactionState(Observable.<String>empty(), null).transactionInProgress()).toBeTrue();
    }

    @Test
    public void transactionInProgressTrueWithPurchaseAndVerifyObservable() throws Exception {
        expect(new TransactionState(Observable.<String>empty(), Observable.<PurchaseStatus>empty()).transactionInProgress()).toBeTrue();
    }

    @Test
    public void verifyingFalseWithNoVerifyObservable() throws Exception {
        expect(new TransactionState(Observable.<String>empty(), null).isVerifying()).toBeFalse();
    }

    @Test
    public void verifyingTrueWithVerifyObservable() throws Exception {
        expect(new TransactionState(Observable.<String>empty(), Observable.<PurchaseStatus>empty()).isVerifying()).toBeTrue();
    }

    @Test
    public void purchaseReturnsPurchaseObservable() throws Exception {
        final Observable<String> purchase = Observable.empty();
        expect(new TransactionState(purchase, Observable.<PurchaseStatus>empty()).purchase()).toBe(purchase);
    }

    @Test
    public void verifyReturnsVerifyObservable() throws Exception {
        final Observable<PurchaseStatus> verify = Observable.empty();
        expect(new TransactionState(Observable.<String>empty(), verify).verify()).toBe(verify);
    }
}
