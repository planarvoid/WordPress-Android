package com.soundcloud.android.payments;

import butterknife.ButterKnife;
import butterknife.InjectView;
import com.soundcloud.android.R;
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

    private class ConnectionSubscriber extends DefaultSubscriber<ConnectionStatus> {
        @Override
        public void onNext(ConnectionStatus status) {
            if (status.isReady()) {
                buyButton.setVisibility(View.VISIBLE);
                subscription.add(paymentOperations.queryProductDetails().subscribe(new DetailsSubscriber()));
            }
        }
    }

    /*
     * TODO: All the error case handling will depend on the subscription flow designs
     */
    private class DetailsSubscriber extends DefaultSubscriber<ProductStatus> {
        @Override
        public void onNext(ProductStatus result) {
            if (result.isSuccess()) {
                ProductDetails details = result.getDetails();
                title.setText(details.title);
                description.setText(details.description);
                price.setText(details.price);
            } else {
                Toast.makeText(context, "No subscription available!", Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        public void onError(Throwable e) {
            super.onError(e);
            Toast.makeText(context, "Connection error!", Toast.LENGTH_SHORT).show();
        }
    }

}
