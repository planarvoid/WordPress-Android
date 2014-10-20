package com.soundcloud.android.payments;

import static com.soundcloud.android.Expect.expect;
import static com.soundcloud.android.payments.AvailableProducts.Product;

import com.google.common.collect.Lists;
import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import rx.Observable;

import java.util.ArrayList;

@RunWith(SoundCloudTestRunner.class)
public class AvailableProductsTest {

    @Test
    public void mapsAvailableProductListToFirstProduct() {
        AvailableProducts products = new AvailableProducts(Lists.newArrayList(new Product("product_id")));
        Product mappedProduct = Observable.just(products).map(AvailableProducts.TO_PRODUCT).toBlocking().first();
        expect(mappedProduct.id).toEqual("product_id");
    }

    @Test
    public void mapsEmptyProductListToEmptyProduct() {
        AvailableProducts products = new AvailableProducts(new ArrayList<Product>());
        Product mappedProduct = Observable.just(products).map(AvailableProducts.TO_PRODUCT).toBlocking().first();
        expect(mappedProduct.isEmpty()).toBeTrue();
    }

}