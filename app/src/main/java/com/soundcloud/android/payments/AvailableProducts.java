package com.soundcloud.android.payments;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import rx.functions.Func1;

import java.util.List;

class AvailableProducts {

    private static final String MID_TIER = "mid_tier";

    public static final Func1<AvailableProducts, Product> TO_PRODUCT = new Func1<AvailableProducts, Product>() {
        @Override
        public Product call(AvailableProducts availableProducts) {
            for (Product product : availableProducts.products) {
                if (product.planId.equals(MID_TIER)) {
                    return product;
                }
            }
            return Product.empty();
        }
    };

    public final List<Product> products;

    @JsonCreator
    public AvailableProducts(@JsonProperty("collection") List<Product> products) {
        this.products = products;
    }

    public static class Product {

        private static final String EMPTY = "unavailable";

        public final String id;
        public final String planId;

        @JsonCreator
        public Product(@JsonProperty("id") String id, @JsonProperty("plan_id") String planId) {
            this.id = id;
            this.planId = planId;
        }

        public static Product empty() {
            return new Product(EMPTY, EMPTY);
        }

        public boolean isEmpty() {
            return id.equals(EMPTY);
        }
    }

}
