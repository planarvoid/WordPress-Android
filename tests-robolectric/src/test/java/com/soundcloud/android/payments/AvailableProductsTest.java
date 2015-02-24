package com.soundcloud.android.payments;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.payments.AvailableProducts.Product;

import com.google.common.collect.Lists;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;

@RunWith(SoundCloudTestRunner.class)
public class AvailableProductsTest {

    private Product knownProduct;
    private Product unknownProduct;

    @Before
    public void setUp() throws Exception {
        knownProduct = new Product("product_id", "consumer_sub");
        unknownProduct = new Product("other_product_id", "super_consumer_sub");
    }

    @Test
    public void mapsAvailableProductListToKnownProductId() {
        AvailableProducts products = new AvailableProducts(Lists.newArrayList(knownProduct));
        Product mappedProduct = AvailableProducts.TO_PRODUCT.call(products);
        expect(mappedProduct.id).toEqual("product_id");
    }

    @Test
    public void ignoresUnknownProducts() {
        AvailableProducts products = new AvailableProducts(Lists.newArrayList(unknownProduct, knownProduct));
        Product mappedProduct = AvailableProducts.TO_PRODUCT.call(products);
        expect(mappedProduct.id).toEqual("product_id");
    }

    @Test
    public void mapsListOfUnknownProductsToEmptyProduct() {
        AvailableProducts products = new AvailableProducts(Lists.newArrayList(unknownProduct));
        Product mappedProduct = AvailableProducts.TO_PRODUCT.call(products);
        expect(mappedProduct.isEmpty()).toBeTrue();
    }

    @Test
    public void mapsEmptyProductListToEmptyProduct() {
        AvailableProducts products = new AvailableProducts(new ArrayList<Product>());
        Product mappedProduct = AvailableProducts.TO_PRODUCT.call(products);
        expect(mappedProduct.isEmpty()).toBeTrue();
    }

}