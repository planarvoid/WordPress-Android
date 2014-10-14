package com.soundcloud.android.payments;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.payments.googleplay.PlayBillingService;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.Observable;

import android.app.Activity;

@RunWith(SoundCloudTestRunner.class)
public class PaymentOperationsTest {

    @Mock PlayBillingService billingService;
    @Mock Activity activity;

    private PaymentOperations paymentOperations;

    @Before
    public void setUp() throws Exception {
        paymentOperations = new PaymentOperations(billingService);
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
    public void requestsProductDetailsForSubscriptionId() {
        when(billingService.getDetails(anyString())).thenReturn(Observable.<ProductDetails>empty());

        paymentOperations.queryProductDetails().subscribe();

        verify(billingService).getDetails(eq("consumer_subscription"));
    }

    @Test
    public void returnsProductDetailsFromBillingService()  {
        ProductDetails details = new ProductDetails("id", "Subscription", "Blah", "$100");
        when(billingService.getDetails(anyString())).thenReturn(Observable.just(details));

        ProductDetails result = paymentOperations.queryProductDetails().toBlocking().first();

        expect(result).toBe(details);
    }

}