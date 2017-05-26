package com.soundcloud.android.payments;

class TestProduct {

    private TestProduct() {}

    static WebProduct highTier() {
        return WebProduct.create("high_tier", "urn:123", WebPrice.create(200, "USD"), null, 30, 0, null, null, "now", "later");
    }

    static WebProduct discountHighTier() {
        return WebProduct.create("high_tier", "urn:123", WebPrice.create(200, "USD"), WebPrice.create(100, "USD"), 30, 0, null, null, "now", "later");
    }

    static WebProduct proratedHighTier() {
        return WebProduct.create("high_tier", "urn:123", WebPrice.create(200, "USD"), null, 30, 0, null, WebPrice.create(50, "USD"), "now", "later");
    }

    static WebProduct promoHighTier() {
        return WebProduct.create("high_tier", "urn:123", WebPrice.create(200, "USD"), null, 0, 90, WebPrice.create(100, "USD"), null, "now", "later");
    }

    static WebProduct midTier() {
        return WebProduct.create("mid_tier", "urn:123", WebPrice.create(100, "USD"), null, 30, 0, null, null, "now", "later");
    }

    static WebProduct unknown() {
        return WebProduct.create("high_tears", "urn:123", WebPrice.create(100, "USD"), WebPrice.create(100, "USD"), 30, 0, null, null, "now", "later");
    }

}
