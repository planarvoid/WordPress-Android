package com.soundcloud.android.payments;

import static org.assertj.core.api.Assertions.assertThat;

import org.assertj.core.util.Lists;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.Collections;

@RunWith(MockitoJUnitRunner.class)
public class AvailableWebProductsTest {

    @Test
    public void mapsSingleProduct() {
        WebProduct highTier = TestProduct.highTier();
        final AvailableWebProducts products = new AvailableWebProducts(Collections.singletonList(highTier));

        assertThat(products.midTier().isPresent()).isFalse();
        assertThat(products.highTier().get()).isEqualTo(highTier);
    }

    @Test
    public void mapsAllProducts() {
        final AvailableWebProducts products = new AvailableWebProducts(Arrays.asList(
                TestProduct.highTier(),
                TestProduct.midTier(),
                TestProduct.unknown()));

        assertThat(products.midTier().isPresent()).isTrue();
        assertThat(products.highTier().isPresent()).isTrue();
    }

    @Test
    public void mapsEmptyProducts() throws Exception {
        final AvailableWebProducts products = new AvailableWebProducts(Lists.<WebProduct>emptyList());

        assertThat(products.midTier().isPresent()).isFalse();
        assertThat(products.highTier().isPresent()).isFalse();
    }

}