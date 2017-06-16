package com.soundcloud.android.payments;

import org.jetbrains.annotations.Nullable;

final class ProductStatus {

    private final ProductDetails details;

    private ProductStatus(ProductDetails details) {
        this.details = details;
    }

    static ProductStatus fromSuccess(ProductDetails details) {
        return new ProductStatus(details);
    }

    static ProductStatus fromNoProduct() {
        return new ProductStatus(null);
    }

    public boolean isSuccess() {
        return details != null;
    }

    @Nullable
    public ProductDetails getDetails() {
        return details;
    }

}
