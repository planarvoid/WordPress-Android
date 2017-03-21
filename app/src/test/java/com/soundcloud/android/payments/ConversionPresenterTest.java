package com.soundcloud.android.payments;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.R;
import com.soundcloud.android.configuration.FeatureOperations;
import com.soundcloud.android.configuration.Plan;
import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.UpgradeFunnelEvent;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.Assertions;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.robolectric.shadows.ShadowDialog;
import rx.Observable;

import android.os.Bundle;
import android.os.Parcelable;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import java.io.IOException;
import java.util.Arrays;

public class ConversionPresenterTest extends AndroidUnitTest {

    private static final AvailableWebProducts DEFAULT = AvailableWebProducts.single(TestProduct.highTier());
    private static final AvailableWebProducts BOTH_PLANS = new AvailableWebProducts(Arrays.asList(TestProduct.midTier(), TestProduct.highTier()));
    private static final AvailableWebProducts PROMO = AvailableWebProducts.single(TestProduct.promoHighTier());
    private static final AvailableWebProducts INVALID = AvailableWebProducts.single(TestProduct.midTier());

    private TestEventBus eventBus = new TestEventBus();

    @Mock private WebPaymentOperations paymentOperations;
    @Mock private ConversionView view;
    @Mock private FeatureFlags featureFlags;
    @Mock private FeatureOperations featureOperations;

    private AppCompatActivity activity = activity();
    private ConversionPresenter presenter;

    @Before
    public void setUp() {
        when(featureOperations.isPlanManageable()).thenReturn(true);

        presenter = new ConversionPresenter(paymentOperations, view, eventBus, featureOperations);
    }

    @Test
    public void enablePurchaseOnProductLoad() {
        when(paymentOperations.products()).thenReturn(Observable.just(DEFAULT));

        presenter.onCreate(activity, null);

        verify(view).showDetails("$2", 30);
    }

    @Test
    public void enablePromoPurchaseOnPromoLoad() {
        when(paymentOperations.products()).thenReturn(Observable.just(PROMO));

        presenter.onCreate(activity, null);

        verify(view).showPromo("$1", 90, "$2");
    }

    @Test
    public void enablePurchaseIfProductSavedInBundle() {
        Bundle savedInstanceState = new Bundle();
        savedInstanceState.putParcelable(ConversionPresenter.LOADED_PRODUCTS, DEFAULT);

        presenter.onCreate(activity, savedInstanceState);

        verifyZeroInteractions(paymentOperations);
        verify(view).showDetails("$2", 30);
    }

    @Test
    public void savesLoadedProductsToBundle() {
        Bundle savedInstanceState = new Bundle();
        when(paymentOperations.products()).thenReturn(Observable.just(DEFAULT));
        presenter.onCreate(activity, null);

        presenter.onSaveInstanceState(activity, savedInstanceState);

        assertThat(savedInstanceState.<Parcelable>getParcelable(ConversionPresenter.LOADED_PRODUCTS)).isEqualTo(DEFAULT);
    }

    @Test
    public void allowsRetryIfProductLoadFails() {
        when(paymentOperations.products()).thenReturn(Observable.error(new IOException()));

        presenter.onCreate(activity, null);

        verify(view).showRetryState();
    }

    @Test
    public void showsPlanConversionErrorDialogForAppleError() {
        when(paymentOperations.products()).thenReturn(Observable.just(BOTH_PLANS));
        when(featureOperations.isPlanManageable()).thenReturn(false);
        when(featureOperations.isPlanVendorApple()).thenReturn(true);

        presenter.onCreate(activity, null);
        presenter.onPurchasePrimary();

        assertDialogMessage(resources().getString(R.string.plan_conversion_error_message_apple));
    }

    @Test
    public void showsPlanConversionErrorDialogForGenericError() {
        when(paymentOperations.products()).thenReturn(Observable.just(BOTH_PLANS));
        when(featureOperations.isPlanManageable()).thenReturn(false);
        when(featureOperations.isPlanVendorApple()).thenReturn(false);

        presenter.onCreate(activity, null);
        presenter.onPurchasePrimary();

        assertDialogMessage(resources().getString(R.string.plan_conversion_error_message_generic));
    }

    @Test
    public void startPurchaseCallbackPassesProductInfoToCheckout() {
        when(paymentOperations.products()).thenReturn(Observable.just(DEFAULT));
        presenter.onCreate(activity, null);

        presenter.onPurchasePrimary();

        Assertions.assertThat(activity)
                .nextStartedIntent()
                .containsExtra(WebCheckoutPresenter.PRODUCT_INFO, DEFAULT.highTier().get())
                .opensActivity(WebCheckoutActivity.class);
    }

    @Test
    public void startPurchasePublishesUpgradeFunnelEvent() {
        when(paymentOperations.products()).thenReturn(Observable.just(DEFAULT));
        presenter.onCreate(activity, null);

        presenter.onPurchasePrimary();

        final UpgradeFunnelEvent event = eventBus.lastEventOn(EventQueue.TRACKING, UpgradeFunnelEvent.class);

        assertThat(event.clickObject().get()).isEqualTo(UpgradeFunnelEvent.TCode.CONVERSION_BUY.code());
    }

    @Test
    public void startPurchaseWithPromoPublishesPromoFunnelEvent() {
        when(paymentOperations.products()).thenReturn(Observable.just(PROMO));
        presenter.onCreate(activity, null);

        presenter.onPurchasePrimary();

        final UpgradeFunnelEvent event = eventBus.lastEventOn(EventQueue.TRACKING, UpgradeFunnelEvent.class);

        assertThat(event.clickObject().get()).isEqualTo(UpgradeFunnelEvent.TCode.CONVERSION_PROMO.code());
    }

    @Test
    public void moreProductsButtonEnabledWhenUserIsFreeTierAndMidTierIsAvailable() {
        when(featureOperations.getCurrentPlan()).thenReturn(Plan.FREE_TIER);
        when(paymentOperations.products()).thenReturn(Observable.just(BOTH_PLANS));

        presenter.onCreate(activity, null);

        verify(view).showDetails("$2", 30);
        verify(view).enableMorePlans("$1");
    }

    @Test
    public void loadingFailsIfOnlyMidTierIsAvailable() {
        when(paymentOperations.products()).thenReturn(Observable.just(INVALID));

        presenter.onCreate(activity, null);

        verify(view).showRetryState();
    }

    @Test
    public void moreProductsCallbackOpensSelectionScreen() {
        when(paymentOperations.products()).thenReturn(Observable.just(DEFAULT));
        presenter.onCreate(activity, null);

        presenter.onMoreProducts();

        Assertions.assertThat(activity)
                .nextStartedIntent()
                .containsExtra(ProductChoiceActivity.AVAILABLE_PRODUCTS, DEFAULT)
                .opensActivity(ProductChoiceActivity.class);
    }

    private void assertDialogMessage(String message) {
        TextView messageTextView = (TextView) ShadowDialog.getLatestDialog().findViewById(R.id.custom_dialog_body);
        assertThat(messageTextView.getText()).isEqualTo(message);
    }

}
