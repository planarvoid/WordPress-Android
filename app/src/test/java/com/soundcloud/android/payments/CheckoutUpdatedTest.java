package com.soundcloud.android.payments;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class CheckoutUpdatedTest {

    @Test
    public void mapsPendingToStatus() throws Exception {
        CheckoutUpdated result = new CheckoutUpdated("pending", "blah", "token");
        assertThat(CheckoutUpdated.TO_STATUS.apply(result)).isEqualTo(PurchaseStatus.PENDING);
    }

    @Test
    public void mapsFailureToStatus() throws Exception {
        CheckoutUpdated result = new CheckoutUpdated("failed", "blah", "token");
        assertThat(CheckoutUpdated.TO_STATUS.apply(result)).isEqualTo(PurchaseStatus.VERIFY_FAIL);
    }

    @Test
    public void mapsSuccessToStatus() throws Exception {
        CheckoutUpdated result = new CheckoutUpdated("successful", "blah", "token");
        assertThat(CheckoutUpdated.TO_STATUS.apply(result)).isEqualTo(PurchaseStatus.SUCCESS);
    }

}