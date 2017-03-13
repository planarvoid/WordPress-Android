package com.soundcloud.android.payments;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.api.ApiResponse;
import com.soundcloud.android.payments.googleplay.BillingResult;
import com.soundcloud.android.payments.googleplay.Payload;
import com.soundcloud.android.payments.googleplay.TestBillingResults;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import rx.Observable;
import rx.observers.TestObserver;
import rx.subjects.PublishSubject;

import android.support.v4.app.FragmentManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;

public class NativeConversionPresenterTest extends AndroidUnitTest {

    private static final String PRODUCT_ID = "id";
    private static final String PRICE = "$100";

    @Mock private NativePaymentOperations paymentOperations;
    @Mock private PaymentErrorPresenter paymentErrorPresenter;
    @Mock private PaymentErrorView paymentErrorView;
    @Mock private ConversionView conversionView;
    @Mock private Navigator navigator;

    @Mock private AppCompatActivity activity;
    @Mock private ActionBar actionBar;
    @Captor private ArgumentCaptor<ConversionView.Listener> listenerCaptor;

    private NativeConversionPresenter presenter;

    private TestObserver testObserver;

    @Before
    public void setUp() {
        testObserver = new TestObserver();
        when(activity.getSupportFragmentManager()).thenReturn(mock(FragmentManager.class));
        presenter = new NativeConversionPresenter(paymentOperations,
                                                  paymentErrorPresenter,
                                                  conversionView,
                                                  new TestEventBus(),
                                                  navigator);
        when(paymentOperations.connect(activity)).thenReturn(Observable.just(ConnectionStatus.DISCONNECTED));
    }

    @Test
    public void onCreateConnectsPaymentOperations() {
        presenter.onCreate(activity, null);
        verify(paymentOperations).connect(activity);
    }

    @Test
    public void onCreateBindsErrorHandler() {
        presenter.onCreate(activity, null);
        verify(paymentErrorPresenter).setActivity(activity);
    }

    @Test
    public void onCreateWithEmptyTransactionStateShowsBuyButton() {
        setupExpectedProductDetails();
        when(activity.getLastCustomNonConfigurationInstance()).thenReturn(new TransactionState(null, null));

        presenter.onCreate(activity, null);

        verify(conversionView).showDetails(PRICE);
    }

    @Test
    public void onCreateWithExistingStatusObservableShw() {
        setupExpectedProductDetails();
        when(activity.getLastCustomNonConfigurationInstance()).thenReturn(new TransactionState(null, null));

        presenter.onCreate(activity, null);

        verify(conversionView).showDetails(PRICE);
    }

    @Test
    public void onCreateWithPurchaseStateDoesNotShowBuyButton() {
        when(activity.getLastCustomNonConfigurationInstance()).thenReturn(new TransactionState(Observable.never(),
                                                                                               null));
        presenter.onCreate(activity, null);

        verify(conversionView, never()).showDetails(anyString());
        verify(conversionView, never()).enableBuyButton();
    }

    @Test
    public void restoringPurchaseStateWithErrorDisplaysError() {
        setupSuccessfulConnection();
        Throwable error = new Throwable();
        when(activity.getLastCustomNonConfigurationInstance()).thenReturn(new TransactionState(Observable.error(
                error), null));

        presenter.onCreate(activity, null);

        verify(paymentErrorPresenter).onError(error);
    }

    @Test
    public void restoringVerificationObservableTriggersSuccess() {
        setupSuccessfulConnection();
        when(activity.getLastCustomNonConfigurationInstance()).thenReturn(new TransactionState(null,
                                                                                               Observable.just(
                                                                                                       PurchaseStatus.SUCCESS)));

        presenter.onCreate(activity, null);

        verify(navigator).resetForAccountUpgrade(activity);
    }

    @Test
    public void onCreateWithVerifyTimeoutObservableShowsFailure() {
        setupSuccessfulConnection();
        when(activity.getLastCustomNonConfigurationInstance()).thenReturn(new TransactionState(null,
                                                                                               Observable.just(
                                                                                                       PurchaseStatus.VERIFY_TIMEOUT)));

        presenter.onCreate(activity, null);

        verify(paymentErrorPresenter).showVerifyTimeout();
    }

    @Test
    public void getStateWithNoObservablesReturnsEmptyTransactionState() {
        presenter.onCreate(activity, null);

        final TransactionState state = presenter.getState();

        assertThat(state.isTransactionInProgress()).isFalse();
    }

