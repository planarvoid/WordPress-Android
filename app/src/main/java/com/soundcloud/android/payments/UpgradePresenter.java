package com.soundcloud.android.payments;

import static com.soundcloud.android.rx.observers.DefaultSubscriber.fireAndForget;

import com.soundcloud.android.configuration.ConfigurationOperations;
import com.soundcloud.android.payments.googleplay.BillingResult;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.lightcycle.DefaultLightCycleActivity;
import org.jetbrains.annotations.Nullable;
import rx.Observable;
import rx.subscriptions.CompositeSubscription;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import javax.inject.Inject;

class UpgradePresenter extends DefaultLightCycleActivity<AppCompatActivity> implements UpgradeView.Listener {

    private final PaymentOperations paymentOperations;
    private final PaymentErrorPresenter paymentErrorPresenter;
    private final ConfigurationOperations configurationOperations;
    private final UpgradeView upgradeView;

    private Observable<String> purchaseObservable;
    private Observable<PurchaseStatus> statusObservable;
    private final CompositeSubscription subscription = new CompositeSubscription();

    private AppCompatActivity activity;
    private ProductDetails details;

    @Inject
    UpgradePresenter(PaymentOperations paymentOperations, PaymentErrorPresenter paymentErrorPresenter,
                     ConfigurationOperations configurationOperations, UpgradeView upgradeView) {
        this.paymentOperations = paymentOperations;
        this.paymentErrorPresenter = paymentErrorPresenter;
        this.configurationOperations = configurationOperations;
        this.upgradeView = upgradeView;
        upgradeView.setListener(this);
    }

    @Override
    public void onCreate(AppCompatActivity activity, @Nullable Bundle bundle) {
        this.activity = activity;
        upgradeView.setupContentView(activity);
        paymentErrorPresenter.setActivity(activity);
        initializeTransactionState();
    }

    private void initializeTransactionState() {
        TransactionState state = (TransactionState) activity.getLastCustomNonConfigurationInstance();
        if (state != null && state.isTransactionInProgress()) {
            restoreTransaction(state);
        } else {
            init();
        }
    }

    private void init() {
        subscription.add(paymentOperations.connect(activity).subscribe(new ConnectionSubscriber()));
    }

    private void restoreTransaction(TransactionState state) {
        if (state.isRetrievingStatus()) {
            subscribeToStatus(state.status());
        } else {
            subscribeToPurchase(state.purchase());
        }
    }

    public TransactionState getState() {
        return new TransactionState(purchaseObservable, statusObservable);
    }

    @Override
    public void onDestroy(AppCompatActivity activity) {
        subscription.unsubscribe();
        paymentOperations.disconnect();
    }

    public void startPurchase() {
        subscribeToPurchase(paymentOperations.purchase(details.getId()));
        upgradeView.disableBuyButton();
    }

    public void handleBillingResult(BillingResult result) {
        if (result.isForRequest()) {
            if (result.isOk()) {
                upgradeView.hideBuyButton();
                subscribeToStatus(paymentOperations.verify(result.getPayload()));
            } else {
                paymentErrorPresenter.showCancelled();
                fireAndForget(paymentOperations.cancel(result.getFailReason()));
                if (details == null) {
                    init();
                } else {
                    upgradeView.enableBuyButton();
                }
            }
        }
    }

    private void subscribeToStatus(Observable<PurchaseStatus> status) {
        statusObservable = status;
        subscription.add(statusObservable.cache().subscribe(new StatusSubscriber()));
    }

    private void subscribeToPurchase(Observable<String> purchase) {
        statusObservable = null;
        purchaseObservable = purchase;
        subscription.add(purchaseObservable.cache().subscribe(new PurchaseSubscriber()));
    }

    private class ConnectionSubscriber extends DefaultSubscriber<ConnectionStatus> {
        @Override
        public void onNext(ConnectionStatus status) {
            if (status.isReady()) {
                subscribeToStatus(paymentOperations.queryStatus());
            } else if (status.isUnsupported()) {
                paymentErrorPresenter.showBillingUnavailable();
            }
        }
    }

    private class DetailsSubscriber extends DefaultSubscriber<ProductStatus> {
        @Override
        public void onNext(ProductStatus result) {
            if (result.isSuccess()) {
                details = result.getDetails();
                upgradeView.showBuyButton(details.getPrice());
            } else {
                paymentErrorPresenter.showConnectionError();
            }
        }

        @Override
        public void onError(Throwable e) {
            super.onError(e);
            paymentErrorPresenter.onError(e);
        }
    }

    private class PurchaseSubscriber extends DefaultSubscriber<String> {
        @Override
        public void onError(Throwable e) {
            super.onError(e);
            paymentErrorPresenter.onError(e);
        }
    }

    private class StatusSubscriber extends DefaultSubscriber<PurchaseStatus> {
        @Override
        public void onNext(PurchaseStatus result) {
            switch (result) {
                case SUCCESS:
                    configurationOperations.update();
                    upgradeView.showSuccess();
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
        }

        @Override
        public void onError(Throwable e) {
            paymentErrorPresenter.onError(e);
        }
    }

    private void loadPurchaseOptions() {
        subscription.add(paymentOperations.queryProduct().subscribe(new DetailsSubscriber()));
    }

}
