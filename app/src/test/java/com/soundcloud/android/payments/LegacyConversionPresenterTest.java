package com.soundcloud.android.payments;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import com.soundcloud.android.events.EventQueue;
import com.soundcloud.android.events.UpgradeFunnelEvent;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.android.testsupport.Assertions;
import com.soundcloud.rx.eventbus.TestEventBus;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observable;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import java.io.IOException;

public class LegacyConversionPresenterTest extends AndroidUnitTest {

    private static final AvailableWebProducts DEFAULT = AvailableWebProducts.single(TestProduct.highTier());
    private static final AvailableWebProducts PROMO = AvailableWebProducts.single(TestProduct.highTierPromo());

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
        when(paymentOperations.products()).thenReturn(Observable.just(DEFAULT));

        presenter.onCreate(activity, null);

        verify(conversionView).showPrice("$2", 30);
        verify(conversionView).setBuyButtonReady();
    }

    @Test
    public void enablePromoPurchaseOnPromoLoad() {
        when(paymentOperations.products()).thenReturn(Observable.just(PROMO));

        presenter.onCreate(activity, null);

        verify(conversionView).showPromo("$1", 90, "$2");
        verify(conversionView).setBuyButtonReady();
    }

    @Test
    public void enablePurchaseIfProductSavedInBundle() {
        Bundle savedInstanceState = new Bundle();
        savedInstanceState.putParcelable(LegacyConversionPresenter.LOADED_PRODUCT, TestProduct.highTier());

        presenter.onCreate(activity, savedInstanceState);

        verifyZeroInteractions(paymentOperations);
        verify(conversionView).showPrice("$2", 30);
        verify(conversionView).setBuyButtonReady();
    }

    @Test
    public void savesLoadedProductToBundle() {
        Bundle savedInstanceState = new Bundle();
        when(paymentOperations.products()).thenReturn(Observable.just(DEFAULT));
        presenter.onCreate(activity, null);

        presenter.onSaveInstanceState(activity, savedInstanceState);

        assertThat(savedInstanceState.getParcelable(LegacyConversionPresenter.LOADED_PRODUCT)).isEqualTo(DEFAULT.highTier().get());
    }

    @Test
    public void allowsRetryIfProductLoadFails() {
        when(paymentOperations.products()).thenReturn(Observable.<AvailableWebProducts>error(new IOException()));

        presenter.onCreate(activity, null);

        verify(conversionView).setBuyButtonRetry();
    }

    @Test
    public void startPurchaseCallbackPassesProductInfoToCheckout() {
        when(paymentOperations.products()).thenReturn(Observable.just(DEFAULT));
        presenter.onCreate(activity, null);

        presenter.startPurchase();

        Assertions.assertThat(activity)
                  .nextStartedIntent()
                  .containsExtra(WebCheckoutPresenter.PRODUCT_INFO, DEFAULT.highTier().get())
                  .opensActivity(WebCheckoutActivity.class);
    }

    @Test
    public void startPurchasePublishesUpgradeFunnelEvent() {
        when(paymentOperations.products()).thenReturn(Observable.just(DEFAULT));
        presenter.onCreate(activity, null);

        presenter.startPurchase();

        assertThat(eventBus.lastEventOn(EventQueue.TRACKING).get(UpgradeFunnelEvent.KEY_ID))
                .isEqualTo(UpgradeFunnelEvent.ID_UPGRADE_BUTTON);
    }

    @Test
    public void startPurchaseWithPromoPublishesPromoFunnelEvent() {
        when(paymentOperations.products()).thenReturn(Observable.just(PROMO));
        presenter.onCreate(activity, null);

        presenter.startPurchase();

        assertThat(eventBus.lastEventOn(EventQueue.TRACKING).get(UpgradeFunnelEvent.KEY_ID))
                .isEqualTo(UpgradeFunnelEvent.ID_UPGRADE_PROMO);
    }

    @Test
    public void closeCallbackFinishesActivity() {
        when(paymentOperations.products()).thenReturn(Observable.<AvailableWebProducts>empty());
        presenter.onCreate(activity, null);

        presenter.close();

        assertThat(activity.isFinishing()).isTrue();
    }

}
