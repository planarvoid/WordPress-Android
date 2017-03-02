package com.soundcloud.android.search;

import com.soundcloud.android.model.Urn;
import com.soundcloud.java.optional.Optional;

import android.os.Parcel;

public class UpsellSearchableItem implements SearchableItem {

    static final Urn UPSELL_URN = new Urn("local:search:upsell");

    static UpsellSearchableItem forUpsell() {
        return new UpsellSearchableItem();
    }

    @Override
    public Urn getUrn() {
        return UPSELL_URN;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
    }

    @Override
    public Optional<String> getImageUrlTemplate() {
        return Optional.absent();
    }

    public static final Creator<com.soundcloud.android.search.UpsellSearchableItem> CREATOR = new Creator<com.soundcloud.android.search.UpsellSearchableItem>() {
        public com.soundcloud.android.search.UpsellSearchableItem createFromParcel(Parcel in) {
            return new com.soundcloud.android.search.UpsellSearchableItem();
        }

        public com.soundcloud.android.search.UpsellSearchableItem[] newArray(int size) {
            return new com.soundcloud.android.search.UpsellSearchableItem[size];
        }
    };
}
