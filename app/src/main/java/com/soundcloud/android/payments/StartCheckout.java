package com.soundcloud.android.payments;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Objects;

final class StartCheckout {

    @JsonProperty("product_id")
    public final String productId;

    public StartCheckout(String productId) {
        this.productId = productId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o == null || getClass() != o.getClass()) {
            return false;
        }
        return Objects.equal(productId, ((StartCheckout) o).productId);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(productId);
    }

}
