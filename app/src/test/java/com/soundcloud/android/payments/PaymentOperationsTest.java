package com.soundcloud.android.payments;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.matchers.SoundCloudMatchers.isMobileApiRequestTo;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.ApiResponse;
import com.soundcloud.android.api.ApiScheduler;
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
import org.mockito.Mock;
import rx.Observable;

import android.app.Activity;

import java.util.ArrayList;

@RunWith(SoundCloudTestRunner.class)
public class PaymentOperationsTest {

    @Mock private ApiScheduler apiScheduler;
    @Mock private BillingService billingService;
    @Mock private PaymentStorage paymentStorage;
    @Mock private Activity activity;

    private PaymentOperations paymentOperations;
    private BillingResult billingResult;

    @Before
    public void setUp() throws Exception {
        paymentOperations = new PaymentOperations(apiScheduler, billingService, paymentStorage);
        billingResult = TestBillingResults.success();
        when(apiScheduler.mappedResponse(argThat(isMobileApiRequestTo("GET", ApiEndpoints.PRODUCTS.path()))))
                .thenReturn(availableProductsObservable());
        when(apiScheduler.mappedResponse(argThat(isMobileApiRequestTo("POST", ApiEndpoints.CHECKOUT.path())
                .withContent(new StartCheckout("product_id")))))
                .thenReturn(checkoutResultObservable());
        when(apiScheduler.response(argThat(isMobileApiRequestTo("POST", ApiEndpoints.CHECKOUT_URN.path("token_123"))
                .withContent(UpdateCheckout.fromFailure("user cancelled")))))
                .thenReturn(Observable.<ApiResponse>empty());
        when(apiScheduler.response(argThat(isMobileApiRequestTo("POST", ApiEndpoints.CHECKOUT_URN.path("token_123"))
                .withContent(UpdateCheckout.fromSuccess(billingResult.getPayload())))))
                .thenReturn(Observable.just(new ApiResponse(null, HttpStatus.SC_OK, null)));
    }

    @Test
    public void connectOpensConnection() {
        paymentOperations.connect(activity);
        verify(billingService).openConnection(activity);
    }

    @Test
    public void disconnectClosesConnection() {
        paymentOperations.disconnect();
        verify(billingService).closeConnection();
    }

    @Test
    public void fetchesProductIdFromProductsEndpoint() {
        paymentOperations.queryProduct().subscribe();

        verify(apiScheduler).mappedResponse(argThat(isMobileApiRequestTo("GET", ApiEndpoints.PRODUCTS.path())));
    }

    @Test
    public void returnsFailedStatusWhenNoProductsAreAvailable() {
        when(apiScheduler.mappedResponse(any(ApiRequest.class))).thenReturn(noProductsObservable());

        ProductStatus result = paymentOperations.queryProduct().toBlocking().firstOrDefault(null);

        expect(result.isSuccess()).toBeFalse();
    }

    @Test
    public void requestsProductDetailsForSubscriptionId() {
        when(billingService.getDetails(anyString())).thenReturn(Observable.<ProductDetails>empty());

        paymentOperations.queryProduct().subscribe();

        verify(billingService).getDetails(eq("product_id"));
    }

    @Test
    public void returnsProductDetailsFromBillingService()  {
        ProductDetails details = new ProductDetails("id", "Subscription", "Blah", "$100");
        when(billingService.getDetails(anyString())).thenReturn(Observable.just(details));

        ProductStatus result = paymentOperations.queryProduct().toBlocking().firstOrDefault(null);

        expect(result.isSuccess()).toBeTrue();
        expect(result.getDetails()).toBe(details);
    }

    @Test
    public void queryStatusReturnsNoneIfBillingServiceGivesNotSubscribed() {
        when(billingService.getStatus()).thenReturn(Observable.just(SubscriptionStatus.notSubscribed()));

        PurchaseStatus status = paymentOperations.queryStatus().toBlocking().firstOrDefault(null);

        expect(status).toEqual(PurchaseStatus.NONE);
    }

    @Test
    public void queryStatusReturnsVerifyingIfBillingServiceGivesSubscription() {
        setupPendingSubscription(new Payload("data", "signature"));

        PurchaseStatus status = paymentOperations.queryStatus().toBlocking().firstOrDefault(null);

        expect(status).toEqual(PurchaseStatus.VERIFYING);
    }

