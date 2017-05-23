package com.soundcloud.android.payments;

import static com.soundcloud.android.testsupport.InjectionSupport.lazyOf;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.soundcloud.android.Navigator;
import com.soundcloud.android.accounts.AccountOperations;
import com.soundcloud.android.api.oauth.Token;
import com.soundcloud.android.configuration.PendingPlanOperations;
import com.soundcloud.android.configuration.Plan;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PurchaseEvent;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.utils.LocaleFormatter;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.rx.eventbus.TestEventBus;
import io.reactivex.Single;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Parcelable;
import android.support.v7.app.AppCompatActivity;

import java.io.IOException;

public class WebCheckoutPresenterTest extends AndroidUnitTest {

    @Mock private WebCheckoutView view;
    @Mock private AccountOperations accountOperations;
    @Mock private LocaleFormatter localeFormatter;
    @Mock private WebPaymentOperations paymentOperations;
    @Mock private PendingPlanOperations pendingPlanOperations;
    @Mock private Navigator navigator;
    @Mock private Resources resources;
    @Mock private AppCompatActivity activity;

    private static final AvailableWebProducts HIGH_TIER = AvailableWebProducts.single(TestProduct.highTier());
    private static final AvailableWebProducts MID_TIER = AvailableWebProducts.single(TestProduct.midTier());

    private TestEventBus eventBus;
    private WebCheckoutPresenter presenter;

    @Before
    public void setUp() throws Exception {
        when(accountOperations.getSoundCloudToken()).thenReturn(Token.EMPTY);
        when(localeFormatter.getLocale()).thenReturn(Optional.of("en-GB"));

        eventBus = new TestEventBus();
        presenter = new WebCheckoutPresenter(view, accountOperations, localeFormatter, lazyOf(paymentOperations),
                                             pendingPlanOperations, navigator, eventBus, resources);
    }

    @Test
    public void loadsFormOnCreate() {
        setupIntentWithProduct(TestProduct.highTier());
        presenter.onCreate(activity, null);

        verify(view).loadUrl(any(String.class));
    }

    @Test
    public void loadsFormOnRetry() {
        setupIntentWithProduct(TestProduct.highTier());
        presenter.onCreate(activity, null);

        presenter.onRetry();

        verify(view, times(2)).loadUrl(any(String.class));
    }

    @Test
    public void loadHighTierProductIfNotPassedInIntent() {
        Intent intent = new Intent();
        intent.putExtra(Navigator.EXTRA_CHECKOUT_PLAN, Plan.HIGH_TIER);
        when(activity.getIntent()).thenReturn(intent);
        when(paymentOperations.products()).thenReturn(Single.just(HIGH_TIER));

        presenter.onCreate(activity, null);

        verify(view).loadUrl(any(String.class));
    }

    @Test
    public void loadMidTierProductIfNotPassedInIntent() {
        Intent intent = new Intent();
        intent.putExtra(Navigator.EXTRA_CHECKOUT_PLAN, Plan.MID_TIER);
        when(activity.getIntent()).thenReturn(intent);
        when(paymentOperations.products()).thenReturn(Single.just(MID_TIER));

        presenter.onCreate(activity, null);

        verify(view).loadUrl(any(String.class));
    }

    @Test
    public void loadingProductCanBeRetriedOnNoExtraCheckoutPlan() {
        when(activity.getIntent()).thenReturn(new Intent());
        when(paymentOperations.products()).thenReturn(Single.just(HIGH_TIER));

        presenter.onCreate(activity, null);

        verify(view).setRetry();
    }

    @Test
    public void loadingProductCanBeRetriedOnIOException() {
        when(activity.getIntent()).thenReturn(new Intent());
        when(paymentOperations.products()).thenReturn(Single.error(new IOException()));

        presenter.onCreate(activity, null);

        verify(view).setRetry();
    }

    @Test
    public void loadedHighTierProductIsSavedInIntent() {
        Intent intent = new Intent();
        intent.putExtra(Navigator.EXTRA_CHECKOUT_PLAN, Plan.HIGH_TIER);
        when(activity.getIntent()).thenReturn(intent);
        when(paymentOperations.products()).thenReturn(Single.just(HIGH_TIER));

        presenter.onCreate(activity, null);

        assertThat(intent.<Parcelable>getParcelableExtra(WebCheckoutPresenter.PRODUCT_INFO)).isEqualTo(HIGH_TIER.highTier().get());
    }

