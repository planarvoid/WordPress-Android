package com.soundcloud.android.payments;

import com.soundcloud.java.optional.Optional;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

class AvailableWebProducts implements Parcelable {

    private static final String MID_TIER_PLAN_ID = "mid_tier";
    private static final String HIGH_TIER_PLAN_ID = "high_tier";

    private Optional<WebProduct> midTier = Optional.absent();
    private Optional<WebProduct> highTier = Optional.absent();

    static AvailableWebProducts single(WebProduct product) {
        return new AvailableWebProducts(Collections.singletonList(product));
    }

    static AvailableWebProducts empty() {
        return new AvailableWebProducts(Collections.emptyList());
    }

    AvailableWebProducts(List<WebProduct> products) {
        for (WebProduct product : products) {
            if (MID_TIER_PLAN_ID.equals(product.getPlanId())) {
                midTier = Optional.of(product);
            } else if (HIGH_TIER_PLAN_ID.equals(product.getPlanId())) {
                highTier = Optional.of(product);
            }
        }
    }

    Optional<WebProduct> midTier() {
        return midTier;
    }

    Optional<WebProduct> highTier() {
        return highTier;
    }

    AvailableWebProducts(Parcel in) {
        this(unpackParcel(in));
    }

    private static List<WebProduct> unpackParcel(Parcel in) {
        List<WebProduct> products = new LinkedList<>();
        AutoParcel_WebProduct[] parcel = new AutoParcel_WebProduct[in.readInt()];
        in.readTypedArray(parcel, AutoParcel_WebProduct.CREATOR);
        Collections.addAll(products, parcel);
        return products;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        List<WebProduct> products = new LinkedList<>();
        if (midTier.isPresent()) {
            products.add(midTier.get());
        }
        if (highTier.isPresent()) {
            products.add(highTier.get());
        }
        dest.writeInt(products.size());
        dest.writeTypedList(products);
    }

    @SuppressWarnings("unused")
    public static final Parcelable.Creator<AvailableWebProducts> CREATOR = new Parcelable.Creator<AvailableWebProducts>() {
        @Override
        public AvailableWebProducts createFromParcel(Parcel in) {
            return new AvailableWebProducts(in);
        }

        @Override
        public AvailableWebProducts[] newArray(int size) {
            return new AvailableWebProducts[size];
        }
    };

}
