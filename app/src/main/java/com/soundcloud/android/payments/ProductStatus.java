package com.soundcloud.android.payments;

import org.jetbrains.annotations.Nullable;
import rx.functions.Func1;

final class ProductStatus {

    public static final Func1<ProductDetails, ProductStatus> SUCCESS = new Func1<ProductDetails, ProductStatus>() {
        @Override
        public ProductStatus call(ProductDetails details) {
            return ProductStatus.fromSuccess(details);
        }
    };

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
