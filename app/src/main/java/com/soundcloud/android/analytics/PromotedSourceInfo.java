package com.soundcloud.android.analytics;

import com.soundcloud.android.model.Urn;
import com.soundcloud.android.presentation.PromotedListItem;
import com.soundcloud.java.objects.MoreObjects;
import com.soundcloud.java.optional.Optional;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.VisibleForTesting;

import java.util.List;

public final class PromotedSourceInfo implements Parcelable {

    private static final int NO_FLAGS = 0;

    public static final Parcelable.Creator<PromotedSourceInfo> CREATOR = new Parcelable.Creator<PromotedSourceInfo>() {
        public PromotedSourceInfo createFromParcel(Parcel in) {
            return new PromotedSourceInfo(in);
        }
        public PromotedSourceInfo[] newArray(int size) {
            return new PromotedSourceInfo[size];
        }
    };

    private final String adUrn;
    private final Urn promotedItemUrn;
    private final Optional<Urn> promoterUrn;
    private final List<String> trackingUrls;
    private boolean playedBefore;

    public static PromotedSourceInfo fromItem(PromotedListItem item) {
        return new PromotedSourceInfo(
                item.getAdUrn(),
                item.getUrn(),
                item.getPromoterUrn(),
                item.getPlayUrls()
        );
    }

    @VisibleForTesting
    public PromotedSourceInfo(String adUrn, Urn promotedItemUrn, Optional<Urn> promoterUrn, List<String> trackingUrls) {
        this.adUrn = adUrn;
        this.promotedItemUrn = promotedItemUrn;
        this.trackingUrls = trackingUrls;
        this.promoterUrn = promoterUrn;
    }

    public PromotedSourceInfo(Parcel in) {
        ClassLoader loader = PromotedSourceInfo.class.getClassLoader();
        playedBefore = in.readByte() != 0;
        adUrn = in.readString();
        promotedItemUrn = in.readParcelable(loader);
        Urn nullableUrn = in.readParcelable(loader);
        promoterUrn = Optional.fromNullable(nullableUrn);
        trackingUrls = in.readArrayList(loader);
    }

    public String getAdUrn() {
        return adUrn;
    }

    public Urn getPromotedItemUrn() {
        return promotedItemUrn;
    }

    public Optional<Urn> getPromoterUrn() {
        return promoterUrn;
    }

    public List<String> getTrackingUrls() {
        return trackingUrls;
    }

    public boolean isFirstPlay() {
        return !playedBefore;
    }

    public void setAsPlayed() {
        playedBefore = true;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByte((byte) (playedBefore ? 1 : 0));
        dest.writeString(adUrn);
        dest.writeParcelable(promotedItemUrn, NO_FLAGS);
        dest.writeParcelable(promoterUrn.isPresent() ? promoterUrn.get() : null, NO_FLAGS);
        dest.writeList(trackingUrls);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        PromotedSourceInfo that = (PromotedSourceInfo) o;

        return MoreObjects.equal(that.adUrn, this.adUrn)
                && MoreObjects.equal(that.promotedItemUrn, this.promotedItemUrn)
                && MoreObjects.equal(that.promoterUrn, this.promoterUrn)
                && MoreObjects.equal(that.trackingUrls, this.trackingUrls);
    }

    @Override
    public int hashCode() {
        return MoreObjects.hashCode(adUrn, promotedItemUrn, promoterUrn, trackingUrls);
    }
}
