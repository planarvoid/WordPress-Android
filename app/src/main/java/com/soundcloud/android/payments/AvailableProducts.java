package com.soundcloud.android.payments;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import rx.functions.Func1;

import java.util.List;

class AvailableProducts {

    public static final Func1<AvailableProducts, Product> TO_PRODUCT = new Func1<AvailableProducts, Product>() {
        @Override
        public Product call(AvailableProducts availableProducts) {
            return availableProducts.hasProduct()
                    ? availableProducts.products.get(0)
                    : Product.empty();
        }
    };

    public final List<Product> products;

    @JsonCreator
    public AvailableProducts(@JsonProperty("products") List<Product> products) {
        this.products = products;
    }

    public boolean hasProduct() {
        return !products.isEmpty();
    }

    public static class Product {

        private static final String EMPTY = "unavailable";

        public final String id;

        @JsonCreator
        public Product(@JsonProperty("id") String id) {
            this.id = id;
        }

        public static Product empty() {
            return new Product(EMPTY);
        }

        public boolean isEmpty() {
            return id.equals(EMPTY);
        }
    }

}
