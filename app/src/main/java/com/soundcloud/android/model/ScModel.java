package com.soundcloud.android.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.soundcloud.android.model.behavior.Identifiable;

import android.content.ContentValues;
import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.BaseColumns;

public class ScModel implements Parcelable, Identifiable {

    public static final String EXTRA_ID = "id";
    public static final int NOT_SET = -1;

    protected long mID = NOT_SET;
    protected ClientUri mURN;

    public ScModel() { }

    public ScModel(long id) {
        this.mID = id;
    }

    public ScModel(String urn) {
        this.mURN = ClientUri.fromUri(urn);
    }

    public ContentValues buildContentValues() {
        ContentValues cv = new ContentValues();
        if (mID != ScResource.NOT_SET) cv.put(BaseColumns._ID, mID);
        return cv;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
    }

    protected static int getIntOrNotSet(Cursor c, String column) {
        final int index = c.getColumnIndex(column);
        return c.isNull(index) ? NOT_SET : c.getInt(index);
    }

    @JsonIgnore
    public long getListItemId() {
        return mID;
    }

    @Override
    @JsonProperty("id")
    public long getId() {
        return mID != NOT_SET ? mID : idFromUrn();
    }

    @Override
    public void setId(long id) {
        this.mID = id;
    }

    public ClientUri getUrn() {
        return mURN;
    }

    public void setUrn(ClientUri urn) {
        this.mURN = urn;
    }

    public void setUrn(String urn) {
        this.mURN = ClientUri.fromUri(urn);
    }

    private long idFromUrn() {
        if (mURN != null) {
            return mURN.numericId;
        }
        return NOT_SET;
    }
}