    @Test
    public void getStateWithPurchaseObservableReturnsPurchasingState() {
        setupExpectedProductDetails();
        Observable<String> purchase = Observable.just("token");
        when(paymentOperations.purchase(PRODUCT_ID)).thenReturn(purchase);

        presenter.onCreate(activity, null);
        presenter.onPurchasePrimary();
        final TransactionState state = presenter.getState();

        assertThat(state.isRetrievingStatus()).isFalse();
        state.purchase().subscribe(testObserver);
        assertThat(testObserver.getOnNextEvents()).containsExactly("token");
    }

    @Test
    public void getStateWithStatusObservableFromOnCreateReturnsStatus() {
        final Observable<PurchaseStatus> status = Observable.just(PurchaseStatus.NONE);
        when(paymentOperations.connect(activity)).thenReturn(Observable.just(ConnectionStatus.READY));
        when(paymentOperations.queryStatus()).thenReturn(status);
        when(paymentOperations.queryProduct()).thenReturn(Observable.never());

        presenter.onCreate(activity, null);

        final TransactionState state = presenter.getState();
        assertThat(state.isRetrievingStatus()).isTrue();
        state.status().subscribe(testObserver);
        assertThat(testObserver.getOnNextEvents()).containsExactly(PurchaseStatus.NONE);
    }

    @Test
    public void getStateWithStatusObservableFromBillingResultReturnsStatus() {
        final BillingResult success = TestBillingResults.success();
        final Observable<PurchaseStatus> status = Observable.just(PurchaseStatus.SUCCESS);
        when(paymentOperations.purchase(PRODUCT_ID)).thenReturn(Observable.just("token"));
        when(paymentOperations.verify(success.getPayload())).thenReturn(status);

        presenter.handleBillingResult(success);

        final TransactionState state = presenter.getState();
        assertThat(state.isRetrievingStatus()).isTrue();
        state.status().subscribe(testObserver);
        assertThat(testObserver.getOnNextEvents()).containsExactly(PurchaseStatus.SUCCESS);
    }

    @Test
    public void onDestroyDisconnectsPaymentOperations() {
        presenter.onDestroy(activity);
        verify(paymentOperations).disconnect();
    }

    @Test
    public void sendsPlayBillingSuccessForVerification() {
        BillingResult billingResult = TestBillingResults.success();
        when(paymentOperations.verify(any(Payload.class))).thenReturn(Observable.just(PurchaseStatus.PENDING));

        presenter.onCreate(activity, null);
        presenter.handleBillingResult(billingResult);

        verify(paymentOperations).verify(billingResult.getPayload());
    }

    @Test
    public void cancelsTransactionForPlayBillingFailure() {
        when(paymentOperations.cancel(anyString())).thenReturn(Observable.empty());

        presenter.onCreate(activity, null);
        presenter.handleBillingResult(TestBillingResults.cancelled());

        verify(paymentOperations).cancel("payment failed");
        verify(paymentErrorPresenter).showCancelled();
    }

    @Test
    public void cancelsTransactionPlayBillingError() {
        when(paymentOperations.cancel(anyString())).thenReturn(Observable.empty());

        presenter.onCreate(activity, null);
        presenter.handleBillingResult(TestBillingResults.error());

        verify(paymentOperations).cancel(anyString());
    }

    @Test
    public void purchaseCancellationWithNoProductSetsUpProduct() {
        setupExpectedProductDetails();
        when(activity.getLastCustomNonConfigurationInstance()).thenReturn(new TransactionState(Observable.never(),
                                                                                               null));
        when(paymentOperations.cancel(anyString())).thenReturn(Observable.empty());
        presenter.onCreate(activity, null);

        presenter.handleBillingResult(TestBillingResults.cancelled());

        verify(conversionView).showDetails(PRICE);
    }

    @Test
    public void doesNotQueryProductDetailsIfBillingIsNotSupported() {
        when(paymentOperations.connect(activity)).thenReturn(Observable.just(ConnectionStatus.UNSUPPORTED));

        presenter.onCreate(activity, null);

        verify(paymentOperations, never()).queryProduct();
        verify(paymentErrorPresenter).showBillingUnavailable();
    }

