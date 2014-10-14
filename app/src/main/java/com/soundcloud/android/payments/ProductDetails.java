package com.soundcloud.android.payments;

public class ProductDetails {

    public final String id;
    public final String title;
    public final String description;
    public final String price;

    public ProductDetails(String id, String title, String description, String price) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.price = price;
    }

}
