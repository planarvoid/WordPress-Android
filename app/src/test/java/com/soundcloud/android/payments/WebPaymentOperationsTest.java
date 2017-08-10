package com.soundcloud.android.payments;

import static com.soundcloud.android.testsupport.matchers.RequestMatchers.isApiRequestTo;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;

import com.soundcloud.android.api.ApiClientRxV2;
import com.soundcloud.android.api.ApiEndpoints;
import com.soundcloud.android.api.model.ModelCollection;
import com.soundcloud.android.testsupport.AndroidUnitTest;
import com.soundcloud.java.reflect.TypeToken;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.Collections;

public class WebPaymentOperationsTest extends AndroidUnitTest {

    @Mock private ApiClientRxV2 api;

    private WebPaymentOperations operations;

    @Before
    public void setUp() throws Exception {
        operations = new WebPaymentOperations(api, Schedulers.trampoline());
    }

    @Test
    public void fetchesAndMapsAvailableProducts() {
        WebProduct expected = TestProduct.highTier();
        setupExpectedProductsCall(expected);

        final AvailableWebProducts products = operations.products().test().values().get(0);

        assertThat(products.highTier().isPresent()).isTrue();
        assertThat(products.highTier().get()).isEqualTo(expected);
    }

    @Test
    public void returnsEmptyAvailableProductsIfNoKnownPlansAvailable() {
        setupExpectedProductsCall(TestProduct.unknown());

        final AvailableWebProducts products = operations.products().test().values().get(0);

        assertThat(products.highTier().isPresent()).isFalse();
    }

    private void setupExpectedProductsCall(WebProduct expected) {
        final ModelCollection<WebProduct> webProducts = new ModelCollection<>(Collections.singletonList(expected));
        when(api.mappedResponse(argThat(isApiRequestTo("GET", ApiEndpoints.WEB_PRODUCTS.path())), any(TypeToken.class)))
                .thenReturn(Single.just(webProducts));
    }
}
