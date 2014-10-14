package com.soundcloud.android.payments;

import butterknife.ButterKnife;
import butterknife.InjectView;
import com.soundcloud.android.R;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import rx.subscriptions.CompositeSubscription;

import android.app.Activity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import javax.inject.Inject;

class SubscribeController {

    @Inject PaymentOperations paymentOperations;

    @InjectView(R.id.subscribe_title) TextView title;
    @InjectView(R.id.subscribe_description) TextView description;
    @InjectView(R.id.subscribe_price) TextView price;
    @InjectView(R.id.subscribe_buy) Button buyButton;

    private CompositeSubscription subscription = new CompositeSubscription();

    @Inject
    SubscribeController(PaymentOperations paymentOperations) {
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

    private class DetailsSubscriber extends DefaultSubscriber<ProductDetails> {
        @Override
        public void onNext(ProductDetails details) {
            title.setText(details.title);
            description.setText(details.description);
            price.setText(details.price);
        }
    }

}
