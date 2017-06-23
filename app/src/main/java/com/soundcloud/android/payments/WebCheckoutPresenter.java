package com.soundcloud.android.payments;

import com.soundcloud.android.navigation.IntentFactory;
import com.soundcloud.android.navigation.NavigationExecutor;
import com.soundcloud.android.R;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.configuration.PendingPlanOperations;
import com.soundcloud.android.configuration.Plan;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PurchaseEvent;
import com.soundcloud.android.events.UpgradeFunnelEvent;
import com.soundcloud.android.rx.RxUtils;
import com.soundcloud.android.rx.observers.DefaultSingleObserver;
import com.soundcloud.android.utils.ErrorUtils;
import com.soundcloud.android.utils.LocaleFormatter;
import com.soundcloud.android.utils.Log;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.lightcycle.DefaultActivityLightCycle;
import com.soundcloud.rx.eventbus.EventBusV2;
import dagger.Lazy;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;

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

    static final String PRODUCT_INFO = "product_info";

    private static final long TIMEOUT_MILLIS = TimeUnit.SECONDS.toMillis(15);

    private static final String PAYMENT_FORM_BASE_URL = "https://soundcloud.com/android_payment.html";
    private static final String OAUTH_TOKEN_KEY = "oauth_token";
    private static final String LOCALE_KEY = "locale";
    private static final String PRICE_KEY = "price";
    private static final String EXPIRY_DATE_KEY = "expiry_date";
    private static final String TRIAL_DAYS_KEY = "trial_days";
    private static final String PROMO_DAYS_KEY = "promo_days";
    private static final String PROMO_PRICE_KEY = "promo_price";
    private static final String PACKAGE_URN_KEY = "package_urn";
    private static final String PRORATED_PRICE_KEY = "prorated_price";
    private static final String DISCOUNT_PRICE_KEY = "discount_price";
    private static final String TIER_KEY = "tier";
    private static final String ENVIRONMENT_KEY = "env";

    private final WebCheckoutView view;
    private final AccountOperations operations;
    private final LocaleFormatter localeFormatter;
    private final Lazy<WebPaymentOperations> paymentOperations;
    private final PendingPlanOperations pendingPlanOperations;
    private final NavigationExecutor navigationExecutor;
    private final EventBusV2 eventBus;
    private final Resources resources;

    private Activity activity;

    private Disposable disposable = RxUtils.emptyDisposable();
    private Handler handler = new Handler();

    @Inject
    WebCheckoutPresenter(WebCheckoutView view,
                                AccountOperations operations,
                                LocaleFormatter localeFormatter,
                                Lazy<WebPaymentOperations> paymentOperations,
                                PendingPlanOperations pendingPlanOperations,
                                NavigationExecutor navigationExecutor,
                                EventBusV2 eventBus,
                                Resources resources) {
        this.view = view;
        this.operations = operations;
        this.localeFormatter = localeFormatter;
        this.paymentOperations = paymentOperations;
        this.pendingPlanOperations = pendingPlanOperations;
        this.navigationExecutor = navigationExecutor;
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
        return (WebProduct) activity.getIntent().getParcelableExtra(PRODUCT_INFO);
    }

    private void loadProduct() {
        view.setLoading(true);
        startTimeout();

        final WebProduct product = getProductFromIntent();
        if (product == null) {
            fetchProducts();
        } else {
            launchWebForm(product);
        }
    }

    private void fetchProducts() {
        disposable = paymentOperations.get().products()
                                        .observeOn(AndroidSchedulers.mainThread())
                                        .subscribeWith(new WebProductsObserver());
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
        activity.runOnUiThread(() -> view.setLoading(false));
    }

    @Override
    public void onPaymentSuccess() {
        eventBus.publish(EventQueue.TRACKING, UpgradeFunnelEvent.forUpgradeSuccess());
        trackPurchase();
        launchUpgrade();
        activity.finish();
    }

    private void launchUpgrade() {
        Plan targetPlan = Plan.fromId(getProductFromIntent().getPlanId());
        pendingPlanOperations.setPendingUpgrade(targetPlan);
        navigationExecutor.resetForAccountUpgrade(activity);
    }

    private void trackPurchase() {
        final WebProduct product = getProductFromIntent();
        if (product == null) {
            Log.e("Dropping purchase tracking event: no product found in Intent!?");
            return;
        }
        switch (Plan.fromId(product.getPlanId())) {
            case MID_TIER:
                eventBus.publish(EventQueue.TRACKING, PurchaseEvent.forMidTierSub(product.getPriceData().decimalString(), product.getPriceData().currency()));
                break;
            case HIGH_TIER:
                eventBus.publish(EventQueue.TRACKING, PurchaseEvent.forHighTierSub(product.getPriceData().decimalString(), product.getPriceData().currency()));
                break;
            default:
                ErrorUtils.handleSilentException(new IllegalStateException("Dropping purchase tracking event: failed to resolve tier from product"));
        }
    }

    @Override
    public void onPaymentError(String errorType) {
        eventBus.publish(EventQueue.TRACKING, PaymentErrorEvent.create(errorType));
    }

    boolean handleBackPress() {
        return view.handleBackPress();
    }

    @Override
    public void onDestroy(AppCompatActivity activity) {
        disposable.dispose();
        cancelTimeout();
        this.activity = null;
    }

    private void startTimeout() {
        handler.postDelayed(this::setRetryState, TIMEOUT_MILLIS);
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
                                       .appendQueryParameter(PRICE_KEY, product.getPriceData().format())
                                       .appendQueryParameter(TRIAL_DAYS_KEY, Integer.toString(product.getTrialDays()))
                                       .appendQueryParameter(EXPIRY_DATE_KEY, product.getExpiryDate())
                                       .appendQueryParameter(PACKAGE_URN_KEY, product.getPackageUrn())
                                       .appendQueryParameter(TIER_KEY, product.getPlanId())
                                       .appendQueryParameter(ENVIRONMENT_KEY, environment);

        appendDiscount(product, builder);
        appendPromo(product, builder);
        appendProratedPrice(product, builder);
        appendLocale(builder);

        return builder.toString();
    }

    private void appendDiscount(WebProduct product, Uri.Builder builder) {
        if (product.getDiscountPriceData().isPresent()) {
            builder.appendQueryParameter(DISCOUNT_PRICE_KEY, product.getDiscountPriceData().get().format());
        }
    }

    private void appendPromo(WebProduct product, Uri.Builder builder) {
        if (product.hasPromo()) {
            builder.appendQueryParameter(PROMO_DAYS_KEY, Integer.toString(product.getPromoDays()));
            builder.appendQueryParameter(PROMO_PRICE_KEY, product.getPromoPriceData().get().format());
        }
    }

    private void appendProratedPrice(WebProduct product, Uri.Builder builder) {
        if (product.getProratedPriceData().isPresent()) {
            builder.appendQueryParameter(PRORATED_PRICE_KEY, product.getProratedPriceData().get().format());
        }
    }

    private void appendLocale(Uri.Builder builder) {
        Optional<String> locale = localeFormatter.getLocale();
        if (locale.isPresent()) {
            builder.appendQueryParameter(LOCALE_KEY, locale.get());
        }
    }

    private class WebProductsObserver extends DefaultSingleObserver<AvailableWebProducts> {

        @Override
        public void onSuccess(@NonNull AvailableWebProducts products) {
            Optional<WebProduct> highTier = products.highTier();
            Optional<WebProduct> midTier = products.midTier();
            if (shouldLaunchWebFormForProduct(highTier)) {
                refreshWebForm(highTier.get());
            } else if (shouldLaunchWebFormForProduct(midTier)) {
                refreshWebForm(midTier.get());
            } else {
                setRetryState();
            }
            super.onSuccess(products);
        }

        @Override
        public void onError(Throwable e) {
            setRetryState();
        }
    }

    private boolean shouldLaunchWebFormForProduct(Optional<WebProduct> product) {
        return product.isPresent() && Plan.fromId(product.get().getPlanId()) == getIntentCheckoutPlan();
    }

    private Plan getIntentCheckoutPlan() {
        if (activity.getIntent().hasExtra(IntentFactory.EXTRA_CHECKOUT_PLAN)) {
            return (Plan) activity.getIntent().getSerializableExtra(IntentFactory.EXTRA_CHECKOUT_PLAN);
        }
        return Plan.UNDEFINED;
    }

    private void refreshWebForm(WebProduct product) {
        saveProduct(product);
        launchWebForm(product);
    }

    private void saveProduct(WebProduct product) {
        activity.getIntent().putExtra(PRODUCT_INFO, product);
    }

}
