package com.soundcloud.android.payments;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(SoundCloudTestRunner.class)
public class ProductStatusTest {

    @Test
    public void successFunctionMapsDetailsToStatus() {
        ProductDetails details = new ProductDetails("id", "Subscription", "Blah", "$100");

        ProductStatus status = ProductStatus.SUCCESS.call(details);

        expect(status.isSuccess()).toBeTrue();
        expect(status.getDetails()).toBe(details);
    }

    @Test
    public void noProductStatusIsFailure() {
        ProductStatus noProduct = ProductStatus.fromNoProduct();
        expect(noProduct.isSuccess()).toBeFalse();
        expect(noProduct.getDetails()).toBeNull();
    }

}