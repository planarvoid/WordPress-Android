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
import com.soundcloud.java.optional.Optional;
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

    private TestSubscriber<Optional<WebProduct>> subscriber = new TestSubscriber<>();

    @Before
    public void setUp() throws Exception {
        operations = new WebPaymentOperations(api, Schedulers.immediate());
    }

    @Test
    public void productReturnsWebProductIfPlanExists() {
        WebProduct expected = WebProduct.create("high_tier", "urn:123", "$1", "$0", 30, "now", "later");
        setupExpectedProductsCall(expected);

        operations.product().subscribe(subscriber);

        final Optional<WebProduct> product = subscriber.getOnNextEvents().get(0);
        assertThat(product.isPresent()).isTrue();
        assertThat(product.get()).isEqualTo(expected);
    }

    @Test
    public void productReturnsAbsentIfPlanDoesNotExist() {
        setupExpectedProductsCall(WebProduct.create("high_tears", "urn:123", "$1", "$0", 2, "now", "later"));

        operations.product().subscribe(subscriber);

        final Optional<WebProduct> product = subscriber.getOnNextEvents().get(0);
        assertThat(product.isPresent()).isFalse();
    }

    private void setupExpectedProductsCall(WebProduct expected) {
        final ModelCollection<WebProduct> webProducts = new ModelCollection<>(Collections.singletonList(expected));
        when(api.mappedResponse(argThat(isApiRequestTo("GET", ApiEndpoints.WEB_PRODUCTS.path())), any(TypeToken.class)))
                .thenReturn(Observable.just(webProducts));
    }
}
