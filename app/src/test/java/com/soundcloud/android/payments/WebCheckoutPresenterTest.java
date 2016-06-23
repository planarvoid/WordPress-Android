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
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.PurchaseEvent;
import com.soundcloud.android.events.TrackingEvent;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.utils.LocaleFormatter;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observable;

import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;

import java.io.IOException;

public class WebCheckoutPresenterTest extends AndroidUnitTest {

    @Mock private WebCheckoutView view;
    @Mock private AccountOperations accountOperations;
    @Mock private LocaleFormatter localeFormatter;
    @Mock private WebPaymentOperations paymentOperations;
    @Mock private Navigator navigator;
    @Mock private Resources resources;
    @Mock private AppCompatActivity activity;

    private static final WebProduct PRODUCT =
            WebProduct.create("high_tier", "some:product:123", "$2", "$1", "1.00", "USD", 30, "start", "expiry");

    private TestEventBus eventBus;
    private WebCheckoutPresenter presenter;

    @Before
    public void setUp() throws Exception {
        when(accountOperations.getSoundCloudToken()).thenReturn(Token.EMPTY);
        when(localeFormatter.getLocale()).thenReturn(Optional.of("en-GB"));

        eventBus = new TestEventBus();
        presenter = new WebCheckoutPresenter(view, accountOperations, localeFormatter, lazyOf(paymentOperations),
                                             navigator, eventBus, resources);
    }

    @Test
    public void loadsFormOnCreate() {
        setupIntentWithProduct();
        presenter.onCreate(activity, null);

        verify(view).loadUrl(any(String.class));
    }

    @Test
    public void loadsFormOnRetry() {
        setupIntentWithProduct();
        presenter.onCreate(activity, null);

        presenter.onRetry();

        verify(view, times(2)).loadUrl(any(String.class));
    }

    @Test
    public void loadProductIfNotPassedInIntent() {
        when(activity.getIntent()).thenReturn(new Intent());
        when(paymentOperations.product()).thenReturn(Observable.just(Optional.of(PRODUCT)));

        presenter.onCreate(activity, null);

        verify(view).loadUrl(any(String.class));
    }

    @Test
    public void loadingProductCanBeRetried() {
        when(activity.getIntent()).thenReturn(new Intent());
        when(paymentOperations.product()).thenReturn(Observable.<Optional<WebProduct>>error(new IOException()));

        presenter.onCreate(activity, null);

        verify(view).setRetry();
    }

    @Test
    public void loadedProductIsSavedInIntent() {
        Intent intent = new Intent();
        when(activity.getIntent()).thenReturn(intent);
        when(paymentOperations.product()).thenReturn(Observable.just(Optional.of(PRODUCT)));

        presenter.onCreate(activity, null);

        assertThat(intent.getParcelableExtra(WebConversionPresenter.PRODUCT_INFO)).isEqualTo(PRODUCT);
    }

    @Test
    public void paymentErrorsShouldBeTracked() {
        setupIntentWithProduct();
        presenter.onCreate(activity, null);
        presenter.onPaymentError("KHAAAAAAAAAAAAAAAAAAAN");

        final TrackingEvent event = eventBus.lastEventOn(EventQueue.TRACKING);
        assertThat(event.getKind()).isEqualTo(PaymentErrorEvent.KIND);
    }

    @Test
    public void successfulPaymentTriggersAccountUpgrade() {
        setupIntentWithProduct();
        presenter.onCreate(activity, null);

        presenter.onPaymentSuccess();

        verify(navigator).resetForAccountUpgrade(eq(activity));
    }

    @Test
    public void successfulPaymentTracksPurchaseForHighTierSub() {
        setupIntentWithProduct();
        presenter.onCreate(activity, null);

        presenter.onPaymentSuccess();

        eventBus.lastEventOn(EventQueue.TRACKING).getKind().equals(PurchaseEvent.KIND_HIGH_TIER_SUB);
    }

    @Test
    public void shouldBuildUrlThatIncludesCorrectQueryParamsWhenThereIsNoDiscount() {
        final WebProduct product = WebProduct.create("high_tier",
                                                     "some:product:123",
                                                     "$2",
                                                     null,
                                                     "2.00",
                                                     "USD",
                                                     30,
                                                     "start",
                                                     "expiry");
        final String token = "12345";
        final String environment = "test";
        final Uri actual = Uri.parse(presenter.buildPaymentFormUrl(token, product, environment));
        final Uri expected = Uri.parse(
                "https://soundcloud.com/android_payment.html?oauth_token=12345&price=%242&trial_days=30&expiry_date=expiry&package_urn=some%3Aproduct%3A123&env=test&locale=en-GB");

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void shouldBuildUrlThatIncludesCorrectQueryParamsWhenThereIsADiscount() {
        final WebProduct product = WebProduct.create("high_tier",
                                                     "some:product:123",
                                                     "$2",
                                                     "$1",
                                                     "1.00",
                                                     "USD",
                                                     30,
                                                     "start",
                                                     "expiry");
        final String token = "12345";
        final String environment = "test";
        final Uri actual = Uri.parse(presenter.buildPaymentFormUrl(token, product, environment));
        final Uri expected = Uri.parse(
                "https://soundcloud.com/android_payment.html?oauth_token=12345&price=%242&trial_days=30&expiry_date=expiry&package_urn=some%3Aproduct%3A123&env=test&discount_price=%241&locale=en-GB");

        assertThat(actual).isEqualTo(expected);
    }

    private void setupIntentWithProduct() {
        when(activity.getIntent()).thenReturn(new Intent().putExtra(WebConversionPresenter.PRODUCT_INFO, PRODUCT));
    }

}
