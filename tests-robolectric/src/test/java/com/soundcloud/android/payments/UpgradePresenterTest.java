package com.soundcloud.android.payments;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import com.soundcloud.android.api.ApiResponse;
import com.soundcloud.android.api.TestApiResponses;
import com.soundcloud.android.configuration.ConfigurationOperations;
import com.soundcloud.android.payments.googleplay.BillingResult;
import com.soundcloud.android.payments.googleplay.Payload;
import com.soundcloud.android.payments.googleplay.TestBillingResults;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import com.xtremelabs.robolectric.Robolectric;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import rx.Observable;
import rx.subjects.PublishSubject;

import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

@RunWith(SoundCloudTestRunner.class)
public class UpgradePresenterTest {

    @Mock private PaymentOperations paymentOperations;
    @Mock private PaymentErrorPresenter paymentErrorPresenter;
    @Mock private PaymentErrorView paymentErrorView;
    @Mock private ConfigurationOperations configurationOperations;

    @Mock private AppCompatActivity activity;
    @Mock private ActionBar actionBar;

    private UpgradePresenter controller;
    private View contentView;

    @Before
    public void setUp() throws Exception {
        controller = new UpgradePresenter(paymentOperations, paymentErrorPresenter, configurationOperations,
                Robolectric.application.getResources());
        contentView = LayoutInflater.from(Robolectric.application).inflate(R.layout.upgrade_activity, null, false);
        when(activity.getApplicationContext()).thenReturn(Robolectric.application);
        when(activity.findViewById(android.R.id.content)).thenReturn(contentView);
        when(activity.getSupportActionBar()).thenReturn(actionBar);
        when(paymentOperations.connect(activity)).thenReturn(Observable.just(ConnectionStatus.DISCONNECTED));
    }

    @Test
    public void onCreateConnectsPaymentOperations() {
        controller.onCreate(activity, null);
        verify(paymentOperations).connect(activity);
    }

    @Test
    public void onCreateBindsErrorHandler() {
        controller.onCreate(activity, null);
        verify(paymentErrorPresenter).setActivity(activity);
    }

    @Test
    public void onDestroyDisconnectsPaymentOperations() {
        controller.onDestroy(activity);
        verify(paymentOperations).disconnect();
    }

    @Test
    public void sendsPlayBillingSuccessForVerification() {
        BillingResult billingResult = TestBillingResults.success();
        when(paymentOperations.verify(any(Payload.class))).thenReturn(Observable.just(PurchaseStatus.PENDING));

        controller.onCreate(activity, null);
        controller.handleBillingResult(billingResult);

        verify(paymentOperations).verify(billingResult.getPayload());
    }

    @Test
    public void cancelsTransactionForPlayBillingFailure() {
        when(paymentOperations.cancel(anyString())).thenReturn(Observable.<ApiResponse>empty());

        controller.onCreate(activity, null);
        controller.handleBillingResult(TestBillingResults.cancelled());

        verify(paymentOperations).cancel("payment failed");
        verify(paymentErrorPresenter).showCancelled();
    }

    @Test
    public void cancelsTransactionPlayBillingError() {
        when(paymentOperations.cancel(anyString())).thenReturn(Observable.<ApiResponse>empty());

        controller.onCreate(activity, null);
        controller.handleBillingResult(TestBillingResults.error());

        verify(paymentOperations).cancel(anyString());
    }

    @Test
    public void doesNotQueryProductDetailsIfBillingIsNotSupported() {
        when(paymentOperations.connect(activity)).thenReturn(Observable.just(ConnectionStatus.UNSUPPORTED));

        controller.onCreate(activity, null);

        verify(paymentOperations, never()).queryProduct();
        verify(paymentErrorPresenter).showBillingUnavailable();
    }

    @Test
    public void queriesPurchaseStatusWhenBillingServiceIsReady() {
        when(paymentOperations.connect(activity)).thenReturn(Observable.just(ConnectionStatus.READY));
        when(paymentOperations.queryStatus()).thenReturn(Observable.<PurchaseStatus>empty());

        controller.onCreate(activity, null);

        verify(paymentOperations).queryStatus();
    }

    @Test
    public void queriesProductDetailsWhenPurchaseStatusIsNone() throws Exception {
        when(paymentOperations.connect(activity)).thenReturn(Observable.just(ConnectionStatus.READY));
        when(paymentOperations.queryStatus()).thenReturn(Observable.just(PurchaseStatus.NONE));

        controller.onCreate(activity, null);

        verify(paymentOperations).queryProduct();
    }

