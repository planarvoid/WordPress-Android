package com.soundcloud.android.analytics;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Objects;
import com.soundcloud.android.Consts;
import com.soundcloud.android.model.ParcelableUrn;
import com.soundcloud.android.model.Urn;
import org.jetbrains.annotations.NotNull;

import android.os.Parcel;
import android.os.Parcelable;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class SearchQuerySourceInfo implements Parcelable {


    public static final Parcelable.Creator<SearchQuerySourceInfo> CREATOR = new Parcelable.Creator<SearchQuerySourceInfo>() {
        public SearchQuerySourceInfo createFromParcel(Parcel in) {
            return new SearchQuerySourceInfo(in);
        }

        public SearchQuerySourceInfo[] newArray(int size) {
            return new SearchQuerySourceInfo[size];
        }
    };

    private final Urn queryUrn;
    private int clickPosition = Consts.NOT_SET;
    private Urn clickUrn = Urn.NOT_SET;
    private List<Urn> queryResults;

    public SearchQuerySourceInfo(Urn queryUrn) {
        this.queryUrn = queryUrn;
    }

    public SearchQuerySourceInfo(Urn queryUrn, int position, Urn clickUrn) {
        this.queryUrn = queryUrn;
        this.clickPosition = position;
        this.clickUrn = clickUrn;
    }

    public SearchQuerySourceInfo(Parcel in) {
        queryUrn = ParcelableUrn.unpack(in);
        clickPosition = in.readInt();
        clickUrn = ParcelableUrn.unpack(in);
        queryResults = ParcelableUrn.unpackList(in);
    }

    @Nullable
    public Urn getClickUrn() {
        return clickUrn;
    }

    public Urn getQueryUrn() {
        return queryUrn;
    }

    public void setQueryResults(@NotNull List<Urn> queryResults) {
        this.queryResults = new ArrayList<>(queryResults);
    }

    public int getUpdatedResultPosition(Urn currentTrack) {
        return clickUrn.isTrack() && queryResults != null ? queryResults.indexOf(currentTrack) : clickPosition;
    }

    @VisibleForTesting
    List<Urn> getQueryResults() {
        return this.queryResults;
    }

    public int getClickPosition() { return clickPosition; }

    @Override
    public String toString() {
        final Objects.ToStringHelper toStringHelper = Objects.toStringHelper(SearchQuerySourceInfo.class)
                .add("queryUrn", queryUrn)
                .add("clickPosition", clickPosition)
                .add("clickUrn", clickUrn);

        return toStringHelper.toString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeParcelable(ParcelableUrn.from(queryUrn), 0);
        dest.writeInt(clickPosition);
        dest.writeParcelable(ParcelableUrn.from(clickUrn), 0);
        dest.writeList(ParcelableUrn.from(queryResults));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        SearchQuerySourceInfo that = (SearchQuerySourceInfo) o;

        return Objects.equal(that.queryUrn, this.queryUrn)
                && Objects.equal(that.clickPosition, this.clickPosition)
                && Objects.equal(that.clickUrn, this.clickUrn)
                && Objects.equal(that.queryResults, this.queryResults);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(queryUrn, clickPosition, clickUrn, queryResults);
    }

    @Override
    public int describeContents() {
        return 0;
    }
}
