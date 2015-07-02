package com.soundcloud.android.payments;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class CheckoutUpdatedTest {

    @Test
    public void mapsPendingToStatus() {
        CheckoutUpdated result = new CheckoutUpdated("pending", "blah", "token");
        assertThat(CheckoutUpdated.TO_STATUS.call(result)).isEqualTo(PurchaseStatus.PENDING);
    }

    @Test
    public void mapsFailureToStatus() {
        CheckoutUpdated result = new CheckoutUpdated("failed", "blah", "token");
        assertThat(CheckoutUpdated.TO_STATUS.call(result)).isEqualTo(PurchaseStatus.VERIFY_FAIL);
    }

    @Test
    public void mapsSuccessToStatus() {
        CheckoutUpdated result = new CheckoutUpdated("successful", "blah", "token");
        assertThat(CheckoutUpdated.TO_STATUS.call(result)).isEqualTo(PurchaseStatus.SUCCESS);
    }

}