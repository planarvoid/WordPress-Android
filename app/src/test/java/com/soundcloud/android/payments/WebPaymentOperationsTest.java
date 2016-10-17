package com.soundcloud.android.payments;

import static com.soundcloud.android.testsupport.matchers.RequestMatchers.isApiRequestTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.when;

import com.soundcloud.android.api.ApiClientRx;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.java.reflect.TypeToken;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import rx.Observable;
import rx.observers.TestSubscriber;
import rx.schedulers.Schedulers;

import java.util.Collections;

public class WebPaymentOperationsTest extends AndroidUnitTest {

    private WebPaymentOperations operations;

    @Mock ApiClientRx api;

    private TestSubscriber<AvailableWebProducts> subscriber = new TestSubscriber<>();

    @Before
    public void setUp() throws Exception {
        operations = new WebPaymentOperations(api, Schedulers.immediate());
    }

    @Test
    public void fetchesAndMapsAvailableProducts() {
        WebProduct expected = TestProduct.highTier();
        setupExpectedProductsCall(expected);

        operations.products().subscribe(subscriber);

        final AvailableWebProducts products = subscriber.getOnNextEvents().get(0);
        assertThat(products.highTier().isPresent()).isTrue();
        assertThat(products.highTier().get()).isEqualTo(expected);
    }

    @Test
    public void returnsEmptyAvailableProductsIfNoKnownPlansAvailable() {
        setupExpectedProductsCall(TestProduct.unknown());

        operations.products().subscribe(subscriber);

        final AvailableWebProducts products = subscriber.getOnNextEvents().get(0);
        assertThat(products.highTier().isPresent()).isFalse();
    }

    private void setupExpectedProductsCall(WebProduct expected) {
        final ModelCollection<WebProduct> webProducts = new ModelCollection<>(Collections.singletonList(expected));
        when(api.mappedResponse(argThat(isApiRequestTo("GET", ApiEndpoints.WEB_PRODUCTS.path())), any(TypeToken.class)))
                .thenReturn(Observable.just(webProducts));
    }
}
