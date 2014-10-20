package com.soundcloud.android.payments;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import rx.Observable;

@RunWith(SoundCloudTestRunner.class)
public class ProductStatusTest {

    @Test
    public void successFunctionMapsDetailsToStatus() {
        ProductDetails details = new ProductDetails("id", "Subscription", "Blah", "$100");

        Observable<ProductStatus> mappedDetails = Observable.just(details).map(ProductStatus.SUCCESS);

        ProductStatus status = mappedDetails.toBlocking().first();
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