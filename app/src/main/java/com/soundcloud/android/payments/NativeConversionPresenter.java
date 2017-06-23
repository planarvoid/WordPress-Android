package com.soundcloud.android.payments;

import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.UpgradeFunnelEvent;
import com.soundcloud.android.navigation.NavigationExecutor;
import com.soundcloud.android.payments.error.PaymentError;
import com.soundcloud.android.payments.googleplay.BillingResult;
import com.soundcloud.android.rx.observers.DefaultObserver;
import com.soundcloud.android.rx.observers.DefaultSingleObserver;
import com.soundcloud.lightcycle.DefaultActivityLightCycle;
import com.soundcloud.rx.eventbus.EventBusV2;
import io.reactivex.Single;
import io.reactivex.disposables.CompositeDisposable;
import org.jetbrains.annotations.Nullable;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;

import javax.inject.Inject;

class NativeConversionPresenter extends DefaultActivityLightCycle<AppCompatActivity>
        implements ConversionView.Listener {

    private final NativePaymentOperations paymentOperations;
    private final PaymentErrorPresenter paymentErrorPresenter;
    private final ConversionView conversionView;
    private final EventBusV2 eventBus;
    private final NavigationExecutor navigationExecutor;

    private Single<String> purchaseSingle;
    private Single<PurchaseStatus> statusSingle;
    private final CompositeDisposable disposable = new CompositeDisposable();
    @Nullable private TransactionState restoreState;

    private AppCompatActivity activity;
    private ProductDetails details;

    @Inject
    NativeConversionPresenter(NativePaymentOperations paymentOperations, PaymentErrorPresenter paymentErrorPresenter,
                              ConversionView conversionView, EventBusV2 eventBus, NavigationExecutor navigationExecutor) {
        this.paymentOperations = paymentOperations;
        this.paymentErrorPresenter = paymentErrorPresenter;
        this.conversionView = conversionView;
        this.eventBus = eventBus;
        this.navigationExecutor = navigationExecutor;
    }

    @Override
    public void onCreate(AppCompatActivity activity, @Nullable Bundle bundle) {
        this.activity = activity;
        conversionView.setupContentView(activity, this);
        paymentErrorPresenter.setActivity(activity);
        restoreState = (TransactionState) activity.getLastCustomNonConfigurationInstance();
        clearExistingError(activity);
        initConnection();
    }

    @Override
    public void onPurchasePrimary() {
        subscribeToPurchase(paymentOperations.purchase(details.getId()).cache());
        conversionView.showLoadingState();
        eventBus.publish(EventQueue.TRACKING, UpgradeFunnelEvent.forConversionBuyButtonClick());
    }

    @Override
    public void onMoreProducts() {
        // No-op for this presenter!
    }

    private void initConnection() {
        disposable.add(paymentOperations.connect(activity).subscribeWith(new ConnectionObserver()));
    }

    private void clearExistingError(AppCompatActivity activity) {
        final Fragment error = activity.getSupportFragmentManager().findFragmentByTag(PaymentError.DIALOG_TAG);
        if (error != null) {
            activity.getSupportFragmentManager()
                    .beginTransaction()
                    .remove(error)
                    .commit();
        }
    }

    public TransactionState getState() {
        return new TransactionState(purchaseSingle, statusSingle);
    }

    @Override
    public void onDestroy(AppCompatActivity activity) {
        disposable.clear();
        paymentOperations.disconnect();
    }

    void handleBillingResult(BillingResult result) {
        if (result.isForRequest()) {
            if (result.isOk()) {
                restoreState = null;
                conversionView.showLoadingState();
                subscribeToStatus(paymentOperations.verify(result.getPayload()).cache());
            } else {
                paymentErrorPresenter.showCancelled();
                paymentOperations.cancel(result.getFailReason()).subscribeWith(new DefaultSingleObserver<>());
                if (details == null) {
                    initConnection();
                } else {
                    conversionView.enableBuyButton();
                }
            }
        }
    }

    private void subscribeToStatus(Single<PurchaseStatus> status) {
        statusSingle = status;
        disposable.add(statusSingle.subscribeWith(new StatusObserver()));
    }

    private void subscribeToPurchase(Single<String> purchase) {
        statusSingle = null;
        purchaseSingle = purchase;
        disposable.add(purchaseSingle.subscribeWith(new PurchaseObserver()));
    }

    private class ConnectionObserver extends DefaultObserver<ConnectionStatus> {
        @Override
        public void onNext(ConnectionStatus status) {
            if (status.isReady()) {
                restorePendingTransactionOrQueryStatus();
            } else if (status.isUnsupported()) {
                paymentErrorPresenter.showBillingUnavailable();
            }
        }
    }

    private void restorePendingTransactionOrQueryStatus() {
        if (restoreState != null && restoreState.isTransactionInProgress()) {
            restoreTransaction(restoreState);
        } else {
            subscribeToStatus(paymentOperations.queryStatus().cache());
        }
    }

    private void restoreTransaction(TransactionState state) {
        if (state.isRetrievingStatus()) {
            subscribeToStatus(state.status());
        } else {
            subscribeToPurchase(state.purchase());
        }
        restoreState = null;
    }

    private class DetailsObserver extends DefaultSingleObserver<ProductStatus> {
        @Override
        public void onSuccess(ProductStatus result) {
            if (result.isSuccess()) {
                details = result.getDetails();
                conversionView.showDetails(details.getPrice());
                eventBus.publish(EventQueue.TRACKING, UpgradeFunnelEvent.forConversionBuyButtonImpression());
            } else {
                paymentErrorPresenter.showConnectionError();
            }
            super.onSuccess(result);
        }

        @Override
        public void onError(Throwable e) {
            super.onError(e);
            paymentErrorPresenter.onError(e);
        }
    }

    private class PurchaseObserver extends DefaultSingleObserver<String> {
        @Override
        public void onError(Throwable e) {
            super.onError(e);
            paymentErrorPresenter.onError(e);
            conversionView.enableBuyButton();
        }
    }

    private class StatusObserver extends DefaultSingleObserver<PurchaseStatus> {
        @Override
        public void onSuccess(PurchaseStatus result) {
            switch (result) {
                case SUCCESS:
                    upgradeSuccess();
                    break;
                case VERIFY_FAIL:
                    paymentErrorPresenter.showVerifyFail();
                    break;
                case VERIFY_TIMEOUT:
                    paymentErrorPresenter.showVerifyTimeout();
                    break;
                case NONE:
                    loadPurchaseOptions();
                    break;
                default:
                    break;
            }
            super.onSuccess(result);
        }

        @Override
        public void onError(Throwable e) {
            super.onError(e);
            paymentErrorPresenter.onError(e);
        }
    }

    private void upgradeSuccess() {
        navigationExecutor.resetForAccountUpgrade(activity);
        eventBus.publish(EventQueue.TRACKING, UpgradeFunnelEvent.forUpgradeSuccess());
    }

    private void loadPurchaseOptions() {
        disposable.add(paymentOperations.queryProduct().subscribeWith(new DetailsObserver()));
    }

}
