package com.soundcloud.android.payments;

import static com.soundcloud.android.Expect.expect;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.ApiResponse;
import com.soundcloud.android.api.TestApiResponses;
import com.soundcloud.android.configuration.ConfigurationOperations;
import com.soundcloud.android.payments.googleplay.BillingResult;
import com.soundcloud.android.payments.googleplay.Payload;
import com.soundcloud.android.payments.googleplay.TestBillingResults;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import rx.Observable;
import rx.observers.TestObserver;
import rx.subjects.PublishSubject;

import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;

@RunWith(SoundCloudTestRunner.class)
public class UpgradePresenterTest {

    private static final String PRODUCT_ID = "id";
    private static final String PRICE = "$100";

    @Mock private PaymentOperations paymentOperations;
    @Mock private PaymentErrorPresenter paymentErrorPresenter;
    @Mock private PaymentErrorView paymentErrorView;
    @Mock private UpgradeView upgradeView;
    @Mock private ConfigurationOperations configurationOperations;

    @Mock private AppCompatActivity activity;
    @Mock private ActionBar actionBar;
    @Captor private ArgumentCaptor<UpgradeView.Listener> listenerCaptor;

    private UpgradePresenter controller;

    private TestObserver testObserver;

    @Before
    public void setUp() {
        testObserver = new TestObserver();
        controller = new UpgradePresenter(paymentOperations, paymentErrorPresenter, configurationOperations, upgradeView);
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
    public void onCreateWithEmptyTransactionStateShowsBuyButton() {
        setupExpectedProductDetails();
        when(activity.getLastCustomNonConfigurationInstance()).thenReturn(new TransactionState(null, null));

        controller.onCreate(activity, null);

        verify(upgradeView).showBuyButton(PRICE);
    }

    @Test
    public void onCreateWithExistingStatusObservableShw() {
        setupExpectedProductDetails();
        when(activity.getLastCustomNonConfigurationInstance()).thenReturn(new TransactionState(null, null));

        controller.onCreate(activity, null);

        verify(upgradeView).showBuyButton(PRICE);
    }

    @Test
    public void onCreateWithPurchaseStateDoesNotShowBuyButton() {
        when(activity.getLastCustomNonConfigurationInstance()).thenReturn(new TransactionState(Observable.<String>never(), null));

        controller.onCreate(activity, null);

        verify(upgradeView, never()).showBuyButton(anyString());
    }

    @Test
    public void restoringPurchaseStateWithErrorDisplaysError() {
        setupSuccessfulConnection();
        Throwable error = new Throwable();
        when(activity.getLastCustomNonConfigurationInstance()).thenReturn(new TransactionState(Observable.<String>error(error), null));

        controller.onCreate(activity, null);

        verify(paymentErrorPresenter).onError(error);
    }

    @Test
    public void restoringVerificationObservableShowsSuccess() {
        setupSuccessfulConnection();
        when(activity.getLastCustomNonConfigurationInstance()).thenReturn(new TransactionState(null, Observable.just(PurchaseStatus.SUCCESS)));

        controller.onCreate(activity, null);

        verify(upgradeView).showSuccess();
        verify(configurationOperations).update();
    }

    @Test
    public void onCreateWithVerifyTimeoutObservableShowsFailure() {
        setupSuccessfulConnection();
        when(activity.getLastCustomNonConfigurationInstance()).thenReturn(new TransactionState(null, Observable.just(PurchaseStatus.VERIFY_TIMEOUT)));

        controller.onCreate(activity, null);

        verify(paymentErrorPresenter).showVerifyTimeout();
    }

    @Test
    public void getStateWithNoObservablesReturnsEmptyTransactionState() {
        controller.onCreate(activity, null);

        final TransactionState state = controller.getState();

        expect(state.isTransactionInProgress()).toBeFalse();
    }

    @Test
    public void getStateWithPurchaseObservableReturnsPurchasingState() {
        setupExpectedProductDetails();
        Observable<String> purchase = Observable.just("token");
        when(paymentOperations.purchase(PRODUCT_ID)).thenReturn(purchase);

        controller.onCreate(activity, null);
        controller.startPurchase();
        final TransactionState state = controller.getState();

        expect(state.isRetrievingStatus()).toBeFalse();
        state.purchase().subscribe(testObserver);
        expect(testObserver.getOnNextEvents()).toContainExactly("token");
    }

    @Test
    public void getStateWithStatusObservableFromOnCreateReturnsStatus() {
        final Observable<PurchaseStatus> status = Observable.just(PurchaseStatus.NONE);
        when(paymentOperations.connect(activity)).thenReturn(Observable.just(ConnectionStatus.READY));
        when(paymentOperations.queryStatus()).thenReturn(status);

        controller.onCreate(activity, null);

        final TransactionState state = controller.getState();
        expect(state.isRetrievingStatus()).toBeTrue();
        state.status().subscribe(testObserver);
        expect(testObserver.getOnNextEvents()).toContainExactly(PurchaseStatus.NONE);
    }

    @Test
    public void getStateWithStatusObservableFromBillingResultReturnsStatus() {
        final BillingResult success = TestBillingResults.success();
        final Observable<PurchaseStatus> status = Observable.just(PurchaseStatus.SUCCESS);
        when(paymentOperations.purchase(PRODUCT_ID)).thenReturn(Observable.just("token"));
        when(paymentOperations.verify(success.getPayload())).thenReturn(status);

        controller.handleBillingResult(success);

        final TransactionState state = controller.getState();
        expect(state.isRetrievingStatus()).toBeTrue();
        state.status().subscribe(testObserver);
        expect(testObserver.getOnNextEvents()).toContainExactly(PurchaseStatus.SUCCESS);
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
    public void purchaseCancellationWithNoProductSetsUpProduct() {
        setupExpectedProductDetails();
        when(activity.getLastCustomNonConfigurationInstance()).thenReturn(new TransactionState(Observable.<String>never(), null));
        when(paymentOperations.cancel(anyString())).thenReturn(Observable.<ApiResponse>empty());
        controller.onCreate(activity, null);

        controller.handleBillingResult(TestBillingResults.cancelled());

        verify(upgradeView).showBuyButton(PRICE);
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
    public void queriesProductDetailsWhenPurchaseStatusIsNone() {
        when(paymentOperations.connect(activity)).thenReturn(Observable.just(ConnectionStatus.READY));
        when(paymentOperations.queryStatus()).thenReturn(Observable.just(PurchaseStatus.NONE));

        controller.onCreate(activity, null);

        verify(paymentOperations).queryProduct();
    }

    @Test
    public void displayBuyButtonWhenPurchaseStatusIsNone() {
        setupExpectedProductDetails();

        controller.onCreate(activity, null);

        verify(upgradeView).showBuyButton(PRICE);
    }

    @Test
    public void disablesBuyButtonWhenClicked() {
        ProductDetails details = setupExpectedProductDetails();
        when(paymentOperations.purchase(details.getId())).thenReturn(Observable.just("token"));
        controller.onCreate(activity, null);

        verify(upgradeView).setupContentView(eq(activity), listenerCaptor.capture());
        listenerCaptor.getValue().startPurchase();

        verify(upgradeView).disableBuyButton();
    }

    @Test
    public void reEnablesBuyButtonWhenPurchaseIsCancelled() {
        setupExpectedProductDetails();
        when(paymentOperations.cancel(anyString())).thenReturn(Observable.just(TestApiResponses.ok()));

        controller.onCreate(activity, null);
        controller.handleBillingResult(TestBillingResults.cancelled());

        verify(upgradeView).enableBuyButton();
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

        verify(upgradeView).showSuccess();
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

    private void setupSuccessfulConnection() {
        when(paymentOperations.connect(activity)).thenReturn(Observable.just(ConnectionStatus.READY));
    }

    @NotNull
    private ProductDetails setupExpectedProductDetails() {
        ProductDetails details = new ProductDetails(PRODUCT_ID, "product title", "description", PRICE);
        when(paymentOperations.connect(activity)).thenReturn(Observable.just(ConnectionStatus.READY));
        when(paymentOperations.queryStatus()).thenReturn(Observable.just(PurchaseStatus.NONE));
        when(paymentOperations.queryProduct()).thenReturn(Observable.just(ProductStatus.fromSuccess(details)));
        return details;
    }

}
