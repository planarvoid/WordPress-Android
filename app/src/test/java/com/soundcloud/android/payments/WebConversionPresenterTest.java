package com.soundcloud.android.payments;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

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

public class WebConversionPresenterTest extends AndroidUnitTest {

    @Mock private WebPaymentOperations paymentOperations;
    @Mock private ConversionView conversionView;

    private AppCompatActivity activity = new AppCompatActivity();

    private WebProduct product;
    private WebConversionPresenter presenter;

    @Before
    public void setUp() throws Exception {
        presenter = new WebConversionPresenter(paymentOperations, conversionView, new TestEventBus());
        product = WebProduct.create("high_tier", "some:product:123", "$2", null, 30, "start", "expiry");
    }

    @Test
    public void enablePurchaseOnProductLoad() {
        when(paymentOperations.product()).thenReturn(Observable.just(Optional.of(product)));

        presenter.onCreate(activity, null);

        verify(conversionView).showPrice("$2");
        verify(conversionView).setBuyButtonReady();
    }

    @Test
    public void enablePurchaseIfProductSavedInBundle() {
        Bundle savedInstanceState = new Bundle();
        savedInstanceState.putParcelable(WebConversionPresenter.PRODUCT_INFO, product);

        presenter.onCreate(activity, savedInstanceState);

        verifyZeroInteractions(paymentOperations);
        verify(conversionView).showPrice("$2");
        verify(conversionView).setBuyButtonReady();
    }

    @Test
    public void savesLoadedProductToBundle() {
        Bundle savedInstanceState = new Bundle();
        when(paymentOperations.product()).thenReturn(Observable.just(Optional.of(product)));
        presenter.onCreate(activity, null);

        presenter.onSaveInstanceState(activity, savedInstanceState);

        assertThat(savedInstanceState.getParcelable(WebConversionPresenter.PRODUCT_INFO)).isEqualTo(product);
    }

    @Test
    public void allowsRetryIfProductLoadFails() {
        when(paymentOperations.product()).thenReturn(Observable.<Optional<WebProduct>>error(new IOException()));

        presenter.onCreate(activity, null);

        verify(conversionView).setBuyButtonRetry();
    }

    @Test
    public void startPurchaseCallbackPassesProductInfoToCheckout() {
        when(paymentOperations.product()).thenReturn(Observable.just(Optional.of(product)));
        presenter.onCreate(activity, null);

        presenter.startPurchase();

        Assertions.assertThat(activity)
                .nextStartedIntent()
                .containsExtra(WebConversionPresenter.PRODUCT_INFO, product)
                .opensActivity(WebCheckoutActivity.class);
    }

    @Test
    public void closeCallbackFinishesActivity() {
        when(paymentOperations.product()).thenReturn(Observable.<Optional<WebProduct>>empty());
        presenter.onCreate(activity, null);

        presenter.close();

        assertThat(activity.isFinishing()).isTrue();
    }

}
