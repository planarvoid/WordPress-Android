package com.soundcloud.android.payments;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.R;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.UpgradeTrackingEvent;
import com.soundcloud.lightcycle.DefaultActivityLightCycle;
import com.soundcloud.rx.eventbus.EventBus;

import android.app.Activity;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.VisibleForTesting;
import android.support.v7.app.AppCompatActivity;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;

class WebCheckoutPresenter extends DefaultActivityLightCycle<AppCompatActivity> implements WebCheckoutInterface.Listener, WebCheckoutView.Listener {

    private static final long TIMEOUT_MILLIS = TimeUnit.SECONDS.toMillis(15);
    public static final String PAYMENT_FORM_BASE_URL = "https://soundcloud.com/android_payment.html";
    public static final String OAUTH_TOKEN_KEY = "oauth_token";
    public static final String PRICE_KEY = "price";
    public static final String EXPIRY_DATE_KEY = "expiry_date";
    public static final String TRIAL_DAYS_KEY = "trial_days";
    public static final String PACKAGE_URN_KEY = "package_urn";
    public static final String ENVIRONMENT_KEY = "env";
    public static final String DISCOUNT_PRICE = "discount_price";

    private final WebCheckoutView view;
    private final AccountOperations operations;
    private final Navigator navigator;
    private final EventBus eventBus;
    private final Resources resources;

    private Activity activity;

    private Handler handler = new Handler();

    @Inject
    public WebCheckoutPresenter(WebCheckoutView view,
                                AccountOperations operations,
                                Navigator navigator,
                                EventBus eventBus,
                                Resources resources) {
        this.view = view;
        this.operations = operations;
        this.navigator = navigator;
        this.eventBus = eventBus;
        this.resources = resources;
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
        final String url = buildPaymentFormUrl(
                operations.getSoundCloudToken().getAccessToken(),
                getProductFromIntent(),
                resources.getString(R.string.web_payment_form_environment));

        view.setLoading(true);
        startTimeout();

        view.setupJavaScriptInterface(WebCheckoutInterface.JAVASCRIPT_OBJECT_NAME, new WebCheckoutInterface(this));
        view.loadUrl(url);
    }

    @Override
    public void onRetry() {
        loadForm();
    }

    @Override
    public void onWebAppReady() {
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
        navigator.resetForAccountUpgrade(activity);
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

    @VisibleForTesting
    String buildPaymentFormUrl(String token, WebProduct product, String environment) {
        final Uri.Builder builder = Uri.parse(PAYMENT_FORM_BASE_URL)
                .buildUpon()
                .appendQueryParameter(OAUTH_TOKEN_KEY, token)
                .appendQueryParameter(PRICE_KEY, product.getPrice())
                .appendQueryParameter(TRIAL_DAYS_KEY, Integer.toString(product.getTrialDays()))
                .appendQueryParameter(EXPIRY_DATE_KEY, product.getExpiryDate())
                .appendQueryParameter(PACKAGE_URN_KEY, product.getPackageUrn())
                .appendQueryParameter(ENVIRONMENT_KEY, environment);

        if (product.getDiscountPrice().isPresent()) {
            builder.appendQueryParameter(DISCOUNT_PRICE, product.getDiscountPrice().get());
        }

        return builder.toString();
    }
}
