package com.soundcloud.android.analytics;

import com.soundcloud.android.Consts;
import com.soundcloud.android.model.Urn;
import com.soundcloud.android.utils.Urns;
import com.soundcloud.java.objects.MoreObjects;
import org.jetbrains.annotations.NotNull;

import android.os.Parcel;
import android.os.Parcelable;

import javax.annotation.Nullable;
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
    private final String queryString;
    private int clickPosition = Consts.NOT_SET;
    private Urn clickUrn = Urn.NOT_SET;
    private List<Urn> queryResults;

    public SearchQuerySourceInfo(Urn queryUrn, String queryString) {
        this.queryUrn = queryUrn;
        this.queryString = queryString;
    }

    public SearchQuerySourceInfo(Urn queryUrn, int position, Urn clickUrn, String queryString) {
        this.queryUrn = queryUrn;
        this.clickPosition = position;
        this.clickUrn = clickUrn;
        this.queryString = queryString;
    }

    public SearchQuerySourceInfo(Parcel in) {
        queryUrn = Urns.urnFromParcel(in);
        clickPosition = in.readInt();
        clickUrn = Urns.urnFromParcel(in);
        queryResults = Urns.urnsFromParcel(in);
        queryString = in.readString();
    }

    @Nullable
    public Urn getClickUrn() {
        return clickUrn;
    }

    public Urn getQueryUrn() {
        return queryUrn;
    }

    public String getQueryString() {
        return queryString;
    }

    public void setQueryResults(@NotNull List<Urn> queryResults) {
        this.queryResults = queryResults;
    }

    public int getUpdatedResultPosition(Urn currentTrack) {
        return clickUrn.isTrack() && queryResults != null ? queryResults.indexOf(currentTrack) : clickPosition;
    }

    public int getClickPosition() {
        return clickPosition;
    }

    @Override
    public String toString() {
        final MoreObjects.ToStringHelper toStringHelper = MoreObjects.toStringHelper(SearchQuerySourceInfo.class)
                                                                     .add("queryUrn", queryUrn)
                                                                     .add("clickPosition", clickPosition)
                                                                     .add("clickUrn", clickUrn);

        return toStringHelper.toString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        Urns.writeToParcel(dest, queryUrn);
        dest.writeInt(clickPosition);
        Urns.writeToParcel(dest, clickUrn);
        Urns.writeToParcel(dest, queryResults);
        dest.writeString(queryString);
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

        return MoreObjects.equal(that.queryUrn, this.queryUrn)
                && MoreObjects.equal(that.clickPosition, this.clickPosition)
                && MoreObjects.equal(that.clickUrn, this.clickUrn)
                && MoreObjects.equal(that.queryResults, this.queryResults)
                && MoreObjects.equal(that.queryString, this.queryString);
    }

    @Override
    public int hashCode() {
        return MoreObjects.hashCode(queryUrn, clickPosition, clickUrn, queryResults, queryString);
    }

    @Override
    public int describeContents() {
        return 0;
    }
}