    @Test
    public void displayBuyButtonWhenPurchaseStatusIsNone() {
        ProductDetails details = setupExpectedProductDetails();

        controller.onCreate(activity, null);

        expect(getView(R.id.upgrade_buy)).toBeVisible();
        expect(getView(R.id.upgrade_buy)).toBeEnabled();
        expect(getText(R.id.upgrade_buy)).toContain(details.getPrice());
    }

    @Test
    public void disablesBuyButtonWhenClicked() {
        ProductDetails details = setupExpectedProductDetails();
        when(paymentOperations.purchase(details.getId())).thenReturn(Observable.just("token"));
        controller.onCreate(activity, null);

        getView(R.id.upgrade_buy).performClick();

        expect(getView(R.id.upgrade_buy)).toBeDisabled();
    }

    @Test
    public void reEnablesBuyButtonWhenPurchaseIsCancelled() {
        when(paymentOperations.cancel(anyString())).thenReturn(Observable.just(TestApiResponses.ok()));

        controller.onCreate(activity, null);
        controller.handleBillingResult(TestBillingResults.cancelled());

        expect(getView(R.id.upgrade_buy)).toBeEnabled();
    }

    @Test
    public void sendsFailReasonWhenPurchaseIsCancelled() {
        BillingResult result = TestBillingResults.cancelled();
        PublishSubject subject = PublishSubject.create();
        when(paymentOperations.cancel(result.getFailReason())).thenReturn(subject);

        controller.onCreate(activity, null);
        controller.handleBillingResult(TestBillingResults.cancelled());

        expect(subject.hasObservers()).toBeTrue();
    }

    @Test
    public void displaysConnectionErrorIfProductIsNotAvailable() {
        when(paymentOperations.connect(activity)).thenReturn(Observable.just(ConnectionStatus.READY));
        when(paymentOperations.queryStatus()).thenReturn(Observable.just(PurchaseStatus.NONE));
        when(paymentOperations.queryProduct()).thenReturn(Observable.just(ProductStatus.fromNoProduct()));

        controller.onCreate(activity, null);

        verify(paymentErrorPresenter).showConnectionError();
    }

    @Test
    public void requestsConfigurationUpdateWhenPurchaseIsSuccess() {
        when(paymentOperations.verify(any(Payload.class))).thenReturn(Observable.just(PurchaseStatus.SUCCESS));

        controller.onCreate(activity, null);
        controller.handleBillingResult(TestBillingResults.success());

        verify(configurationOperations).update();
    }

    @Test
    public void startsSuccessActivityWhenPurchaseIsSuccess() {
        when(paymentOperations.verify(any(Payload.class))).thenReturn(Observable.just(PurchaseStatus.SUCCESS));

        controller.onCreate(activity, null);
        controller.handleBillingResult(TestBillingResults.success());

        expect(getView(R.id.upgrade_header)).toBeGone();
        expect(getView(R.id.success_header)).toBeVisible();
    }

    @Test
    public void displaysErrorOnVerificationFail() {
        when(paymentOperations.verify(any(Payload.class))).thenReturn(Observable.just(PurchaseStatus.VERIFY_FAIL));

        controller.onCreate(activity, null);
        controller.handleBillingResult(TestBillingResults.success());

        verify(paymentErrorPresenter).showVerifyFail();
    }

    @Test
    public void displaysErrorOnVerificationTimeout() {
        when(paymentOperations.verify(any(Payload.class))).thenReturn(Observable.just(PurchaseStatus.VERIFY_TIMEOUT));

        controller.onCreate(activity, null);
        controller.handleBillingResult(TestBillingResults.success());

        verify(paymentErrorPresenter).showVerifyTimeout();
    }

    private View getView(int id) {
        return contentView.findViewById(id);
    }

    private String getText(int id) {
        return ((TextView) contentView.findViewById(id)).getText().toString();
    }

    @NotNull
    private ProductDetails setupExpectedProductDetails() {
        ProductDetails details = new ProductDetails("id", "product title", "description", "$100");
        when(paymentOperations.connect(activity)).thenReturn(Observable.just(ConnectionStatus.READY));
        when(paymentOperations.queryStatus()).thenReturn(Observable.just(PurchaseStatus.NONE));
        when(paymentOperations.queryProduct()).thenReturn(Observable.just(ProductStatus.fromSuccess(details)));
        return details;
    }

}
