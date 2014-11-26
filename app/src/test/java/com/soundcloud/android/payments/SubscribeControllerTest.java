package com.soundcloud.android.payments;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import com.soundcloud.android.api.ApiResponse;
import com.soundcloud.android.payments.googleplay.BillingResult;
import com.soundcloud.android.payments.googleplay.Payload;
import com.soundcloud.android.payments.googleplay.TestBillingResults;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.xtremelabs.robolectric.Robolectric;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.Observable;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

@RunWith(SoundCloudTestRunner.class)
public class SubscribeControllerTest {

    @Mock private PaymentOperations paymentOperations;
    @Mock private Activity activity;

    private SubscribeController controller;
    private View contentView;

    @Before
    public void setUp() throws Exception {
        controller = new SubscribeController(Robolectric.application, paymentOperations);
        contentView = LayoutInflater.from(Robolectric.application).inflate(R.layout.payments_activity, null, false);
        when(activity.findViewById(anyInt())).thenReturn(contentView);
        when(paymentOperations.connect(activity)).thenReturn(Observable.just(ConnectionStatus.DISCONNECTED));
    }

    @Test
    public void onCreateSetsActivityContentView() {
        controller.onCreate(activity);
        verify(activity).setContentView(R.layout.payments_activity);
    }

    @Test
    public void onCreateConnectsPaymentOperations() {
        controller.onCreate(activity);
        verify(paymentOperations).connect(activity);
    }

    @Test
    public void onDestroyDisconnectsPaymentOperations() {
        controller.onDestroy();
        verify(paymentOperations).disconnect();
    }

    @Test
    public void sendsPlayBillingSuccessForVerification() {
        BillingResult billingResult = TestBillingResults.success();
        when(paymentOperations.verify(any(Payload.class))).thenReturn(Observable.just(PurchaseStatus.PENDING));

        controller.handleBillingResult(billingResult);

        verify(paymentOperations).verify(billingResult.getPayload());
    }

    @Test
    public void cancelsTransactionForPlayBillingFailure() {
        when(paymentOperations.cancel(anyString())).thenReturn(Observable.<ApiResponse>empty());

        controller.handleBillingResult(TestBillingResults.cancelled());

        verify(paymentOperations).cancel("user cancelled");
    }

    @Test
    public void cancelsTransactionPlayBillingError() {
        when(paymentOperations.cancel(anyString())).thenReturn(Observable.<ApiResponse>empty());

        controller.handleBillingResult(TestBillingResults.error());

        verify(paymentOperations).cancel(anyString());
    }

    @Test
    public void doesNotQueryProductDetailsIfBillingIsNotSupported() {
        when(paymentOperations.connect(activity)).thenReturn(Observable.just(ConnectionStatus.UNSUPPORTED));

        controller.onCreate(activity);

        verify(paymentOperations, never()).queryProduct();
    }

    @Test
    public void queriesPurchaseStatusWhenBillingServiceIsReady() {
        when(paymentOperations.connect(activity)).thenReturn(Observable.just(ConnectionStatus.READY));
        when(paymentOperations.queryStatus()).thenReturn(Observable.<PurchaseStatus>empty());

        controller.onCreate(activity);

        verify(paymentOperations).queryStatus();
    }

    @Test
    public void queriesProductDetailsWhenPurchaseStatusIsNone() throws Exception {
        when(paymentOperations.connect(activity)).thenReturn(Observable.just(ConnectionStatus.READY));
        when(paymentOperations.queryStatus()).thenReturn(Observable.just(PurchaseStatus.NONE));

        controller.onCreate(activity);

        verify(paymentOperations).queryProduct();
    }

    @Test
    public void displaysPurchaseOptionsWhenPurchaseStatusIsNone() {
        ProductDetails details = new ProductDetails("id", "product title", "description", "$100");
        when(paymentOperations.connect(activity)).thenReturn(Observable.just(ConnectionStatus.READY));
        when(paymentOperations.queryStatus()).thenReturn(Observable.just(PurchaseStatus.NONE));
        when(paymentOperations.queryProduct()).thenReturn(Observable.just(ProductStatus.fromSuccess(details)));

        controller.onCreate(activity);

        expect(getText(R.id.subscribe_title)).toEqual(details.getTitle());
        expect(getText(R.id.subscribe_description)).toEqual(details.getDescription());
        expect(getText(R.id.subscribe_price)).toEqual(details.getPrice());
    }

    private String getText(int id) {
        return ((TextView) contentView.findViewById(id)).getText().toString();
    }

}