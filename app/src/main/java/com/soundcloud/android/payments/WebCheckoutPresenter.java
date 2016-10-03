package com.soundcloud.android.payments;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.R;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.configuration.Plan;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PurchaseEvent;
import com.soundcloud.android.events.UpgradeFunnelEvent;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.rx.observers.DefaultSubscriber;
import com.soundcloud.android.utils.LocaleFormatter;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.lightcycle.DefaultActivityLightCycle;
import com.soundcloud.rx.eventbus.EventBus;
import dagger.Lazy;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;

import android.app.Activity;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.support.v7.app.AppCompatActivity;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;

class WebCheckoutPresenter extends DefaultActivityLightCycle<AppCompatActivity>
        implements WebCheckoutInterface.Listener, WebCheckoutView.Listener {

    private static final long TIMEOUT_MILLIS = TimeUnit.SECONDS.toMillis(15);

    public static final String PAYMENT_FORM_BASE_URL = "https://soundcloud.com/android_payment.html";
    public static final String OAUTH_TOKEN_KEY = "oauth_token";
    public static final String LOCALE_KEY = "locale";
    public static final String PRICE_KEY = "price";
    public static final String EXPIRY_DATE_KEY = "expiry_date";
    public static final String TRIAL_DAYS_KEY = "trial_days";
    public static final String PROMO_DAYS_KEY = "promo_days";
    public static final String PROMO_PRICE_KEY = "promo_price";
    public static final String PACKAGE_URN_KEY = "package_urn";
    public static final String ENVIRONMENT_KEY = "env";
    public static final String DISCOUNT_PRICE_KEY = "discount_price";

    private final WebCheckoutView view;
    private final AccountOperations operations;
    private final LocaleFormatter localeFormatter;
    private final Lazy<WebPaymentOperations> paymentOperations;
    private final Navigator navigator;
    private final EventBus eventBus;
    private final Resources resources;

    private Activity activity;

    private Subscription subscription = RxUtils.invalidSubscription();
    private Handler handler = new Handler();

    @Inject
    public WebCheckoutPresenter(WebCheckoutView view,
                                AccountOperations operations,
                                LocaleFormatter localeFormatter,
                                Lazy<WebPaymentOperations> paymentOperations,
                                Navigator navigator,
                                EventBus eventBus,
                                Resources resources) {
        this.view = view;
        this.operations = operations;
        this.localeFormatter = localeFormatter;
        this.paymentOperations = paymentOperations;
        this.navigator = navigator;
        this.eventBus = eventBus;
        this.resources = resources;
    }

    @Override
    public void onCreate(AppCompatActivity activity, Bundle bundle) {
        this.activity = activity;
        view.setupContentView(activity, this);
        loadProduct();
    }

    @Nullable
    private WebProduct getProductFromIntent() {
        return (WebProduct) activity.getIntent().getParcelableExtra(WebConversionPresenter.PRODUCT_INFO);
    }

    private void loadProduct() {
        view.setLoading(true);
        startTimeout();

        final WebProduct product = getProductFromIntent();
        if (product == null) {
            subscription = paymentOperations.get()
                                            .product()
                                            .observeOn(AndroidSchedulers.mainThread())
                                            .subscribe(new WebProductSubscriber());
        } else {
            launchWebForm(product);
        }
    }

    private void launchWebForm(WebProduct product) {
        final String url = buildPaymentFormUrl(
                operations.getSoundCloudToken().getAccessToken(),
                product,
                resources.getString(R.string.web_payment_form_environment));

        view.setupJavaScriptInterface(WebCheckoutInterface.JAVASCRIPT_OBJECT_NAME, new WebCheckoutInterface(this));
        view.loadUrl(url);
    }

    @Override
    public void onRetry() {
        loadProduct();
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
        eventBus.publish(EventQueue.TRACKING, UpgradeFunnelEvent.forUpgradeSuccess());
        trackPurchase();
        navigator.resetForAccountUpgrade(activity);
        activity.finish();
    }

    private void trackPurchase() {
        final WebProduct product = getProductFromIntent();
        if (product != null && Plan.fromId(product.getPlanId()) == Plan.HIGH_TIER) {
            eventBus.publish(EventQueue.TRACKING,
                             PurchaseEvent.forHighTierSub(product.getRawPrice(), product.getRawCurrency()));
        }
    }

    @Override
    public void onPaymentError(String errorType) {
        eventBus.publish(EventQueue.TRACKING, new PaymentErrorEvent(errorType));
    }

    public boolean handleBackPress() {
        return view.handleBackPress();
    }

    @Override
    public void onDestroy(AppCompatActivity activity) {
        subscription.unsubscribe();
        cancelTimeout();
        this.activity = null;
    }

    private void startTimeout() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                setRetryState();
            }
        }, TIMEOUT_MILLIS);
    }

    private void cancelTimeout() {
        handler.removeCallbacksAndMessages(null);
    }

    private void setRetryState() {
        cancelTimeout();
        view.setRetry();
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

        appendDiscount(product, builder);
        appendPromo(product, builder);
        appendLocale(builder);

        return builder.toString();
    }

    private void appendPromo(WebProduct product, Uri.Builder builder) {
        if (product.hasPromo()) {
            builder.appendQueryParameter(PROMO_DAYS_KEY, Integer.toString(product.getPromoDays()));
            builder.appendQueryParameter(PROMO_PRICE_KEY, product.getPromoPrice().get());
        }
    }

    private void appendDiscount(WebProduct product, Uri.Builder builder) {
        if (product.getDiscountPrice().isPresent()) {
            builder.appendQueryParameter(DISCOUNT_PRICE_KEY, product.getDiscountPrice().get());
        }
    }

    private void appendLocale(Uri.Builder builder) {
        Optional<String> locale = localeFormatter.getLocale();
        if (locale.isPresent()) {
            builder.appendQueryParameter(LOCALE_KEY, locale.get());
        }
    }

    private class WebProductSubscriber extends DefaultSubscriber<Optional<WebProduct>> {
        @Override
        public void onNext(Optional<WebProduct> result) {
            if (result.isPresent()) {
                saveProduct(result.get());
                launchWebForm(result.get());
            } else {
                setRetryState();
            }
        }

        @Override
        public void onError(Throwable e) {
            setRetryState();
        }
    }

    private void saveProduct(WebProduct product) {
        activity.getIntent().putExtra(WebConversionPresenter.PRODUCT_INFO, product);
    }

}
