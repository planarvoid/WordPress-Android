package com.soundcloud.android.payments;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import rx.functions.Func1;

import java.util.List;

class AvailableProducts {

    private static final String CONSUMER_SUB = "consumer_sub";

    public static final Func1<AvailableProducts, Product> TO_PRODUCT = new Func1<AvailableProducts, Product>() {
        @Override
        public Product call(AvailableProducts availableProducts) {
            for (Product product : availableProducts.products) {
                if (product.clientProductId.equals(CONSUMER_SUB)) {
                    return product;
                }
            }
            return Product.empty();
        }
    };

    public final List<Product> products;

    @JsonCreator
    public AvailableProducts(@JsonProperty("products") List<Product> products) {
        this.products = products;
    }

    public static class Product {

        private static final String EMPTY = "unavailable";

        public final String id;
        public final String clientProductId;

        @JsonCreator
        public Product(@JsonProperty("id") String id, @JsonProperty("client_product_id") String clientProductId) {
            this.id = id;
            this.clientProductId = clientProductId;
        }

        public static Product empty() {
            return new Product(EMPTY, EMPTY);
        }

        public boolean isEmpty() {
            return id.equals(EMPTY);
        }
    }

}
