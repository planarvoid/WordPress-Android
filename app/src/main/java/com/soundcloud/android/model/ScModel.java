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
    protected String mURN;

    public ScModel() {
    }

    public ScModel(long id) {
        this.setId(id);
    }

    public ScModel(String urn) {
        mURN = urn;
        setId(idFromUrn());
    }

    public ScModel(Parcel parcel) {
        mID = parcel.readLong();
        byte hasUrn = parcel.readByte();
        if (hasUrn == 1) {
            mURN = parcel.readString();
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
        dest.writeLong(mID);
        dest.writeByte((byte) (mURN == null ? 0 : 1));
        if (mURN != null) {
            dest.writeString(mURN);
        }
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
    public String getUrn() {
        return mURN;
    }

    @JsonProperty
    public final void setUrn(String urn) {
        mURN = urn;
        mID = idFromUrn();
    }

    private long idFromUrn() {
        if (mURN != null) {
            return Urn.parse(mURN).numericId;
        }
        return NOT_SET;
    }

    @Override
    public int hashCode() {
        return Long.valueOf(getId()).hashCode();
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
