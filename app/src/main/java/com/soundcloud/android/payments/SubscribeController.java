package com.soundcloud.android.payments;

import static com.soundcloud.android.rx.observers.DefaultSubscriber.fireAndForget;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import com.soundcloud.android.R;
import com.soundcloud.android.configuration.ConfigurationOperations;
import com.soundcloud.lightcycle.DefaultLightCycleActivity;
import com.soundcloud.android.payments.googleplay.BillingResult;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import org.jetbrains.annotations.Nullable;
import rx.subscriptions.CompositeSubscription;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import javax.inject.Inject;

class SubscribeController extends DefaultLightCycleActivity<ActionBarActivity> {

    private final PaymentOperations paymentOperations;
    private final PaymentErrorController paymentErrorController;
    private final ConfigurationOperations configurationOperations;

    @InjectView(R.id.subscribe_title) TextView title;
    @InjectView(R.id.subscribe_description) TextView description;
    @InjectView(R.id.subscribe_price) TextView price;
    @InjectView(R.id.subscribe_buy) Button buyButton;

    private final CompositeSubscription subscription = new CompositeSubscription();

    private Activity activity;
    private ProductDetails details;

    @Inject
    SubscribeController(PaymentOperations paymentOperations, PaymentErrorController paymentErrorController,
                        ConfigurationOperations configurationOperations) {
        this.paymentOperations = paymentOperations;
        this.paymentErrorController = paymentErrorController;
        this.configurationOperations = configurationOperations;
    }

    @Override
    public void onCreate(ActionBarActivity activity, @Nullable Bundle bundle) {
        this.activity = activity;
        activity.setContentView(R.layout.subscribe_activity);
        ButterKnife.inject(this, activity.findViewById(android.R.id.content));
        paymentErrorController.bind(activity);
        subscription.add(paymentOperations.connect(activity).subscribe(new ConnectionSubscriber()));
    }

    @Override
    public void onDestroy(ActionBarActivity activity) {
        subscription.unsubscribe();
        paymentOperations.disconnect();
    }

    public void handleBillingResult(BillingResult result) {
        if (result.isForRequest()) {
            if (result.isOk()) {
                showText(R.string.payments_verifying);
                subscription.add(paymentOperations.verify(result.getPayload()).subscribe(new StatusSubscriber()));
            } else {
                showText(R.string.payments_user_cancelled);
                fireAndForget(paymentOperations.cancel(result.getFailReason()));
            }
        }
    }

    @OnClick(R.id.subscribe_buy)
    public void beginTransaction() {
        subscription.add(paymentOperations.purchase(details.getId()).subscribe(new TransactionSubscriber()));
    }

    private void displayProductDetails() {
        title.setText(details.getTitle());
        description.setText(details.getDescription());
        price.setText(details.getPrice());
        buyButton.setVisibility(View.VISIBLE);
    }

    private class ConnectionSubscriber extends DefaultSubscriber<ConnectionStatus> {
        @Override
        public void onNext(ConnectionStatus status) {
            if (status.isReady()) {
                subscription.add(paymentOperations.queryStatus().subscribe(new StatusSubscriber()));
            } else if (status.isUnsupported()) {
                showText(R.string.payments_connection_unavailable);
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
                showText(R.string.payments_none_available);
            }
        }

        @Override
        public void onError(Throwable e) {
            super.onError(e);
            paymentErrorController.onError(e);
        }
    }

    private class TransactionSubscriber extends DefaultSubscriber<String> {
        @Override
        public void onError(Throwable e) {
            super.onError(e);
            paymentErrorController.onError(e);
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
                    showText(R.string.payments_verify_fail);
                    break;
                case VERIFY_TIMEOUT:
                    showText(R.string.payments_verify_timeout);
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
            paymentErrorController.onError(e);
        }
    }

    private void showSuccessScreen() {
        activity.finish();
        activity.startActivity(new Intent(activity, SubscribeSuccessActivity.class));
    }

    private void loadPurchaseOptions() {
        subscription.add(paymentOperations.queryProduct().subscribe(new DetailsSubscriber()));
    }

    private void showText(int messageId) {
        Toast.makeText(activity.getApplicationContext(), messageId, Toast.LENGTH_SHORT).show();
    }

}
