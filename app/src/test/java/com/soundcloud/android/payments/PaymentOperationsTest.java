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
import com.soundcloud.android.api.RxHttpClient;
import com.soundcloud.android.payments.googleplay.PlayBillingService;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.Observable;

import android.app.Activity;

import java.util.ArrayList;

@RunWith(SoundCloudTestRunner.class)
public class PaymentOperationsTest {

    @Mock RxHttpClient rxHttpClient;
    @Mock PlayBillingService billingService;
    @Mock Activity activity;

    private PaymentOperations paymentOperations;

    @Before
    public void setUp() throws Exception {
        paymentOperations = new PaymentOperations(rxHttpClient, billingService);
        when(rxHttpClient.<AvailableProducts>fetchModels(argThat(isMobileApiRequestTo("GET", ApiEndpoints.PRODUCTS.path()))))
                .thenReturn(availableProductsObservable());
        when(rxHttpClient.<CheckoutResult>fetchModels(argThat(isMobileApiRequestTo("POST", ApiEndpoints.CHECKOUT.path()))))
                .thenReturn(checkoutResultObservable());
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
        paymentOperations.queryProductDetails().subscribe();

        verify(rxHttpClient).fetchModels(argThat(isMobileApiRequestTo("GET", ApiEndpoints.PRODUCTS.path())));
    }

    @Test
    public void returnsFailedStatusWhenNoProductsAreAvailable() {
        when(rxHttpClient.<AvailableProducts>fetchModels(any(ApiRequest.class))).thenReturn(noProductsObservable());

        ProductStatus result = paymentOperations.queryProductDetails().toBlocking().first();

        expect(result.isSuccess()).toBeFalse();
    }

    @Test
    public void requestsProductDetailsForSubscriptionId() {
        when(billingService.getDetails(anyString())).thenReturn(Observable.<ProductDetails>empty());

        paymentOperations.queryProductDetails().subscribe();

        verify(billingService).getDetails(eq("product_id"));
    }

    @Test
    public void returnsProductDetailsFromBillingService()  {
        ProductDetails details = new ProductDetails("id", "Subscription", "Blah", "$100");
        when(billingService.getDetails(anyString())).thenReturn(Observable.just(details));

        ProductStatus result = paymentOperations.queryProductDetails().toBlocking().first();

        expect(result.isSuccess()).toBeTrue();
        expect(result.getDetails()).toBe(details);
    }

    @Test
    public void postsCheckoutStart() {
        paymentOperations.buy("product_id").subscribe();

        verify(rxHttpClient).fetchModels(argThat(isMobileApiRequestTo("POST", ApiEndpoints.CHECKOUT.path())));
    }

    @Test
    public void returnsCheckoutTokenFromCheckoutStart() {
        String checkoutToken = paymentOperations.buy("product_id").toBlocking().first();

        expect(checkoutToken).toEqual("token_123");
    }

    private Observable<AvailableProducts> availableProductsObservable() {
        AvailableProducts products = new AvailableProducts(Lists.newArrayList(new AvailableProducts.Product("product_id")));
        return Observable.just(products);
    }

    private Observable<CheckoutResult> checkoutResultObservable() {
        return Observable.just(new CheckoutResult("token_123"));
    }

    private Observable<AvailableProducts> noProductsObservable() {
        return Observable.just(new AvailableProducts(new ArrayList<AvailableProducts.Product>()));
    }

}