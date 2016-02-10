package com.soundcloud.android.payments;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.UpgradeTrackingEvent;
import com.soundcloud.lightcycle.DefaultActivityLightCycle;
import com.soundcloud.rx.eventbus.EventBus;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;

class WebCheckoutPresenter extends DefaultActivityLightCycle<AppCompatActivity> implements WebCheckoutInterface.Listener, WebCheckoutView.Listener {

    private static final long TIMEOUT_MILLIS = TimeUnit.SECONDS.toMillis(8);

    private final WebCheckoutView view;
    private final AccountOperations operations;
    private final Navigator navigator;
    private final EventBus eventBus;

    private Activity activity;

    private Handler handler = new Handler();

    @Inject
    public WebCheckoutPresenter(WebCheckoutView view,
                                AccountOperations operations,
                                Navigator navigator,
                                EventBus eventBus) {
        this.view = view;
        this.operations = operations;
        this.navigator = navigator;
        this.eventBus = eventBus;
    }

    @Override
    public void onCreate(AppCompatActivity activity, Bundle bundle) {
        this.activity = activity;
        view.setupContentView(activity, this);
        loadForm();
    }

    private WebProduct getProductFromIntent() {
        return (WebProduct) activity.getIntent().getParcelableExtra(WebConversionPresenter.PRODUCT_INFO);
    }

    private void loadForm() {
        final WebCheckoutInterface checkoutInterface = new WebCheckoutInterface(this,
                operations.getSoundCloudToken().getAccessToken(),
                getProductFromIntent());

        view.setLoading(true);
        startTimeout();

        view.setupJavaScriptInterface(WebCheckoutInterface.JAVASCRIPT_OBJECT_NAME, checkoutInterface);
        view.loadUrl(WebCheckoutInterface.PAYMENT_FORM_URL);
    }

    @Override
    public void onRetry() {
        loadForm();
    }

    @Override
    public void onLoad() {
        cancelTimeout();
        // WebView callbacks are not on the UI thread
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                view.setLoading(false);
            }
        });
    }

    @Override
    public void onPaymentSuccess() {
        eventBus.publish(EventQueue.TRACKING, UpgradeTrackingEvent.forUpgradeSuccess());
        navigator.restartForAccountUpgrade(activity);
        activity.finish();
    }

    public boolean handleBackPress() {
        return view.handleBackPress();
    }

    @Override
    public void onDestroy(AppCompatActivity activity) {
        cancelTimeout();
        this.activity = null;
    }

    private void startTimeout() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                view.setRetry();
            }
        }, TIMEOUT_MILLIS);
    }

    private void cancelTimeout() {
        handler.removeCallbacksAndMessages(null);
    }

}
