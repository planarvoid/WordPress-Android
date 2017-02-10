package com.soundcloud.android.payments;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.UpgradeFunnelEvent;
import com.soundcloud.android.properties.FeatureFlags;
import com.soundcloud.android.properties.Flag;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.Assertions;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observable;

import android.os.Bundle;
import android.os.Parcelable;
import android.support.v7.app.AppCompatActivity;

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

    private AppCompatActivity activity = activity();
    private ConversionPresenter presenter;

    @Before
    public void setUp() {
        presenter = new ConversionPresenter(paymentOperations, view, eventBus, featureFlags);
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

        assertThat(event.clickObject().get()).isEqualTo(UpgradeFunnelEvent.TCode.CONVERSION_BUY.toString());
    }

    @Test
    public void startPurchaseWithPromoPublishesPromoFunnelEvent() {
        when(paymentOperations.products()).thenReturn(Observable.just(PROMO));
        presenter.onCreate(activity, null);

        presenter.onPurchasePrimary();

        final UpgradeFunnelEvent event = eventBus.lastEventOn(EventQueue.TRACKING, UpgradeFunnelEvent.class);

        assertThat(event.clickObject().get()).isEqualTo(UpgradeFunnelEvent.TCode.CONVERSION_PROMO.toString());
    }

    @Test
    public void moreProductsButtonEnabledWhenMidTierIsAvailable() {
        when(paymentOperations.products()).thenReturn(Observable.just(BOTH_PLANS));
        when(featureFlags.isEnabled(Flag.MID_TIER)).thenReturn(true);

        presenter.onCreate(activity, null);

        verify(view).showDetails("$2", 30);
        verify(view).enableMorePlans();
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

}
