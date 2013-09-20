package com.soundcloud.android.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.primitives.Longs;
import com.soundcloud.android.model.behavior.Identifiable;

import android.content.ContentValues;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.BaseColumns;

import java.util.List;

@edu.umd.cs.findbugs.annotations.SuppressWarnings(
        value="EQ_DOESNT_OVERRIDE_EQUALS",
        justification="Subclasses can sufficiently use the ID/URN for equals implementation")
public class ScModel implements Parcelable, Identifiable {

    public static final String EXTRA_ID = "id";
    public static final int NOT_SET = -1;

    private long mID = NOT_SET;
    protected ClientUri mURN;

    public ScModel() {
    }

    public ScModel(long id) {
        this.setId(id);
    }

    public ScModel(String urn) {
        this(ClientUri.fromUri(urn));
    }

    public ScModel(ClientUri urn) {
        setUrn(urn);
    }

    public ScModel(Parcel parcel) {
        String urn = parcel.readString();
        if (urn != null) {
            mURN = ClientUri.fromUri(urn);
        }
    }

    public ContentValues buildContentValues() {
        ContentValues cv = new ContentValues();
        if (getId() != NOT_SET) cv.put(BaseColumns._ID, getId());
        return cv;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mURN != null ? mURN.toString() : null);
    }

    @JsonIgnore
    public long getListItemId() {
        return getId();
    }

    @Override
    public long getId() {
        return mID != NOT_SET ? mID : idFromUrn();
    }

    @Override
    public void setId(long id) {
        this.mID = id;
    }

    @JsonIgnore
    public ClientUri getUrn() {
        return mURN;
    }

    @JsonProperty
    public void setUrn(String urn) {
        setUrn(ClientUri.fromUri(urn));
    }

    @JsonIgnore
    public void setUrn(ClientUri urn) {
        this.mURN = urn;
        if (mURN != null) {
            this.setId(mURN.numericId);
        }
    }

    private long idFromUrn() {
        if (mURN != null) {
            return mURN.numericId;
        }
        return NOT_SET;
    }

    @Override
    public int hashCode() {
        return new Long(getId()).hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ScModel)) {
            return false;
        }
        ScModel that = (ScModel) o;
        return this.getId() == that.getId();
    }

    public static <T extends ScModel> long[] getIdList(List<T> modelList) {
        return Longs.toArray(Lists.transform(modelList, new Function<T, Long>() {
            @Override
            public Long apply(T input) {
                return input.getId();
            }
        }));
    }
}