    @Test
    public void queriesPurchaseStatusWhenBillingServiceIsReady() {
        when(paymentOperations.connect(activity)).thenReturn(Observable.just(ConnectionStatus.READY));
        when(paymentOperations.queryStatus()).thenReturn(Observable.empty());

        presenter.onCreate(activity, null);

        verify(paymentOperations).queryStatus();
    }

    @Test
    public void queriesProductDetailsWhenPurchaseStatusIsNone() {
        when(paymentOperations.connect(activity)).thenReturn(Observable.just(ConnectionStatus.READY));
        when(paymentOperations.queryStatus()).thenReturn(Observable.just(PurchaseStatus.NONE));
        when(paymentOperations.queryProduct()).thenReturn(Observable.never());

        presenter.onCreate(activity, null);

        verify(paymentOperations).queryProduct();
    }

    @Test
    public void displayBuyButtonWhenPurchaseStatusIsNone() {
        setupExpectedProductDetails();

        presenter.onCreate(activity, null);

        verify(conversionView).showDetails(PRICE);
    }

    @Test
    public void disablesBuyButtonWhenClicked() {
        ProductDetails details = setupExpectedProductDetails();
        when(paymentOperations.purchase(details.getId())).thenReturn(Observable.just("token"));
        presenter.onCreate(activity, null);

        verify(conversionView).setupContentView(eq(activity), listenerCaptor.capture());
        listenerCaptor.getValue().onPurchasePrimary();

        verify(conversionView).showLoadingState();
    }

    @Test
    public void reEnablesBuyButtonIfPurchaseStartFails() {
        ProductDetails details = setupExpectedProductDetails();
        Exception exception = new Exception();
        when(paymentOperations.purchase(details.getId())).thenReturn(Observable.error(exception));
        presenter.onCreate(activity, null);

        verify(conversionView).setupContentView(eq(activity), listenerCaptor.capture());
        listenerCaptor.getValue().onPurchasePrimary();

        verify(paymentErrorPresenter).onError(exception);
        verify(conversionView).enableBuyButton();
    }

    @Test
    public void reEnablesBuyButtonWhenPurchaseIsCancelled() {
        setupExpectedProductDetails();
        when(paymentOperations.cancel(anyString())).thenReturn(Observable.just(new ApiResponse(null, 200, null)));

        presenter.onCreate(activity, null);
        presenter.handleBillingResult(TestBillingResults.cancelled());

        verify(conversionView).enableBuyButton();
    }

    @Test
    public void sendsFailReasonWhenPurchaseIsCancelled() {
        BillingResult result = TestBillingResults.cancelled();
        PublishSubject subject = PublishSubject.create();
        when(paymentOperations.cancel(result.getFailReason())).thenReturn(subject);

        presenter.onCreate(activity, null);
        presenter.handleBillingResult(TestBillingResults.cancelled());

        assertThat(subject.hasObservers()).isTrue();
    }

    @Test
    public void displaysConnectionErrorIfProductIsNotAvailable() {
        when(paymentOperations.connect(activity)).thenReturn(Observable.just(ConnectionStatus.READY));
        when(paymentOperations.queryStatus()).thenReturn(Observable.just(PurchaseStatus.NONE));
        when(paymentOperations.queryProduct()).thenReturn(Observable.just(ProductStatus.fromNoProduct()));

        presenter.onCreate(activity, null);

        verify(paymentErrorPresenter).showConnectionError();
    }

    @Test
    public void startsSuccessActivityWhenPurchaseIsSuccess() {
        when(paymentOperations.verify(any(Payload.class))).thenReturn(Observable.just(PurchaseStatus.SUCCESS));

        presenter.onCreate(activity, null);
        presenter.handleBillingResult(TestBillingResults.success());

        verify(navigator).resetForAccountUpgrade(activity);
    }

    @Test
    public void displaysErrorOnVerificationFail() {
        when(paymentOperations.verify(any(Payload.class))).thenReturn(Observable.just(PurchaseStatus.VERIFY_FAIL));

        presenter.onCreate(activity, null);
        presenter.handleBillingResult(TestBillingResults.success());

        verify(paymentErrorPresenter).showVerifyFail();
    }

    @Test
    public void displaysErrorOnVerificationTimeout() {
        when(paymentOperations.verify(any(Payload.class))).thenReturn(Observable.just(PurchaseStatus.VERIFY_TIMEOUT));

        presenter.onCreate(activity, null);
        presenter.handleBillingResult(TestBillingResults.success());

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
