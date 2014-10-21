package com.soundcloud.android.payments;

import static com.soundcloud.android.rx.observers.DefaultSubscriber.fireAndForget;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import com.soundcloud.android.R;
import com.soundcloud.android.payments.googleplay.PlayBillingResult;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import rx.subscriptions.CompositeSubscription;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import javax.inject.Inject;

class SubscribeController {

    private final Context context;
    private final PaymentOperations paymentOperations;

    @InjectView(R.id.subscribe_title) TextView title;
    @InjectView(R.id.subscribe_description) TextView description;
    @InjectView(R.id.subscribe_price) TextView price;
    @InjectView(R.id.subscribe_buy) Button buyButton;

    private final CompositeSubscription subscription = new CompositeSubscription();

    private ProductDetails details;

    @Inject
    SubscribeController(Context context, PaymentOperations paymentOperations) {
        this.context = context;
        this.paymentOperations = paymentOperations;
    }

    public void onCreate(Activity activity) {
        activity.setContentView(R.layout.payments_activity);
        ButterKnife.inject(this, activity.findViewById(android.R.id.content));
        subscription.add(paymentOperations.connect(activity).subscribe(new ConnectionSubscriber()));
    }

    public void onDestroy() {
        subscription.unsubscribe();
        paymentOperations.disconnect();
    }

    public void handleBillingResult(PlayBillingResult result) {
        if (result.isForRequest()) {
            if (result.isOk()) {
                subscription.add(paymentOperations.verify(result).subscribe(new VerifySubscriber()));
            } else {
                showText(R.string.payments_user_cancelled);
                fireAndForget(paymentOperations.cancel(result));
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
                subscription.add(paymentOperations.queryProduct().subscribe(new DetailsSubscriber()));
            }
        }
    }

    /*
     * TODO: All the error case handling & messaging will depend on the subscription flow designs
     */
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
            showConnectionError();
        }
    }

    private class TransactionSubscriber extends DefaultSubscriber<String> {
        @Override
        public void onError(Throwable e) {
            super.onError(e);
            showConnectionError();
        }
    }

    private class VerifySubscriber extends DefaultSubscriber<PurchaseStatus> {
        @Override
        public void onNext(PurchaseStatus result) {
            switch(result) {
                case VERIFYING:
                    showText(R.string.payments_verifying);
                    break;
                case FAILURE:
                    showText(R.string.payments_verification_failed);
                    break;
                case SUCCESS:
                    showText(R.string.payments_success);
                default:
                    break;
            }
        }
    }

    private void showConnectionError() {
        showText(R.string.payments_connection_error);
    }

    private void showText(int messageId) {
        Toast.makeText(context, messageId, Toast.LENGTH_SHORT).show();
    }

}
