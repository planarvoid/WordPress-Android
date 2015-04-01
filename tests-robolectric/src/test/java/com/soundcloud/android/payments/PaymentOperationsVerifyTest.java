package com.soundcloud.android.payments;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.matchers.SoundCloudMatchers.isApiRequestTo;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.ApiClientRx;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiResponse;
import com.soundcloud.android.payments.googleplay.BillingResult;
import com.soundcloud.android.payments.googleplay.BillingService;
import com.soundcloud.android.payments.googleplay.Payload;
import com.soundcloud.android.payments.googleplay.SubscriptionStatus;
import com.soundcloud.android.payments.googleplay.TestBillingResults;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import rx.Observable;
import rx.observers.TestObserver;
import rx.schedulers.TestScheduler;

import java.util.concurrent.TimeUnit;

@RunWith(SoundCloudTestRunner.class)
public class PaymentOperationsVerifyTest {

    @Mock private ApiClientRx api;
    @Mock private BillingService billingService;
    @Mock private TokenStorage tokenStorage;

    private PaymentOperations paymentOperations;
    private BillingResult billingResult;
    private TestObserver<PurchaseStatus> observer;
    private TestScheduler scheduler;

    @Before
    public void setUp() throws Exception {
        scheduler = new TestScheduler();
        paymentOperations = new PaymentOperations(scheduler, api, billingService, tokenStorage);
        billingResult = TestBillingResults.success();
        observer = new TestObserver<>();
        when(tokenStorage.getCheckoutToken()).thenReturn("token_123");
        when(api.response(argThat(isApiRequestTo("POST", ApiEndpoints.CHECKOUT_URN.path("token_123"))
                .withContent(UpdateCheckout.fromSuccess(billingResult.getPayload())))))
                .thenReturn(Observable.just(new ApiResponse(null, HttpStatus.SC_OK, null)));
        when(api.mappedResponse(argThat(isApiRequestTo("GET", ApiEndpoints.CHECKOUT_URN.path("token_123")))))
                .thenReturn(successObservable());
    }

    @Test
    public void verifyPostsCheckoutSuccess() {
        paymentOperations.verify(billingResult.getPayload()).subscribe();

        verify(api).response(argThat(isApiRequestTo("POST", ApiEndpoints.CHECKOUT_URN.path("token_123"))
                .withContent(UpdateCheckout.fromSuccess(billingResult.getPayload()))));
    }

    @Test
    public void verifyReturnsUpdateFailStatusIfUpdateFailed() {
        when(api.response(argThat(isApiRequestTo("POST", ApiEndpoints.CHECKOUT_URN.path("token_123"))
                .withContent(UpdateCheckout.fromSuccess(billingResult.getPayload())))))
                .thenReturn(Observable.just(new ApiResponse(null, HttpStatus.SC_FORBIDDEN, null)));

        paymentOperations.verify(billingResult.getPayload()).subscribe(observer);
        scheduler.advanceTimeBy(2, TimeUnit.SECONDS);

        expect(observer.getOnNextEvents().get(0)).toEqual(PurchaseStatus.UPDATE_FAIL);
    }

    @Test
    public void verifyGetsStatusIfUpdateWasSuccessful() {
        paymentOperations.verify(billingResult.getPayload()).subscribe(observer);
        scheduler.advanceTimeBy(2, TimeUnit.SECONDS);

        InOrder inOrder = inOrder(api);
        inOrder.verify(api).response(argThat(isApiRequestTo("POST", ApiEndpoints.CHECKOUT_URN.path("token_123"))
                .withContent(UpdateCheckout.fromSuccess(billingResult.getPayload()))));
        inOrder.verify(api).mappedResponse(argThat(isApiRequestTo("GET", ApiEndpoints.CHECKOUT_URN.path("token_123"))));
    }

    @Test
    public void verifyReturnsSuccessStatusIfPaymentWasConfirmed() {
        paymentOperations.verify(billingResult.getPayload()).subscribe(observer);
        scheduler.advanceTimeBy(2, TimeUnit.SECONDS);

        expect(observer.getOnNextEvents().get(0)).toEqual(PurchaseStatus.SUCCESS);
    }

