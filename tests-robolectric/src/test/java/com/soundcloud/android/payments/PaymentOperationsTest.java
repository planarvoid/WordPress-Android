package com.soundcloud.android.payments;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.testsupport.matchers.RequestMatchers.isApiRequestTo;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import com.soundcloud.android.api.ApiClientRx;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.ApiRequest;
import com.soundcloud.android.api.ApiResponse;
import com.soundcloud.android.payments.googleplay.BillingService;
import com.soundcloud.android.payments.googleplay.SubscriptionStatus;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.Observable;
import rx.observers.TestObserver;
import rx.schedulers.Schedulers;

import android.app.Activity;

import java.util.ArrayList;

@RunWith(SoundCloudTestRunner.class)
public class PaymentOperationsTest {

    @Mock private ApiClientRx api;
    @Mock private BillingService billingService;
    @Mock private TokenStorage tokenStorage;
    @Mock private Activity activity;

    private PaymentOperations paymentOperations;

    @Before
    public void setUp() throws Exception {
        paymentOperations = new PaymentOperations(Schedulers.immediate(), api, billingService, tokenStorage);
        when(api.mappedResponse(argThat(isApiRequestTo("GET", ApiEndpoints.PRODUCTS.path())), eq(AvailableProducts.class)))
                .thenReturn(availableProductsObservable());
        when(api.mappedResponse(argThat(isApiRequestTo("POST", ApiEndpoints.CHECKOUT.path())
                .withContent(new StartCheckout("product_id"))), eq(CheckoutStarted.class)))
                .thenReturn(checkoutResultObservable());
        when(api.response(argThat(isApiRequestTo("POST", ApiEndpoints.CHECKOUT_URN.path("token_123"))
                .withContent(UpdateCheckout.fromFailure("user cancelled")))))
                .thenReturn(Observable.<ApiResponse>empty());
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
    public void queryStatusReturnsNoneIfBillingServiceReturnsNoExistingSubscription() {
        when(billingService.getStatus()).thenReturn(Observable.just(SubscriptionStatus.notSubscribed()));
        TestObserver<PurchaseStatus> observer = new TestObserver<>();

        paymentOperations.queryStatus().subscribe(observer);

        expect(observer.getOnNextEvents()).toContainExactly(PurchaseStatus.NONE);
    }

    @Test
    public void queryProductFetchesProductIdFromApi() {
        when(billingService.getDetails(anyString())).thenReturn(Observable.<ProductDetails>empty());

        paymentOperations.queryProduct().subscribe();

        verify(api).mappedResponse(argThat(isApiRequestTo("GET", ApiEndpoints.PRODUCTS.path())), eq(AvailableProducts.class));
    }

    @Test
    public void queryProductReturnsFailedStatusWhenNoProductsAreAvailable() {
        when(api.mappedResponse(any(ApiRequest.class), eq(AvailableProducts.class))).thenReturn(noProductsObservable());
        TestObserver<ProductStatus> observer = new TestObserver<>();

        paymentOperations.queryProduct().subscribe(observer);

        ProductStatus result = observer.getOnNextEvents().get(0);
        expect(result.isSuccess()).toBeFalse();
    }

    @Test
    public void requestsProductDetailsForId() {
        when(billingService.getDetails(anyString())).thenReturn(Observable.<ProductDetails>empty());

        paymentOperations.queryProduct().subscribe();

        verify(billingService).getDetails(eq("product_id"));
    }

    @Test
    public void returnsProductDetailsFromBillingService()  {
        ProductDetails details = new ProductDetails("id", "Subscription", "Blah", "$100");
        when(billingService.getDetails(anyString())).thenReturn(Observable.just(details));
        TestObserver<ProductStatus> observer = new TestObserver<>();

        paymentOperations.queryProduct().subscribe(observer);

        ProductStatus result = observer.getOnNextEvents().get(0);
        expect(result.isSuccess()).toBeTrue();
        expect(result.getDetails()).toBe(details);
    }

    @Test
    public void purchasePostsCheckoutStart() {
        paymentOperations.purchase("product_id").subscribe();

        verify(api).mappedResponse(argThat(isApiRequestTo("POST", ApiEndpoints.CHECKOUT.path())
                .withContent(new StartCheckout("product_id"))), eq(CheckoutStarted.class));
    }

    @Test
    public void savesCheckoutTokenFromCheckoutStart() {
        paymentOperations.purchase("product_id").subscribe();

        verify(tokenStorage).setCheckoutToken("token_123");
    }

    @Test
    public void startsPurchaseWithTokenFromCheckoutStart() {
        paymentOperations.purchase("product_id").subscribe();

        verify(billingService).startPurchase("product_id", "token_123");
    }

    @Test
    public void cancelPostsCheckoutFailure() {
        when(tokenStorage.getCheckoutToken()).thenReturn("token_123");

        paymentOperations.cancel("user cancelled").subscribe();

        verify(api).response(argThat(isApiRequestTo("POST", ApiEndpoints.CHECKOUT_URN.path("token_123"))
                .withContent(UpdateCheckout.fromFailure("user cancelled"))));
    }

    @Test
    public void cancelClearsToken() {
        when(tokenStorage.getCheckoutToken()).thenReturn("token_123");

        paymentOperations.cancel("user cancelled").subscribe();

        verify(tokenStorage).clear();
    }

    private Observable<AvailableProducts> availableProductsObservable() {
        AvailableProducts products = new AvailableProducts(Lists.newArrayList(new AvailableProducts.Product("product_id", "mid_tier")));
        return Observable.just(products);
    }

    private Observable<CheckoutStarted> checkoutResultObservable() {
        return Observable.just(new CheckoutStarted("token_123"));
    }

    private Observable<AvailableProducts> noProductsObservable() {
        return Observable.just(new AvailableProducts(new ArrayList<AvailableProducts.Product>()));
    }

}