package com.soundcloud.android.analytics;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.soundcloud.android.model.ParcelableUrn;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.tracks.PromotedTrackItem;

import android.os.Parcel;
import android.os.Parcelable;

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
    private final Urn trackUrn;
    private final Optional<Urn> promoterUrn;
    private final List<String> trackingUrls;

    public static PromotedSourceInfo fromTrack(PromotedTrackItem promotedTrack) {
        return new PromotedSourceInfo(
                promotedTrack.getAdUrn(),
                promotedTrack.getEntityUrn(),
                promotedTrack.getPromoterUrn(),
                promotedTrack.getPlayUrls()
        );
    }

    @VisibleForTesting
    public PromotedSourceInfo(String adUrn, Urn trackUrn, Optional<Urn> promoterUrn, List<String> trackingUrls) {
        this.adUrn = adUrn;
        this.trackUrn = trackUrn;
        this.trackingUrls = trackingUrls;
        this.promoterUrn = promoterUrn;
    }

    public PromotedSourceInfo(Parcel in) {
        ClassLoader loader = PromotedSourceInfo.class.getClassLoader();
        adUrn = in.readString();
        trackUrn = ParcelableUrn.unpack(in);
        promoterUrn = Optional.fromNullable(ParcelableUrn.unpack(in));
        trackingUrls = in.readArrayList(loader);
    }

    public String getAdUrn() {
        return adUrn;
    }

    public Urn getTrackUrn() {
        return trackUrn;
    }

    public Optional<Urn> getPromoterUrn() {
        return promoterUrn;
    }

    public List<String> getTrackingUrls() {
        return trackingUrls;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(adUrn);
        dest.writeParcelable(ParcelableUrn.from(trackUrn), NO_FLAGS);
        dest.writeParcelable(promoterUrn.isPresent() ? ParcelableUrn.from(promoterUrn.get()) : null, NO_FLAGS);
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

        return Objects.equal(that.adUrn, this.adUrn)
                && Objects.equal(that.trackUrn, this.promoterUrn)
                && Objects.equal(that.promoterUrn, this.promoterUrn)
                && Objects.equal(that.trackingUrls, this.trackingUrls);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(adUrn, trackUrn, promoterUrn, trackingUrls);
    }

}