    @Test
    public void verifyReturnsVerificationFailStatusIfPaymentWasNotValid() {
        when(api.mappedResponse(argThat(isApiRequestTo("GET", ApiEndpoints.CHECKOUT_URN.path("token_123")))))
                .thenReturn(failObservable());

        paymentOperations.verify(billingResult.getPayload()).subscribe(observer);
        scheduler.advanceTimeBy(2, TimeUnit.SECONDS);

        expect(observer.getOnNextEvents().get(0)).toEqual(PurchaseStatus.VERIFY_FAIL);
    }

    @Test
    public void pollVerificationUntilNonPendingStatus() {
        when(api.mappedResponse(argThat(isApiRequestTo("GET", ApiEndpoints.CHECKOUT_URN.path("token_123")))))
                .thenReturn(pendingObservable(), successObservable());

        paymentOperations.verify(billingResult.getPayload()).subscribe(observer);

        scheduler.advanceTimeBy(2, TimeUnit.SECONDS);
        expect(observer.getOnNextEvents()).toBeEmpty();
        scheduler.advanceTimeBy(2, TimeUnit.SECONDS);
        expect(observer.getOnNextEvents().get(0)).toEqual(PurchaseStatus.SUCCESS);
        observer.assertTerminalEvent();
    }

    @Test
    public void pollVerificationWithThreeReattemptsIfPaymentIsNotConfirmed() {
        when(api.mappedResponse(argThat(isApiRequestTo("GET", ApiEndpoints.CHECKOUT_URN.path("token_123")))))
                .thenReturn(pendingObservable());

        paymentOperations.verify(billingResult.getPayload()).subscribe(observer);
        scheduler.advanceTimeBy(8, TimeUnit.SECONDS);

        verify(api, times(4)).mappedResponse(argThat(isApiRequestTo("GET", ApiEndpoints.CHECKOUT_URN.path("token_123"))));
        observer.assertTerminalEvent();
    }

    @Test
    public void pollVerificationTimeoutReturnsTimeoutStatus() {
        when(api.mappedResponse(argThat(isApiRequestTo("GET", ApiEndpoints.CHECKOUT_URN.path("token_123")))))
                .thenReturn(pendingObservable());

        paymentOperations.verify(billingResult.getPayload()).subscribe(observer);
        scheduler.advanceTimeBy(8, TimeUnit.SECONDS);

        expect(observer.getOnNextEvents().get(0)).toEqual(PurchaseStatus.VERIFY_TIMEOUT);
    }

    @Test
    public void queryStatusVerifiesPendingSubscription() {
        Payload payload = new Payload("data", "signature");
        setupPendingSubscription(payload);

        paymentOperations.queryStatus().subscribe();
        scheduler.advanceTimeBy(2, TimeUnit.SECONDS);

        verify(tokenStorage).setCheckoutToken("token_123");
        verify(api).response(argThat(isApiRequestTo("POST", ApiEndpoints.CHECKOUT_URN.path("token_123"))
                .withContent(UpdateCheckout.fromSuccess(payload))));
    }

    @Test
    public void verifyClearsTokenWhenFinished() {
        when(api.mappedResponse(argThat(isApiRequestTo("GET", ApiEndpoints.CHECKOUT_URN.path("token_123")))))
                .thenReturn(pendingObservable());

        paymentOperations.verify(billingResult.getPayload()).subscribe(observer);
        scheduler.advanceTimeBy(8, TimeUnit.SECONDS);

        verify(tokenStorage).clear();
    }

    private Observable<CheckoutUpdated> successObservable() {
        return Observable.just(new CheckoutUpdated("successful", "ok", "token_123"));
    }

    private Observable<CheckoutUpdated> failObservable() {
        return Observable.just(new CheckoutUpdated("failed", "error", "token_123"));
    }

    private Observable<CheckoutUpdated> pendingObservable() {
        return Observable.just(new CheckoutUpdated("pending", "working", "token_123"));
    }

    private void setupPendingSubscription(Payload payload) {
        when(billingService.getStatus()).thenReturn(Observable.just(SubscriptionStatus.subscribed("token_123", payload)));
        when(tokenStorage.getCheckoutToken()).thenReturn("token_123");
        when(api.response(argThat(isApiRequestTo("POST", ApiEndpoints.CHECKOUT_URN.path("token_123"))
                .withContent(UpdateCheckout.fromSuccess(payload)))))
                .thenReturn(Observable.just(new ApiResponse(null, HttpStatus.SC_OK, null)));
    }

}