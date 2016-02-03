package com.soundcloud.android.payments;

import static com.soundcloud.android.rx.observers.DefaultSubscriber.fireAndForget;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.UpgradeTrackingEvent;
import com.soundcloud.android.payments.error.PaymentError;
import com.soundcloud.android.payments.googleplay.BillingResult;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.lightcycle.DefaultActivityLightCycle;
import com.soundcloud.rx.eventbus.EventBus;
import org.jetbrains.annotations.Nullable;
import rx.Observable;
import rx.subscriptions.CompositeSubscription;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;

import javax.inject.Inject;

class UpgradePresenter extends DefaultActivityLightCycle<AppCompatActivity> implements UpgradeView.Listener {

    private final PaymentOperations paymentOperations;
    private final PaymentErrorPresenter paymentErrorPresenter;
    private final UpgradeView upgradeView;
    private final EventBus eventBus;
    private final Navigator navigator;

    private Observable<String> purchaseObservable;
    private Observable<PurchaseStatus> statusObservable;
    private final CompositeSubscription subscription = new CompositeSubscription();
    @Nullable private TransactionState restoreState;

    private AppCompatActivity activity;
    private ProductDetails details;

    @Inject
    UpgradePresenter(PaymentOperations paymentOperations, PaymentErrorPresenter paymentErrorPresenter,
                     UpgradeView upgradeView, EventBus eventBus, Navigator navigator) {
        this.paymentOperations = paymentOperations;
        this.paymentErrorPresenter = paymentErrorPresenter;
        this.upgradeView = upgradeView;
        this.eventBus = eventBus;
        this.navigator = navigator;
    }

    @Override
    public void onCreate(AppCompatActivity activity, @Nullable Bundle bundle) {
        this.activity = activity;
        upgradeView.setupContentView(activity, this);
        paymentErrorPresenter.setActivity(activity);
        restoreState = (TransactionState) activity.getLastCustomNonConfigurationInstance();
        clearExistingError(activity);
        initConnection();
    }

    private void initConnection() {
        subscription.add(paymentOperations.connect(activity).subscribe(new ConnectionSubscriber()));
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
        return new TransactionState(purchaseObservable, statusObservable);
    }

    @Override
    public void onDestroy(AppCompatActivity activity) {
        subscription.unsubscribe();
        paymentOperations.disconnect();
    }

    @Override
    public void startPurchase() {
        subscribeToPurchase(paymentOperations.purchase(details.getId()).cache());
        upgradeView.disableBuyButton();
        eventBus.publish(EventQueue.TRACKING, UpgradeTrackingEvent.forUpgradeButtonClick());
    }

    @Override
    public void done() {
        navigator.restartForAccountUpgrade(activity);
        activity.finish();
    }

    @Override
    public void moreInfo() {
        navigator.restartForAccountUpgrade(activity);
        activity.finish();
    }

    public void handleBillingResult(BillingResult result) {
        if (result.isForRequest()) {
            if (result.isOk()) {
                restoreState = null;
                upgradeView.hideBuyButton();
                subscribeToStatus(paymentOperations.verify(result.getPayload()).cache());
            } else {
                paymentErrorPresenter.showCancelled();
                fireAndForget(paymentOperations.cancel(result.getFailReason()));
                if (details == null) {
                    initConnection();
                } else {
                    upgradeView.enableBuyButton();
                }
            }
        }
    }

    private void subscribeToStatus(Observable<PurchaseStatus> status) {
        statusObservable = status;
        subscription.add(statusObservable.subscribe(new StatusSubscriber()));
    }

    private void subscribeToPurchase(Observable<String> purchase) {
        statusObservable = null;
        purchaseObservable = purchase;
        subscription.add(purchaseObservable.subscribe(new PurchaseSubscriber()));
    }

    private class ConnectionSubscriber extends DefaultSubscriber<ConnectionStatus> {
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

    private class DetailsSubscriber extends DefaultSubscriber<ProductStatus> {
        @Override
        public void onNext(ProductStatus result) {
            if (result.isSuccess()) {
                details = result.getDetails();
                upgradeView.showBuyButton(details.getPrice());
                eventBus.publish(EventQueue.TRACKING, UpgradeTrackingEvent.forUpgradeButtonImpression());
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
            upgradeView.enableBuyButton();
        }
    }

    private class StatusSubscriber extends DefaultSubscriber<PurchaseStatus> {
        @Override
        public void onNext(PurchaseStatus result) {
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
        }

        @Override
        public void onError(Throwable e) {
            super.onError(e);
            paymentErrorPresenter.onError(e);
        }
    }

    private void upgradeSuccess() {
        upgradeView.showSuccess();
        eventBus.publish(EventQueue.TRACKING, UpgradeTrackingEvent.forUpgradeSuccess());
    }

    private void loadPurchaseOptions() {
        subscription.add(paymentOperations.queryProduct().subscribe(new DetailsSubscriber()));
    }

}