    @Test
    public void paymentErrorsShouldBeTracked() {
        setupIntentWithProduct(TestProduct.highTier());
        presenter.onCreate(activity, null);
        final String errorType = "KHAAAAAAAAAAAAAAAAAAAN";
        presenter.onPaymentError(errorType);

        final PaymentErrorEvent event = (PaymentErrorEvent) eventBus.lastEventOn(EventQueue.TRACKING);
        assertThat(event.errorType()).isEqualTo(errorType);
    }

    @Test
    public void successfulMidTierPaymentTriggersAccountUpgrade() {
        setupIntentWithProduct(TestProduct.midTier());
        presenter.onCreate(activity, null);

        presenter.onPaymentSuccess();

        verify(pendingPlanOperations).setPendingUpgrade(Plan.MID_TIER);
        verify(navigator).resetForAccountUpgrade(eq(activity));
    }

    @Test
    public void successfulHighTierPaymentTriggersAccountUpgrade() {
        setupIntentWithProduct(TestProduct.highTier());
        presenter.onCreate(activity, null);

        presenter.onPaymentSuccess();

        verify(pendingPlanOperations).setPendingUpgrade(Plan.HIGH_TIER);
        verify(navigator).resetForAccountUpgrade(eq(activity));
    }

    @Test
    public void successfulPaymentTracksPurchaseForMidTierSub() {
        setupIntentWithProduct(TestProduct.midTier());
        presenter.onCreate(activity, null);

        presenter.onPaymentSuccess();

        assertThat(eventBus.lastEventOn(EventQueue.TRACKING).getKind()).isEqualTo(PurchaseEvent.Subscription.MID_TIER.toString());
    }

    @Test
    public void successfulPaymentTracksPurchaseForHighTierSub() {
        setupIntentWithProduct(TestProduct.highTier());
        presenter.onCreate(activity, null);

        presenter.onPaymentSuccess();

        assertThat(eventBus.lastEventOn(EventQueue.TRACKING).getKind()).isEqualTo(PurchaseEvent.Subscription.HIGH_TIER.toString());
    }

    @Test
    public void shouldBuildUrlThatIncludesCorrectQueryParamsWhenThereIsNoDiscount() {
        final WebProduct product = TestProduct.highTier();
        final String token = "12345";
        final String environment = "test";

        final Uri actual = Uri.parse(presenter.buildPaymentFormUrl(token, product, environment));

        final Uri expected = Uri.parse("https://soundcloud.com/android_payment.html?oauth_token=12345&price=%242&trial_days=30&expiry_date=later&package_urn=urn%3A123&tier=high_tier&env=test&locale=en-GB");
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void shouldBuildUrlThatIncludesCorrectQueryParamsWhenThereIsADiscount() {
        final WebProduct product = TestProduct.discountHighTier();
        final String token = "12345";
        final String environment = "test";

        final Uri actual = Uri.parse(presenter.buildPaymentFormUrl(token, product, environment));

        final Uri expected = Uri.parse("https://soundcloud.com/android_payment.html?oauth_token=12345&price=%242&trial_days=30&expiry_date=later&package_urn=urn%3A123&tier=high_tier&env=test&discount_price=%241&locale=en-GB");
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void shouldBuildUrlThatIncludesCorrectQueryParamsWhenThereIsAPromo() {
        final WebProduct product = TestProduct.promoHighTier();
        final String token = "12345";
        final String environment = "test";

        final Uri actual = Uri.parse(presenter.buildPaymentFormUrl(token, product, environment));

        final Uri expected = Uri.parse("https://soundcloud.com/android_payment.html?oauth_token=12345&price=%242&trial_days=0&expiry_date=later&package_urn=urn%3A123&tier=high_tier&env=test&promo_days=90&promo_price=%241&locale=en-GB");
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void shouldBuildUrlThatIncludesCorrectQueryParamsWhenThereIsAProratedPrice() {
        final WebProduct product = TestProduct.proratedHighTier();
        final String token = "12345";
        final String environment = "test";

        final Uri actual = Uri.parse(presenter.buildPaymentFormUrl(token, product, environment));

        final Uri expected = Uri.parse("https://soundcloud.com/android_payment.html?oauth_token=12345&price=%242&trial_days=30&expiry_date=later&package_urn=urn%3A123&tier=high_tier&env=test&prorated_price=%240.50&locale=en-GB");
        assertThat(actual).isEqualTo(expected);
    }

    private void setupIntentWithProduct(WebProduct product) {
        when(activity.getIntent()).thenReturn(new Intent().putExtra(WebCheckoutPresenter.PRODUCT_INFO, product));
    }

}
