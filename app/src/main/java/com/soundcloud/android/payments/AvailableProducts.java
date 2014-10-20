package com.soundcloud.android.payments;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

class AvailableProducts {

    public final List<Product> products;

    @JsonCreator
    public AvailableProducts(@JsonProperty("products") List<Product> products) {
        this.products = products;
    }

    public boolean isEmpty() {
        return products.isEmpty();
    }

    public static class Product {

        public final String id;

        @JsonCreator
        public Product(@JsonProperty("id") String id) {
            this.id = id;
        }
    }

}
