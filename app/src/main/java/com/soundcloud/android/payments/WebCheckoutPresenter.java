package com.soundcloud.android.payments;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.UpgradeTrackingEvent;
import com.soundcloud.lightcycle.DefaultActivityLightCycle;
import com.soundcloud.rx.eventbus.EventBus;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import javax.inject.Inject;

class WebCheckoutPresenter extends DefaultActivityLightCycle<AppCompatActivity> implements WebCheckoutInterface.Listener {

    private final WebCheckoutView view;
    private final AccountOperations operations;
    private final Navigator navigator;
    private final EventBus eventBus;

    private Activity activity;

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

    @SuppressLint("AddJavascriptInterface")
    @Override
    public void onCreate(AppCompatActivity activity, Bundle bundle) {
        this.activity = activity;
        view.setupContentView(activity);

        final WebCheckoutInterface checkoutInterface = new WebCheckoutInterface(this,
                operations.getSoundCloudToken().getAccessToken(),
                (WebProduct) activity.getIntent().getParcelableExtra(WebConversionPresenter.PRODUCT_INFO));

        view.setupJavaScriptInterface(WebCheckoutInterface.JAVASCRIPT_OBJECT_NAME, checkoutInterface);
        view.loadUrl(WebCheckoutInterface.PAYMENT_FORM_URL);
    }

    @Override
    public void onLoad() {
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
        this.activity = null;
    }

}
