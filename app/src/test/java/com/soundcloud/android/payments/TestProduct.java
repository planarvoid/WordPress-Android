package com.soundcloud.android.payments;

class TestProduct {

    private TestProduct() {}

    static WebProduct highTier() {
        return WebProduct.create("high_tier", "urn:123", "$2", null, "0.00", "USD", 30, 0, null, "now", "later");
    }

    static WebProduct highTierPromo() {
        return WebProduct.create("high_tier", "urn:123", "$2", "$0", "0.00", "USD", 0, 90, "$1", "now", "later");
    }

    static WebProduct midTier() {
        return WebProduct.create("mid_tier", "urn:123", "$1", null, "0.00", "USD", 30, 0, null, "now", "later");
    }

    static WebProduct unknown() {
        return WebProduct.create("high_tears", "urn:123", "$1", "$0", "0.00", "USD", 30, 0, null, "now", "later");
    }

}
