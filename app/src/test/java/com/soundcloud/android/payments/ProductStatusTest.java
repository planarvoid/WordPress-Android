package com.soundcloud.android.payments;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class ProductStatusTest {

    @Test
    public void successFunctionMapsDetailsToStatus() {
        ProductDetails details = new ProductDetails("id", "Subscription", "Blah", "$100");

        ProductStatus status = ProductStatus.SUCCESS.call(details);

        assertThat(status.isSuccess()).isTrue();
        assertThat(status.getDetails()).isSameAs(details);
    }

    @Test
    public void noProductStatusIsFailure() {
        ProductStatus noProduct = ProductStatus.fromNoProduct();
        assertThat(noProduct.isSuccess()).isFalse();
        assertThat(noProduct.getDetails()).isNull();
    }

}