package com.soundcloud.android.payments;

class TestProduct {

    private TestProduct() {}

    static WebProduct highTier() {
        return WebProduct.create("high_tier", "urn:123", "$2", null, "2.00", "USD", 30, 0, null, null, "now", "later");
    }

    static WebProduct discountHighTier() {
        return WebProduct.create("high_tier", "urn:123", "$2", "$1", "1.00", "USD", 30, 0, null, null, "now", "later");
    }

    static WebProduct proratedHighTier() {
        return WebProduct.create("high_tier", "urn:123", "$2", null, "2.00", "USD", 30, 0, null, "$0.50", "now", "later");
    }

    static WebProduct promoHighTier() {
        return WebProduct.create("high_tier", "urn:123", "$2", null, "2.00", "USD", 0, 90, "$1", null, "now", "later");
    }

    static WebProduct midTier() {
        return WebProduct.create("mid_tier", "urn:123", "$1", null, "1.00", "USD", 30, 0, null, null, "now", "later");
    }

    static WebProduct unknown() {
        return WebProduct.create("high_tears", "urn:123", "$1", "$0", "1.00", "USD", 30, 0, null, null, "now", "later");
    }

}
