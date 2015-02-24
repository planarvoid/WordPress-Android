package com.soundcloud.android.payments;

import static com.soundcloud.android.Expect.expect;

import com.soundcloud.android.robolectric.SoundCloudTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(SoundCloudTestRunner.class)
public class CheckoutUpdatedTest {

    @Test
    public void mapsPendingToStatus() {
        CheckoutUpdated result = new CheckoutUpdated("pending", "blah", "token");
        expect(CheckoutUpdated.TO_STATUS.call(result)).toEqual(PurchaseStatus.PENDING);
    }

    @Test
    public void mapsFailureToStatus() {
        CheckoutUpdated result = new CheckoutUpdated("failed", "blah", "token");
        expect(CheckoutUpdated.TO_STATUS.call(result)).toEqual(PurchaseStatus.VERIFY_FAIL);
    }

    @Test
    public void mapsSuccessToStatus() {
        CheckoutUpdated result = new CheckoutUpdated("successful", "blah", "token");
        expect(CheckoutUpdated.TO_STATUS.call(result)).toEqual(PurchaseStatus.SUCCESS);
    }

}