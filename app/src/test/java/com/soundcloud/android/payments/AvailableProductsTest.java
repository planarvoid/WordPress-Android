package com.soundcloud.android.payments;

import static com.soundcloud.android.payments.AvailableProducts.Product;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Java6Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;

public class AvailableProductsTest {

    private Product knownProduct;
    private Product unknownProduct;

    @Before
    public void setUp() throws Exception {
        knownProduct = new Product("product_id", "high_tier");
        unknownProduct = new Product("other_product_id", "super_consumer_sub");
    }

    @Test
    public void mapsAvailableProductListToKnownProductId() throws Exception {
        AvailableProducts products = new AvailableProducts(singletonList(knownProduct));
        Product mappedProduct = AvailableProducts.TO_PRODUCT.apply(products);
        assertThat(mappedProduct.id).isEqualTo("product_id");
    }

    @Test
    public void ignoresUnknownProducts() throws Exception {
        AvailableProducts products = new AvailableProducts(asList(unknownProduct, knownProduct));
        Product mappedProduct = AvailableProducts.TO_PRODUCT.apply(products);
        assertThat(mappedProduct.id).isEqualTo("product_id");
    }

    @Test
    public void mapsListOfUnknownProductsToEmptyProduct() throws Exception {
        AvailableProducts products = new AvailableProducts(singletonList(unknownProduct));
        Product mappedProduct = AvailableProducts.TO_PRODUCT.apply(products);
        assertThat(mappedProduct.isEmpty()).isTrue();
    }

    @Test
    public void mapsEmptyProductListToEmptyProduct() throws Exception {
        AvailableProducts products = new AvailableProducts(new ArrayList<>());
        Product mappedProduct = AvailableProducts.TO_PRODUCT.apply(products);
        assertThat(mappedProduct.isEmpty()).isTrue();
    }

}
