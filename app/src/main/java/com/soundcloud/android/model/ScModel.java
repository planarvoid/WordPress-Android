package com.soundcloud.android.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.primitives.Longs;
import com.soundcloud.android.api.legacy.model.behavior.Identifiable;

import android.content.ContentValues;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.BaseColumns;

import java.util.List;

@edu.umd.cs.findbugs.annotations.SuppressWarnings(
        value = "EQ_DOESNT_OVERRIDE_EQUALS",
        justification = "Subclasses can sufficiently use the ID/URN for equals implementation")
public class ScModel implements Parcelable, Identifiable {

    public static final String EXTRA_ID = "id";

    @Deprecated
    public static final int NOT_SET = -1;

    private long id = NOT_SET;
    protected Urn urn;

    public ScModel() {
    }

    public ScModel(long id) {
        this.setId(id);
    }

    public ScModel(String urn) {
        this.urn = new Urn(urn);
        setId(idFromUrn());
    }

    public ScModel(Urn urn) {
        this.urn = urn;
        setId(idFromUrn());
    }

    public ScModel(Parcel parcel) {
        id = parcel.readLong();
        byte hasUrn = parcel.readByte();
        if (hasUrn == 1) {
            urn = new Urn(parcel.readString());
        }
    }

    public ContentValues buildContentValues() {
        ContentValues cv = new ContentValues();
        if (getId() != NOT_SET) {
            cv.put(BaseColumns._ID, getId());
        }
        return cv;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(id);
        dest.writeByte((byte) (urn == null ? 0 : 1));
        if (urn != null) {
            dest.writeString(urn.toString());
        }
    }

    @JsonIgnore
    public long getListItemId() {
        return getId();
    }

    @Override
    public long getId() {
        return id != NOT_SET ? id : idFromUrn();
    }

    @Override
    public void setId(long id) {
        this.id = id;
    }

    @JsonIgnore
    public Urn getUrn() {
        return urn;
    }

    @JsonProperty
    public final void setUrn(String urn) {
        this.urn = new Urn(urn);
        id = idFromUrn();
    }

    private long idFromUrn() {
        if (urn != null) {
            return urn.getNumericId();
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
