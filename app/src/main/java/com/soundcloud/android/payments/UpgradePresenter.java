package com.soundcloud.android.payments;

import static com.soundcloud.android.rx.observers.DefaultSubscriber.fireAndForget;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import com.soundcloud.android.R;
import com.soundcloud.android.configuration.ConfigurationOperations;
import com.soundcloud.android.payments.googleplay.BillingResult;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.util.AnimUtils;
import com.soundcloud.lightcycle.DefaultLightCycleActivity;
import org.jetbrains.annotations.Nullable;
import rx.subscriptions.CompositeSubscription;

import android.content.res.Resources;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import javax.inject.Inject;

class UpgradePresenter extends DefaultLightCycleActivity<AppCompatActivity> {

    private final PaymentOperations paymentOperations;
    private final PaymentErrorPresenter paymentErrorPresenter;
    private final ConfigurationOperations configurationOperations;
    private final Resources resources;

    @InjectView(R.id.upgrade_header) View upgradeHeader;
    @InjectView(R.id.success_header) View successHeader;
    @InjectView(R.id.upgrade_buy) Button buyButton;
    @InjectView(R.id.upgrade_loading) View loading;

    private final CompositeSubscription subscription = new CompositeSubscription();

    private ProductDetails details;

    @Inject
    UpgradePresenter(PaymentOperations paymentOperations, PaymentErrorPresenter paymentErrorPresenter,
                     ConfigurationOperations configurationOperations, Resources resources) {
        this.paymentOperations = paymentOperations;
        this.paymentErrorPresenter = paymentErrorPresenter;
        this.configurationOperations = configurationOperations;
        this.resources = resources;
    }

    @Override
    public void onCreate(AppCompatActivity activity, @Nullable Bundle bundle) {
        ButterKnife.inject(this, activity.findViewById(android.R.id.content));
        paymentErrorPresenter.setActivity(activity);
        subscription.add(paymentOperations.connect(activity).subscribe(new ConnectionSubscriber()));
    }

    @Override
    public void onDestroy(AppCompatActivity activity) {
        subscription.unsubscribe();
        paymentOperations.disconnect();
    }

    public void handleBillingResult(BillingResult result) {
        if (result.isForRequest()) {
            if (result.isOk()) {
                AnimUtils.hideView(buyButton.getContext(), buyButton, true);
                loading.setVisibility(View.VISIBLE);
                subscription.add(paymentOperations.verify(result.getPayload()).subscribe(new StatusSubscriber()));
            } else {
                buyButton.setEnabled(true);
                paymentErrorPresenter.showCancelled();
                fireAndForget(paymentOperations.cancel(result.getFailReason()));
            }
        }
    }

    @OnClick(R.id.upgrade_buy)
    public void beginTransaction() {
        subscription.add(paymentOperations.purchase(details.getId()).subscribe(new TransactionSubscriber()));
        buyButton.setEnabled(false);
    }

    private void displayProductDetails() {
        buyButton.setText(resources.getString(R.string.upgrade_buy_price, details.getPrice()));
        loading.setVisibility(View.GONE);
        AnimUtils.showView(buyButton.getContext(), buyButton, true);
    }

    private class ConnectionSubscriber extends DefaultSubscriber<ConnectionStatus> {
        @Override
        public void onNext(ConnectionStatus status) {
            if (status.isReady()) {
                subscription.add(paymentOperations.queryStatus().subscribe(new StatusSubscriber()));
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
                displayProductDetails();
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

    private class TransactionSubscriber extends DefaultSubscriber<String> {
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
                    showSuccessScreen();
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

    private void showSuccessScreen() {
        upgradeHeader.setVisibility(View.GONE);
        successHeader.setVisibility(View.VISIBLE);
    }

    private void loadPurchaseOptions() {
        subscription.add(paymentOperations.queryProduct().subscribe(new DetailsSubscriber()));
    }

}
