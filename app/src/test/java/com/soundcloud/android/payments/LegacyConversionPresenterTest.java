package com.soundcloud.android.payments;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.UpgradeFunnelEvent;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.Assertions;
import com.soundcloud.java.optional.Optional;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observable;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import java.io.IOException;

public class LegacyConversionPresenterTest extends AndroidUnitTest {

    private static final WebProduct DEFAULT = WebProduct.create("high_tier", "some:product:123", "$2", null, "2.00", "USD", 30, 0, null, "start", "expiry");
    private static final WebProduct PROMO = WebProduct.create("high_tier", "some:product:123", "$2", null, "2.00", "USD", 0, 90, "$1", "start", "expiry");

    private TestEventBus eventBus = new TestEventBus();

    @Mock private WebPaymentOperations paymentOperations;
    @Mock private ConversionView conversionView;

    private AppCompatActivity activity = new AppCompatActivity();
    private LegacyConversionPresenter presenter;

    @Before
    public void setUp() throws Exception {
        presenter = new LegacyConversionPresenter(paymentOperations, conversionView, eventBus);
    }

    @Test
    public void enablePurchaseOnProductLoad() {
        when(paymentOperations.product()).thenReturn(Observable.just(Optional.of(DEFAULT)));

        presenter.onCreate(activity, null);

        verify(conversionView).showPrice("$2", 30);
        verify(conversionView).setBuyButtonReady();
    }

    @Test
    public void enablePromoPurchaseOnPromoLoad() {
        when(paymentOperations.product()).thenReturn(Observable.just(Optional.of(PROMO)));

        presenter.onCreate(activity, null);

        verify(conversionView).showPromo("$1", 90, "$2");
        verify(conversionView).setBuyButtonReady();
    }

    @Test
    public void enablePurchaseIfProductSavedInBundle() {
        Bundle savedInstanceState = new Bundle();
        savedInstanceState.putParcelable(LegacyConversionPresenter.LOADED_PRODUCT, DEFAULT);

        presenter.onCreate(activity, savedInstanceState);

        verifyZeroInteractions(paymentOperations);
        verify(conversionView).showPrice("$2", 30);
        verify(conversionView).setBuyButtonReady();
    }

    @Test
    public void savesLoadedProductToBundle() {
        Bundle savedInstanceState = new Bundle();
        when(paymentOperations.product()).thenReturn(Observable.just(Optional.of(DEFAULT)));
        presenter.onCreate(activity, null);

        presenter.onSaveInstanceState(activity, savedInstanceState);

        assertThat(savedInstanceState.getParcelable(LegacyConversionPresenter.LOADED_PRODUCT)).isEqualTo(DEFAULT);
    }

    @Test
    public void allowsRetryIfProductLoadFails() {
        when(paymentOperations.product()).thenReturn(Observable.<Optional<WebProduct>>error(new IOException()));

        presenter.onCreate(activity, null);

        verify(conversionView).setBuyButtonRetry();
    }

    @Test
    public void startPurchaseCallbackPassesProductInfoToCheckout() {
        when(paymentOperations.product()).thenReturn(Observable.just(Optional.of(DEFAULT)));
        presenter.onCreate(activity, null);

        presenter.startPurchase();

        Assertions.assertThat(activity)
                  .nextStartedIntent()
                  .containsExtra(WebCheckoutPresenter.PRODUCT_INFO, DEFAULT)
                  .opensActivity(WebCheckoutActivity.class);
    }

    @Test
    public void startPurchasePublishesUpgradeFunnelEvent() {
        when(paymentOperations.product()).thenReturn(Observable.just(Optional.of(DEFAULT)));
        presenter.onCreate(activity, null);

        presenter.startPurchase();

        assertThat(eventBus.lastEventOn(EventQueue.TRACKING).get(UpgradeFunnelEvent.KEY_ID))
                .isEqualTo(UpgradeFunnelEvent.ID_UPGRADE_BUTTON);
    }

    @Test
    public void startPurchaseWithPromoPublishesPromoFunnelEvent() {
        when(paymentOperations.product()).thenReturn(Observable.just(Optional.of(PROMO)));
        presenter.onCreate(activity, null);

        presenter.startPurchase();

        assertThat(eventBus.lastEventOn(EventQueue.TRACKING).get(UpgradeFunnelEvent.KEY_ID))
                .isEqualTo(UpgradeFunnelEvent.ID_UPGRADE_PROMO);
    }

    @Test
    public void closeCallbackFinishesActivity() {
        when(paymentOperations.product()).thenReturn(Observable.<Optional<WebProduct>>empty());
        presenter.onCreate(activity, null);

        presenter.close();

        assertThat(activity.isFinishing()).isTrue();
    }

}