    @Test
    public void queryStatusVerifiesPendingSubscription() {
        Payload payload = new Payload("data", "signature");
        setupPendingSubscription(payload);

        paymentOperations.queryStatus().toBlocking().firstOrDefault(null);

        verify(paymentStorage).setCheckoutToken("token_123");
        verify(apiScheduler).response(argThat(isMobileApiRequestTo("POST", ApiEndpoints.CHECKOUT_URN.path("token_123"))
                .withContent(UpdateCheckout.fromSuccess(payload))));
    }

    @Test
    public void postsCheckoutStart() {
        paymentOperations.purchase("product_id").subscribe();

        verify(apiScheduler).mappedResponse(argThat(isMobileApiRequestTo("POST", ApiEndpoints.CHECKOUT.path())
                .withContent(new StartCheckout("product_id"))));
    }

    @Test
    public void beginsPurchaseWithTokenFromCheckoutStart() {
        paymentOperations.purchase("product_id").subscribe();

        verify(billingService).startPurchase("product_id", "token_123");
    }

    @Test
    public void savesCheckoutTokenFromCheckoutStart() {
        paymentOperations.purchase("product_id").subscribe();

        verify(paymentStorage).setCheckoutToken("token_123");
    }

    @Test
    public void postsCheckoutSuccess() {
        when(paymentStorage.getCheckoutToken()).thenReturn("token_123");

        paymentOperations.verify(billingResult.getPayload());

        verify(apiScheduler).response(argThat(isMobileApiRequestTo("POST", ApiEndpoints.CHECKOUT_URN.path("token_123"))
                .withContent(UpdateCheckout.fromSuccess(billingResult.getPayload()))));
    }

    @Test
    public void verifyReturnsVerifyingStatusIfUpdateWasSuccessful() {
        when(paymentStorage.getCheckoutToken()).thenReturn("token_123");

        PurchaseStatus status = paymentOperations.verify(billingResult.getPayload()).toBlocking().firstOrDefault(null);

        expect(status).toEqual(PurchaseStatus.VERIFYING);
    }

    @Test
    public void verifyReturnsFailureStatusIfUpdateFailed() {
        when(apiScheduler.response(argThat(isMobileApiRequestTo("POST", ApiEndpoints.CHECKOUT_URN.path("token_123"))
                .withContent(UpdateCheckout.fromSuccess(billingResult.getPayload())))))
                .thenReturn(Observable.just(new ApiResponse(null, HttpStatus.SC_FORBIDDEN, null)));
        when(paymentStorage.getCheckoutToken()).thenReturn("token_123");

        PurchaseStatus status = paymentOperations.verify(billingResult.getPayload()).toBlocking().firstOrDefault(null);

        expect(status).toEqual(PurchaseStatus.FAILURE);
    }

    @Test
    public void postsCheckoutFailure() {
        when(paymentStorage.getCheckoutToken()).thenReturn("token_123");

        paymentOperations.cancel("user cancelled").subscribe();

        verify(apiScheduler).response(argThat(isMobileApiRequestTo("POST", ApiEndpoints.CHECKOUT_URN.path("token_123"))
                .withContent(UpdateCheckout.fromFailure("user cancelled"))));
    }

    private Observable<AvailableProducts> availableProductsObservable() {
        AvailableProducts products = new AvailableProducts(Lists.newArrayList(new AvailableProducts.Product("product_id")));
        return Observable.just(products);
    }

    private Observable<CheckoutStarted> checkoutResultObservable() {
        return Observable.just(new CheckoutStarted("token_123"));
    }

    private Observable<AvailableProducts> noProductsObservable() {
        return Observable.just(new AvailableProducts(new ArrayList<AvailableProducts.Product>()));
    }

    private void setupPendingSubscription(Payload payload) {
        when(billingService.getStatus()).thenReturn(Observable.just(SubscriptionStatus.subscribed("token_123", payload)));
        when(paymentStorage.getCheckoutToken()).thenReturn("token_123");
        when(apiScheduler.response(argThat(isMobileApiRequestTo("POST", ApiEndpoints.CHECKOUT_URN.path("token_123"))
                .withContent(UpdateCheckout.fromSuccess(payload)))))
                .thenReturn(Observable.just(new ApiResponse(null, HttpStatus.SC_OK, null)));
    }

}